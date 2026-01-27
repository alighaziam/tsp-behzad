package ir.tax.trustedtaxpayers.biz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.service.InvoiceFormSubmissionService;
import ir.tax.trustedtaxpayers.application.service.InvoiceFormValidationService;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternClientEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternRequestClientEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternRequestEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PatternClientRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PatternRequestClientRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PatternRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * لایه فاساد برای مدیریت اعتبارسنجی، ارسال و پایش وضعیت درخواست‌های الگو.
 * <p>
 * Facade orchestrating validation, submission, and status tracking for pattern requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatternFacadeService {

    /**
     * مرجع قابل‌استفاده مجدد برای تبدیل رشته JSON به {@code Map<String, Object>}.
     * <p>
     * Reusable type reference for parsing {@code Map<String, Object>} payloads.
     */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * ریپازیتوری ذخیره و بازیابی درخواست‌های الگو.
     * <p>
     * Repository accessor for persisting and querying pattern requests.
     */
    private final PatternRequestRepository patternRequestRepository;
    /**
     * ریپازیتوری نگهدارنده ارتباط بین درخواست‌ها و کلاینت‌ها.
     * <p>
     * Repository accessor for the link table between requests and clients.
     */
    private final PatternRequestClientRepository patternRequestClientRepository;
    /**
     * ریپازیتوری مدیریت شرکت‌های طرف قرارداد ثبت‌شده.
     * <p>
     * Repository accessor for registered third-party clients.
     */
    private final PatternClientRepository patternClientRepository;
    /**
     * سرویس مسئول دریافت شِما و اعتبارسنجی اطلاعات قبل از ارسال.
     * <p>
     * Service used to fetch schemas and validate payloads prior to submission.
     */
    private final InvoiceFormValidationService invoiceFormValidationService;
    /**
     * سرویس مسئول ارسال داده‌های معتبر به سامانه بیرونی.
     * <p>
     * Service responsible for forwarding validated payloads to the external system.
     */
    private final InvoiceFormSubmissionService invoiceFormSubmissionService;
    /**
     * ریپازیتوری جهت دریافت جزئیات وضعیت از سامانه امور مالیاتی.
     * <p>
     * Repository used for fetching transmission status details provided by the external system.
     */
    private final ElectronicInvoiceSystemRepository electronicInvoiceSystemRepository;
    /**
     * سرویس اوتباکس برای همگام‌سازی غیرهمزمان با سامانه‌های مصرف‌کننده.
     * <p>
     * Event outbox for asynchronous integration with downstream consumers.
     */
    private final OutboxService outboxService;
    /**
     * آبجکت مپ مشترک جهت سریال‌سازی و دی‌سریال‌سازی JSON.
     * <p>
     * Shared Jackson mapper used for serializing and deserializing JSON payloads.
     */
    private final ObjectMapper objectMapper;

    /**
     * یک درخواست الگو ایجاد می‌کند و متادیتای لازم برای رابط کاربری را فراهم می‌سازد.
     * <p>
     * Creates a new pattern request and prepares metadata required by the client UI.
     *
     * @param requestDto مشخصات ورودی درخواست / inbound attributes for the new request
     * @return خروجی شامل شناسه‌ها و ساختار فرم / DTO containing request identifiers and form definition
     */
    @Transactional
    public PatternRequestResponseDto createRequest(PatternRequestCreateDto requestDto) {
        Integer patternNumber = requestDto.getPatternNumber();
        log.info("Creating pattern request for pattern {} by {}", patternNumber, requestDto.getRequestedBy());

        PatternClientEntity client = resolveClient(requestDto.getClientKey());
        String requester = client != null ? client.getClientKey() : requestDto.getRequestedBy();
        if ((requester == null || requester.isBlank())) {
            throw new IllegalArgumentException("clientKey یا requestedBy باید ارسال شود");
        }
        requester = requester.trim();

        PatternRequestEntity entity = PatternRequestEntity.builder()
                .patternNumber(patternNumber)
                .status(PatternRequestStatus.PENDING)
                .requestedBy(requester)
                .requestMetadata(writeJsonSafely(requestDto.getMetadata()))
                .build();

        entity = patternRequestRepository.save(entity);
        PatternRequestClientEntity mapping = persistClientMapping(entity, client, requester, requestDto.getMetadata());

        FormSchemaDto schema = invoiceFormValidationService.getFormSchema(patternNumber);

        List<PatternFieldDto> headerFields = new ArrayList<>();
        List<PatternFieldDto> itemFields = new ArrayList<>();

        if (schema != null) {
            mapSchemaFields(schema, headerFields, itemFields);
        }

        String externalRequestId = mapping != null ? mapping.getExternalRequestId() : entity.getId().toString();

        return PatternRequestResponseDto.builder()
                .requestId(entity.getId())
                .patternNumber(patternNumber)
                .title(schema != null ? schema.getTitle() : null)
                .description(schema != null ? schema.getDescription() : null)
                .requestedBy(entity.getRequestedBy())
                .clientKey(client != null ? client.getClientKey() : entity.getRequestedBy())
                .clientName(client != null ? client.getName() : null)
                .externalRequestId(externalRequestId)
                .deferredSubmission(entity.isDeferredSubmission())
                .deferredApproved(entity.isDeferredApproved())
                .status(entity.getStatus().name())
                .headerFields(headerFields)
                .itemFields(itemFields)
                .metadata(parseMapSafely(entity.getRequestMetadata()))
                .build();
    }

    /**
     * داده‌های درخواست الگو را اعتبارسنجی و در صورت موفقیت ارسال می‌کند.
     * <p>
     * Validates and transmits the payload associated with a pattern request.
     *
     * @param requestId  شناسه یکتای درخواست / unique identifier of the request
     * @param requestDto داده‌های سربرگ و اقلام ارسالی / header and item payload submitted by the client
     * @return پاسخ حاوی نتیجه اعتبارسنجی و ارسال / submission response describing validation and transmission outcome
     */
    @Transactional
    public PatternSubmissionResponseDto submit(UUID requestId, PatternSubmissionRequestDto requestDto) {
        PatternRequestEntity entity = patternRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found: " + requestId));

        log.info("Submitting pattern request {} for pattern {}", requestId, entity.getPatternNumber());

        PatternClientEntity client = getClientForRequest(entity);
        PatternRequestClientEntity mapping = getMapping(entity.getId());
        String externalRequestId = mapping != null ? mapping.getExternalRequestId() : entity.getId().toString();

        ValidationResultDto validationResult = invoiceFormValidationService.validate(
                entity.getPatternNumber(),
                requestDto.getHeader(),
                requestDto.getItems());

        entity.setRequestPayload(writeJsonSafely(requestDto));

        if (!validationResult.isValid()) {
            entity.setStatus(PatternRequestStatus.VALIDATION_FAILED);
            entity.setResponsePayload(writeJsonSafely(validationResult));
            entity.setCompletedAt(OffsetDateTime.now());
            patternRequestRepository.save(entity);

            return PatternSubmissionResponseDto.builder()
                    .requestId(entity.getId())
                    .transmissionStatus(PatternRequestStatus.VALIDATION_FAILED.name())
                    .requestedBy(entity.getRequestedBy())
                    .clientKey(client != null ? client.getClientKey() : entity.getRequestedBy())
                    .clientName(client != null ? client.getName() : null)
                    .externalRequestId(externalRequestId)
                    .deferredSubmission(entity.isDeferredSubmission())
                    .deferredApproved(entity.isDeferredApproved())
                    .validation(validationResult)
                    .success(false)
                    .completedAt(entity.getCompletedAt())
                    .build();
        }

        entity.setStatus(PatternRequestStatus.VALIDATED);
        entity.setResponsePayload(null);
        patternRequestRepository.save(entity);

        InvoiceFormSubmissionService.SubmitResult submitResult =
                invoiceFormSubmissionService.submit(entity.getPatternNumber(),
                        requestDto.getHeader(), requestDto.getItems());

        entity.setTaxId(submitResult.taxId);
        entity.setSubmittedAt(OffsetDateTime.now());
        entity.setHeaderId(submitResult.headerId);
        entity.setCompletedAt(null);
        entity.setRejectionCode(null);
        entity.setRejectionDescription(null);

        if (requestDto.isDeferred()) {
            entity.setDeferredSubmission(true);
            entity.setDeferredApproved(false);
            entity.setStatus(PatternRequestStatus.DEFERRED);
            patternRequestRepository.save(entity);

            return PatternSubmissionResponseDto.builder()
                    .requestId(entity.getId())
                    .headerId(submitResult.headerId)
                    .taxId(submitResult.taxId)
                    .transmissionStatus(PatternRequestStatus.DEFERRED.name())
                    .message("AWAITING_APPROVAL")
                    .requestedBy(entity.getRequestedBy())
                    .clientKey(client != null ? client.getClientKey() : entity.getRequestedBy())
                    .clientName(client != null ? client.getName() : null)
                    .externalRequestId(externalRequestId)
                    .deferredSubmission(true)
                    .deferredApproved(false)
                    .validation(validationResult)
                    .success(true)
                    .completedAt(null)
                    .build();
        }

        entity.setDeferredSubmission(false);
        entity.setDeferredApproved(false);
        entity.setStatus(PatternRequestStatus.SUBMITTED);
        patternRequestRepository.save(entity);

        enqueueSubmissionEvent(entity, submitResult.headerId);

        return PatternSubmissionResponseDto.builder()
                .requestId(entity.getId())
                .headerId(submitResult.headerId)
                .taxId(submitResult.taxId)
                .transmissionStatus(PatternRequestStatus.SUBMITTED.name())
                .message("QUEUED")
                .requestedBy(entity.getRequestedBy())
                .clientKey(client != null ? client.getClientKey() : entity.getRequestedBy())
                .clientName(client != null ? client.getName() : null)
                .externalRequestId(externalRequestId)
                .deferredSubmission(false)
                .deferredApproved(false)
                .validation(validationResult)
                .success(true)
                .completedAt(null)
                .build();
    }

    /**
     * وضعیت فعلی درخواست الگو را بر اساس شناسه اصلی بازمی‌گرداند.
     * <p>
     * Returns the current status of a pattern request by its primary identifier.
     *
     * @param requestId شناسه درخواست / request identifier
     * @return DTO استعلام وضعیت / inquiry DTO populated with latest status information
     */
    @Transactional
    public PatternInquiryResponseDto getStatus(UUID requestId) {
        PatternRequestEntity entity = patternRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found: " + requestId));
        return toInquiryDto(entity);
    }

    /**
     * وضعیت فعلی درخواست را بر اساس referenceNumber سامانه بیرونی بازمی‌گرداند.
     * <p>
     * Returns the current status of a pattern request by its external reference number.
     *
     * @param referenceNumber شماره مرجع در سامانه بیرونی / reference number stored in the electronic invoice system
     * @return DTO استعلام وضعیت / inquiry DTO populated with latest status information
     */
    @Transactional
    public PatternInquiryResponseDto getStatusByReference(String referenceNumber) {
        PatternRequestEntity entity = patternRequestRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found by reference: " + referenceNumber));
        return toInquiryDto(entity);
    }

    /**
     * درخواست‌های الگو را با فیلترهای انتخابی مانند وضعیت و بازه زمانی جستجو می‌کند.
     * <p>
     * Searches pattern requests using arbitrary filters such as status and date range.
     *
     * @param criteria مجموعه فیلترها / filter container
     * @return فهرست نتایج مطابق فیلتر / list of matching request projections
     */
    @Transactional
    public List<PatternInquiryResponseDto> search(PatternRequestSearchCriteria criteria) {
        Specification<PatternRequestEntity> spec = Specification.where(null);

        if (criteria.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), criteria.getStatus()));
        }
        if (criteria.getPatternNumber() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patternNumber"), criteria.getPatternNumber()));
        }
        if (criteria.getRequestedBy() != null && !criteria.getRequestedBy().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requestedBy"), criteria.getRequestedBy()));
        }
        if (criteria.getFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdOn"), criteria.getFrom()));
        }
        if (criteria.getTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdOn"), criteria.getTo()));
        }

        List<PatternRequestEntity> entities = patternRequestRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdOn"));
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        List<PatternInquiryResponseDto> responses = new ArrayList<>(entities.size());
        for (PatternRequestEntity entity : entities) {
            responses.add(toInquiryDto(entity));
        }
        return responses;
    }

    /**
     * همه الگوهای پشتیبانی‌شده و اطلاعات فیلدهای آن‌ها را بازمی‌گرداند.
     * <p>
     * Enumerates all supported pattern schemas along with their fields metadata.
     *
     * @return فهرست الگوها و تعاریفشان / list of available patterns and corresponding definitions
     */
    @Transactional(readOnly = true)
    public List<PatternDefinitionDto> listAllPatterns() {
        List<PatternDefinitionDto> result = new ArrayList<>();
        for (int pattern = 1; pattern <= 16; pattern++) {
            FormSchemaDto schema = invoiceFormValidationService.getFormSchema(pattern);
            if (schema == null) {
                continue;
            }
            List<PatternFieldDto> headerFields = new ArrayList<>();
            List<PatternFieldDto> itemFields = new ArrayList<>();
            mapSchemaFields(schema, headerFields, itemFields);
            PatternDefinitionDto dto = PatternDefinitionDto.builder()
                    .patternNumber(pattern)
                    .title(schema.getTitle())
                    .description(schema.getDescription())
                    .headerFields(headerFields)
                    .itemFields(itemFields)
                    .build();
            result.add(dto);
        }
        return result;
    }

    /**
     * درخواست معوق را تایید می‌کند تا در آینده ارسال شود.
     * <p>
     * Marks a deferred request as approved to allow later transmission.
     *
     * @param requestId شناسه درخواست معوق / identifier of the deferred request
     */
    @Transactional
    public void approveDeferredRequest(UUID requestId) {
        PatternRequestEntity entity = patternRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found: " + requestId));
        if (!entity.isDeferredSubmission()) {
            throw new IllegalStateException("Request is not marked as deferred: " + requestId);
        }
        entity.setDeferredApproved(true);
        entity.setStatus(PatternRequestStatus.DEFERRED);
        patternRequestRepository.save(entity);
    }

    /**
     * شناسه‌های دریافتی از سامانه امور مالیاتی را به درخواست اضافه می‌کند.
     * <p>
     * Enriches the request entity with identifiers returned by the electronic invoice system.
     *
     * @param entity   موجودیت درخواست برای به‌روزرسانی / request entity to update
     * @param headerId شناسه سربرگ جهت استعلام / header identifier used to query the external system
     */
    private void populateSystemIdentifiers(PatternRequestEntity entity, UUID headerId) {
        Optional<ElectronicInvoiceSystemEntity> systemOpt = electronicInvoiceSystemRepository.findByHeaderId(headerId);
        systemOpt.ifPresent(systemEntity -> {
            entity.setReferenceNumber(systemEntity.getReferenceNumber());
            if (systemEntity.getRejcode() != null && entity.getRejectionCode() == null) {
                entity.setRejectionCode(systemEntity.getRejcode());
            }
            if (systemEntity.getRejdesc() != null && entity.getRejectionDescription() == null) {
                entity.setRejectionDescription(systemEntity.getRejdesc());
            }
        });
    }

    /**
     * موجودیت درخواست را به DTO مناسب برای سرویس‌های استعلام نگاشت می‌کند.
     * <p>
     * Maps a persisted request entity to a response DTO suitable for inquiry endpoints.
     *
     * @param entity موجودیت درخواست / request entity
     * @return DTO استعلام / inquiry DTO
     */
    private PatternInquiryResponseDto toInquiryDto(PatternRequestEntity entity) {
        PatternClientEntity client = getClientForRequest(entity);
        PatternRequestClientEntity mapping = getMapping(entity.getId());
        String externalRequestId = mapping != null ? mapping.getExternalRequestId() : entity.getId().toString();
        return PatternInquiryResponseDto.builder()
                .requestId(entity.getId())
                .patternNumber(entity.getPatternNumber())
                .status(entity.getStatus().name())
                .requestedBy(entity.getRequestedBy())
                .clientKey(client != null ? client.getClientKey() : entity.getRequestedBy())
                .clientName(client != null ? client.getName() : null)
                .externalRequestId(externalRequestId)
                .deferredSubmission(entity.isDeferredSubmission())
                .deferredApproved(entity.isDeferredApproved())
                .taxId(entity.getTaxId())
                .referenceNumber(entity.getReferenceNumber())
                .rejectionCode(entity.getRejectionCode())
                .rejectionDescription(entity.getRejectionDescription())
                .createdOn(entity.getCreatedOn())
                .submittedAt(entity.getSubmittedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    /**
     * فیلدهای سربرگ و اقلام را بر اساس شِمای فرم تفکیک و در لیست‌ها قرار می‌دهد.
     * <p>
     * Populates DTO collections with header and item fields derived from a form schema.
     *
     * @param schema       شِمای دریافت‌شده / schema obtained from validation service
     * @param headerFields لیست فیلدهای سربرگ / accumulator for header fields
     * @param itemFields   لیست فیلدهای اقلام / accumulator for item fields
     */
    private void mapSchemaFields(FormSchemaDto schema, List<PatternFieldDto> headerFields, List<PatternFieldDto> itemFields) {
        if (schema.getFields() == null) {
            return;
        }
        for (FormFieldDto field : schema.getFields()) {
            if (field.getKey() == null) {
                continue;
            }
            boolean isBody = field.getKey().startsWith("body.");
            PatternFieldDto dto = toPatternField(field, isBody ? "ITEM" : "HEADER");
            if (isBody) {
                itemFields.add(dto);
            } else {
                headerFields.add(dto);
            }
        }
    }

    /**
     * یک فیلد فرم را به DTO خروجی تبدیل و کلید و برچسب را نرمال‌سازی می‌کند.
     * <p>
     * Converts a form field to an outbound DTO while normalizing key and labels.
     *
     * @param field   فیلد منبع / source field
     * @param section بخش هدف {@code HEADER} یا {@code ITEM} / target section flag {@code HEADER} or {@code ITEM}
     * @return DTO تبدیل‌شده / converted DTO
     */
    private PatternFieldDto toPatternField(FormFieldDto field, String section) {
        String key = field.getKey();
        if ("ITEM".equals(section) && key.startsWith("body.")) {
            key = key.substring("body.".length());
        }
        return PatternFieldDto.builder()
                .section(section)
                .key(key)
                .label(field.getLabel() != null ? field.getLabel() : field.getName())
                .type(field.getType())
                .required(field.getRequired())
                .maxLength(field.getMaxLength())
                .minLength(field.getMinLength())
                .defaultValue(field.getDefaultValue())
                .options(field.getOptions())
                .build();
    }

    /**
     * وضعیت‌های سامانه بیرونی را به مقادیر داخلی {@link PatternRequestStatus} نگاشت می‌کند.
     * <p>
     * Maps external transmission statuses to internal {@link PatternRequestStatus} values.
     *
     * @param status رشته وضعیت بیرونی / external status string
     * @return مقدار معادل یا {@link PatternRequestStatus#FAILED} / matching enum value, defaulting to {@link PatternRequestStatus#FAILED}
     */
    private PatternRequestStatus mapTransmissionStatus(String status) {
        if (status == null) {
            return PatternRequestStatus.FAILED;
        }
        return switch (status.toUpperCase()) {
            case "ACCEPTED" -> PatternRequestStatus.ACCEPTED;
            case "SUBMITTED" -> PatternRequestStatus.SUBMITTED;
            case "REJECTED" -> PatternRequestStatus.REJECTED;
            default -> PatternRequestStatus.FAILED;
        };
    }

    /**
     * یک شیء را به JSON تبدیل می‌کند و خطاها را به استثنای unchecked تبدیل می‌کند.
     * <p>
     * Serializes an object to JSON while transforming exceptions into unchecked errors.
     *
     * @param value مقدار برای سریال‌سازی / value to serialize
     * @return JSON تولیدشده یا {@code null} / serialized JSON or {@code null} if the value is {@code null}
     */
    private String writeJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value to JSON: {}", e.getMessage());
            throw new IllegalStateException("Unable to serialize value", e);
        }
    }

    /**
     * رشته JSON را با تحمل خطا به {@code Map<String, Object>} تبدیل می‌کند.
     * <p>
     * Parses a JSON string into a {@code Map<String, Object>} with relaxed error handling.
     *
     * @param value رشته JSON / JSON string
     * @return نقشه حاصل یا خالی در صورت خطا / parsed map or empty map on error
     */
    private Map<String, Object> parseMapSafely(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (Exception e) {
            log.debug("Failed to parse metadata JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * کلاینت الگو را بر اساس کلید یافته و فعال بودن آن را بررسی می‌کند.
     * <p>
     * Resolves a pattern client by key and validates its activation status.
     *
     * @param clientKey کلید کلاینت / client identifier supplied by the requester
     * @return کلاینت یافت‌شده یا {@code null} / resolved client or {@code null} when no key is provided
     */
    private PatternClientEntity resolveClient(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return null;
        }
        PatternClientEntity client = patternClientRepository.findByClientKey(clientKey)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern client not found for key: " + clientKey));
        if (!client.getIsActive()) {
            throw new IllegalArgumentException("Pattern client is inactive: " + clientKey);
        }
        return client;
    }

    /**
     * کلاینت متناظر با یک درخواست ثبت‌شده را بازیابی می‌کند.
     * <p>
     * Resolves the client entity associated with a persisted request.
     *
     * @param entity موجودیت درخواست / request entity
     * @return کلاینت متناظر یا {@code null} / associated client or {@code null} when not found
     */
    private PatternClientEntity getClientForRequest(PatternRequestEntity entity) {
        PatternRequestClientEntity mapping = getMapping(entity.getId());
        if (mapping != null) {
            if (mapping.getClient().getId() != null) {
                return patternClientRepository.findById(mapping.getClient().getId()).orElse(null);
            }
            return patternClientRepository.findByClientKey(mapping.getThirdPartyId()).orElse(null);
        }
        return patternClientRepository.findByClientKey(entity.getRequestedBy()).orElse(null);
    }

    /**
     * مپینگ بین درخواست و کلاینت را ایجاد یا بازیابی می‌کند.
     * <p>
     * Creates or returns the existing mapping between a request and a client.
     *
     * @param entity       موجودیت درخواست / request entity
     * @param client       کلاینت متناظر / resolved client
     * @param requesterKey شناسه جایگزین درخواست‌کننده / fallback requester identifier
     * @param metadata     متادیتای ورودی جهت استخراج ارجاع‌ها / inbound metadata for deriving references
     * @return مپینگ ذخیره‌شده یا {@code null} / persisted mapping entity or {@code null} when not applicable
     */
    private PatternRequestClientEntity persistClientMapping(PatternRequestEntity entity, PatternClientEntity client, String requesterKey, Map<String, Object> metadata) {
        if ((requesterKey == null || requesterKey.isBlank()) && client == null) {
            return null;
        }

        PatternRequestClientEntity existing = patternRequestClientRepository.findByRequestId(entity.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        String externalReference = extractExternalReference(metadata);
        String externalRequestId = generateExternalRequestId(entity.getId(), client);

        PatternRequestClientEntity.PatternRequestClientEntityBuilder builder = PatternRequestClientEntity.builder()
                .request(entity)
                .thirdPartyId(client != null ? client.getClientKey() : requesterKey)
                .externalReference(externalReference)
                .externalRequestId(externalRequestId);
        if (client != null) {
            builder.client(client);
        }
        return patternRequestClientRepository.save(builder.build());
    }

    /**
     * مپینگ کلاینت مرتبط با شناسه درخواست را بازیابی می‌کند.
     * <p>
     * Fetches the client mapping associated with a request identifier.
     *
     * @param requestId شناسه درخواست / request identifier
     * @return موجودیت مپینگ یا {@code null} / mapping entity or {@code null} if not found
     */
    private PatternRequestClientEntity getMapping(UUID requestId) {
        return patternRequestClientRepository.findByRequestId(requestId).orElse(null);
    }

    /**
     * شناسه خارجی درخواست را تولید می‌کند و در صورت وجود، با کلید کلاینت امضا می‌کند.
     * <p>
     * Generates a deterministic external request identifier, optionally HMAC-signed per client.
     *
     * @param requestId شناسه درخواست / request identifier
     * @param client    کلاینت شامل کلید امضا (اختیاری) / optional client providing the signing secret
     * @return رشته شناسه تولیدشده / generated identifier string
     */
    private String generateExternalRequestId(UUID requestId, PatternClientEntity client) {
        if (client != null && client.getClientSecret() != null && !client.getClientSecret().isBlank()) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(client.getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] digest = mac.doFinal(requestId.toString().getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Failed to generate external request id", e);
            }
        }
        return requestId.toString();
    }

    /**
     * رویداد اوتباکس محتوی اطلاعات ارسال را برای پردازش غیرهمزمان در صف قرار می‌دهد.
     * <p>
     * Enqueues an outbox event capturing submission metadata for asynchronous processing.
     *
     * @param entity   موجودیت درخواست / request entity
     * @param headerId شناسه سربرگ بازگشتی / header identifier returned by the transmission service
     */
    private void enqueueSubmissionEvent(PatternRequestEntity entity, UUID headerId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("requestId", entity.getId().toString());
        payload.put("headerId", headerId.toString());
        payload.put("clientKey", entity.getRequestedBy());
        outboxService.enqueue(
                "PATTERN_REQUEST",
                headerId.toString(),
                "PATTERN_SUBMISSION",
                payload,
                Map.of("requestId", entity.getId().toString()));
    }

    /**
     * نتیجه نهایی ارسال‌شده از سامانه بیرونی را روی درخواست اعمال می‌کند.
     * <p>
     * Applies the final transmission result received from the external system.
     *
     * @param requestId شناسه درخواست / request identifier
     * @param headerId  شناسه سربرگ / header identifier coming from the downstream service
     * @param result    نتیجه ارسال / transmission outcome
     */
    @Transactional
    public void applyTransmissionResult(UUID requestId, UUID headerId, InvoiceTransmissionService.TransmissionResult result) {
        PatternRequestEntity entity = patternRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found: " + requestId));

        PatternRequestStatus finalStatus = mapTransmissionStatus(result.getStatus());
        entity.setStatus(finalStatus);
        entity.setCompletedAt(OffsetDateTime.now());
        entity.setRejectionCode(result.getRejectionCode());
        entity.setRejectionDescription(result.getRejectionDescription());
        entity.setTaxId(result.getTaxId() != null ? result.getTaxId() : entity.getTaxId());
        entity.setResponsePayload(writeJsonSafely(result));
        entity.setHeaderId(headerId);
        entity.setDeferredSubmission(false);
        entity.setDeferredApproved(false);

        populateSystemIdentifiers(entity, headerId);

        patternRequestRepository.save(entity);
    }

    /**
     * در صورت بروز خطای ارسال، وضعیت شکست را روی درخواست اعمال می‌کند.
     * <p>
     * Applies a transmission failure when the external service call does not succeed.
     *
     * @param requestId شناسه درخواست / request identifier
     * @param headerId  شناسه سربرگ خطادار / header identifier that failed
     * @param message   پیام خطای توصیفی / descriptive error message
     */
    @Transactional
    public void applyTransmissionFailure(UUID requestId, UUID headerId, String message) {
        PatternRequestEntity entity = patternRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern request not found: " + requestId));

        entity.setStatus(PatternRequestStatus.FAILED);
        entity.setCompletedAt(OffsetDateTime.now());
        entity.setRejectionCode("TRANSMISSION_ERROR");
        entity.setRejectionDescription(message);
        entity.setResponsePayload(writeJsonSafely(Map.of("error", message)));
        entity.setHeaderId(headerId);
        entity.setDeferredSubmission(false);
        entity.setDeferredApproved(false);

        populateSystemIdentifiers(entity, headerId);

        patternRequestRepository.save(entity);
    }

    /**
     * تلاش می‌کند ارجاع بیرونی ارائه‌شده توسط کلاینت را از متادیتا استخراج کند.
     * <p>
     * Attempts to locate a client-supplied reference inside the metadata map.
     *
     * @param metadata متادیتای ورودی / inbound metadata
     * @return ارجاع استخراج‌شده یا {@code null} / extracted reference or {@code null} if not present
     */
    private String extractExternalReference(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object value = metadata.getOrDefault("clientRef",
                metadata.getOrDefault("clientReference",
                        metadata.getOrDefault("externalReference", null)));
        return value != null ? value.toString() : null;
    }
}

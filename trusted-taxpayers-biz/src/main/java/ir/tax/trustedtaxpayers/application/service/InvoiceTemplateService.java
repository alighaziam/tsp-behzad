package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.InvoiceTemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.InvoiceTemplateRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing invoice templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TaxpayerRepository taxpayerRepository;

    @Transactional(readOnly = true)
    public List<InvoiceTemplateEntity> getUserTemplates(UUID userId, UUID taxpayerId) {
        return templateRepository.findByUserIdAndTaxpayerId(
                userId, taxpayerId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceTemplateEntity> getUserTemplatesByPattern(
            UUID userId, UUID taxpayerId, Integer patternNumber) {
        return templateRepository.findByUserIdAndTaxpayerIdAndPatternNumber(
                userId, taxpayerId, patternNumber);
    }

    @Transactional(readOnly = true)
    public InvoiceTemplateEntity getTemplate(UUID templateId, UUID userId, UUID taxpayerId) {
        return templateRepository.findById(templateId)
                .filter(t -> t.getUser().getId().equals(userId) && t.getTaxpayer().getId().equals(taxpayerId))
                .filter(t -> t.getIsActive())
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    }

    @Transactional
    public InvoiceTemplateEntity createTemplate(
            UUID userId,
            UUID taxpayerId,
            String templateName,
            Integer patternNumber,
            String headerData,
            String itemsData,
            String description) {
        
        InvoiceTemplateEntity template = InvoiceTemplateEntity.builder()
                .user(userRepository.getById(userId))
                .taxpayer(taxpayerRepository.getById(taxpayerId))
                .templateName(templateName)
                .patternNumber(patternNumber)
                .headerData(headerData)
                .itemsData(itemsData)
                .description(description)
                .isActive(true)
                .build();

        return templateRepository.save(template);
    }

    @Transactional
    public InvoiceTemplateEntity updateTemplate(
            UUID templateId,
            UUID userId,
            UUID taxpayerId,
            String templateName,
            String headerData,
            String itemsData,
            String description) {
        
        InvoiceTemplateEntity template = getTemplate(templateId, userId, taxpayerId);
        
        if (templateName != null) {
            template.setTemplateName(templateName);
        }
        if (headerData != null) {
            template.setHeaderData(headerData);
        }
        if (itemsData != null) {
            template.setItemsData(itemsData);
        }
        if (description != null) {
            template.setDescription(description);
        }

        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId, UUID userId, UUID taxpayerId) {
        InvoiceTemplateEntity template = getTemplate(templateId, userId, taxpayerId);
        template.setIsActive(false);
        templateRepository.save(template);
    }
}


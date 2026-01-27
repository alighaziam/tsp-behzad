package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceBodyDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceHeaderDto;
import ir.tax.trustedtaxpayers.enums.FlightType;
import ir.tax.trustedtaxpayers.enums.InvoiceStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceItemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Business service for tax invoice operations
 * این سرویس تمام business logic مربوط به صورت‌حساب‌های مالیاتی را handle می‌کند
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxInvoiceBusinessService {

    private final ElectronicInvoiceHeaderService headerService;
    private final ElectronicInvoiceItemService itemService;
    private final ElectronicInvoiceSystemService systemService;
    private final ElectronicInvoiceSystemRepository systemRepository;
    private final TaxpayerService taxpayerService;
    private final InvoiceTemplateLoaderService templateLoaderService;
    private final TaxpayerBuyerService taxpayerBuyerService;
    private final TaxpayerTaxProductService taxpayerTaxProductService;
    private final TaxProductRepository taxProductRepository;

    /**
     * Save invoices to database with READY_TO_SEND status
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<UUID> saveInvoicesToDatabase(List<InvoiceDto> invoices) {

        List<UUID> savedHeaderIds = new ArrayList<>();

        for (InvoiceDto invoice : invoices) {

            UUID headerId = null;

            try {
                /* =======================
                 * 1. Save Invoice Header
                 * ======================= */
                ElectronicInvoiceHeaderEntity headerEntity =
                        convertInvoiceDtoToEntity(invoice);

                headerEntity.setStatus(InvoiceStatus.READY_TO_SEND);

                headerEntity = headerService.save(headerEntity);
                headerId = headerEntity.getId();
                savedHeaderIds.add(headerId);

                /* =======================
                 * 2. Save Invoice Items
                 * ======================= */
                if (invoice.getBody() != null && !invoice.getBody().isEmpty()) {
                    int rowNumber = 1;
                    for (var bodyDto : invoice.getBody()) {
                        ElectronicInvoiceItemEntity itemEntity =
                                convertBodyDtoToEntity(bodyDto, headerId, rowNumber++);
                        itemService.save(itemEntity);
                    }
                }

                /* =========================================
                 * 3. Update / Insert ELECTRONIC_INVOICE_SYSTEM
                 * ========================================= */
                int updated =
                        systemRepository.updateStsNative(headerId.toString(), "READY_TO_SEND");

                if (updated == 0) {
                    try {
                        systemRepository.insertNative(headerId.toString(), "READY_TO_SEND");
                    } catch (Exception ex) {
                        // race-condition → رکورد هم‌زمان insert شده
                        log.debug(
                                "ELECTRONIC_INVOICE_SYSTEM already exists for headerId={}",
                                headerId
                        );
                    }
                }

                /* =========================================
                 * 4. Create TaxpayerBuyer Relation
                 * ========================================= */
                createTaxpayerBuyerRelation(headerEntity);

                /* =========================================
                 * 5. Create TaxpayerTaxProduct Relations
                 * ========================================= */
                createTaxpayerTaxProductRelations(headerEntity, invoice);

                log.info(
                        "Invoice saved successfully. headerId={}, systemStatus=READY_TO_SEND",
                        headerId
                );

            } catch (Exception ex) {
                log.error(
                        "Error saving invoice to database. headerId={}, invoice={}",
                        headerId,
                        invoice,
                        ex
                );
                // deliberately continue with next invoice
            }
        }

        return savedHeaderIds;
    }



    /**
     * Convert InvoiceDto to ElectronicInvoiceHeaderEntity
     */
    private ElectronicInvoiceHeaderEntity convertInvoiceDtoToEntity(InvoiceDto invoice) {
        InvoiceHeaderDto headerDto = invoice.getHeader();
        if (headerDto == null) {
            throw new IllegalArgumentException("Invoice header cannot be null");
        }
        
        ElectronicInvoiceHeaderEntity entity = new ElectronicInvoiceHeaderEntity();
        
        // Basic fields
        // taxId: generate if not provided
        String taxId = headerDto.getTaxid();
        if (taxId == null || taxId.isBlank()) {
            taxId = generateTaxId();
            log.info("Generated taxId: {}", taxId);
        }
        entity.setTaxId(taxId);
        
        entity.setInno(headerDto.getInno() != null ? headerDto.getInno() : "");
        entity.setIndatim(headerDto.getIndatim());
        entity.setM2Indati(headerDto.getIndati2m());
        
        // inp (pattern number): required field - must be set
        if (headerDto.getInp() == null) {
            throw new IllegalArgumentException("Field 'inp' (pattern number) is required but not provided");
        }
        entity.setInp(headerDto.getInp());
        
        entity.setIrtaxid(headerDto.getIrtaxid());
        
        // Seller information
        entity.setTins(headerDto.getTins());
        entity.setSbc(headerDto.getSbc());
        
        // Buyer information (may be empty if skipBuyerInfo is true)
        entity.setBid(headerDto.getBid());
        entity.setTinb(headerDto.getTinb());
        entity.setBbc(headerDto.getBbc());
        entity.setBpc(headerDto.getBpc());
        entity.setBpn(headerDto.getBpn());
        
        // Financial totals
        entity.setTprdis(headerDto.getTprdis());
        entity.setTdis(headerDto.getTdis());
        entity.setTadis(headerDto.getTadis());
        entity.setTvam(headerDto.getTvam());
        entity.setTodam(headerDto.getTodam());
        entity.setTbill(headerDto.getTbill());
        entity.setTvop(headerDto.getTvop());
        entity.setTax17(headerDto.getTax17());
        entity.setCap(headerDto.getCap());
        entity.setInsp(headerDto.getInsp());
        
        // Convert enums
        if (headerDto.getInty() != null) {
            entity.setInty(ir.tax.trustedtaxpayers.beans.model.InvoiceType.fromValue(headerDto.getInty()));
        }
        if (headerDto.getIns() != null) {
            entity.setIns(ir.tax.trustedtaxpayers.beans.model.InvoiceSubject.fromValue(headerDto.getIns()));
        }
        if (headerDto.getTob() != null) {
            entity.setTob(ir.tax.trustedtaxpayers.beans.model.PersonType.fromValue(headerDto.getTob()));
        }
        if (headerDto.getSetm() != null) {
            entity.setSetm(ir.tax.trustedtaxpayers.beans.model.SettlementMethod.fromValue(headerDto.getSetm()));
        }
        if (headerDto.getFt() != null) {
            entity.setFt(FlightType.fromValue(headerDto.getFt()));
        }
        
        // Template code - find by inty
        String templateCode = null;
        if (headerDto.getInty() != null) {
            var templateOpt = templateLoaderService.loadFirstTemplateByInty(headerDto.getInty());
            if (templateOpt.isPresent()) {
                templateCode = templateOpt.get().getCode();
            }
        }
        if (templateCode == null) {
            templateCode = "DEFAULT";
            log.warn("No template found for inty: {}, using default template code", headerDto.getInty());
        }
        entity.setTemplateCode(templateCode);
        
        // Set seller/buyer taxpayers
        ensureSellerBuyer(entity);
        
        return entity;
    }

    /**
     * Ensure seller and buyer taxpayers are set
     * اگر buyer خالی باشد (skipBuyerInfo = true)، buyer taxpayer set نمی‌شود
     */
    private void ensureSellerBuyer(ElectronicInvoiceHeaderEntity headerEntity) {
        // Seller (required) - استفاده از tins (شناسه اقتصادی فروشنده)
        String tins = headerEntity.getTins();
        if (tins != null && !tins.isBlank()) {
            TaxpayerEntity seller = taxpayerService.findByTaxId(tins)
                    .orElseGet(() -> taxpayerService.save(TaxpayerEntity.builder()
                            .taxId(tins)
                            .name("UNKNOWN SELLER")
                            .type(null)
                            .build()));
            /*headerEntity.setSellerTaxpayer(seller);*/
        }

        // Buyer (optional) - اول از tinb (شناسه اقتصادی خریدار) استفاده می‌کنیم، اگر نبود از bid (شناسه ملی خریدار)
        // اگر هر دو خالی باشند، buyer taxpayer set نمی‌شود (skipBuyerInfo = true)
        String tinb = headerEntity.getTinb();
        String bid = headerEntity.getBid();
        
        // اگر هر دو خالی باشند، buyer را set نمی‌کنیم
        if ((tinb == null || tinb.isBlank()) && (bid == null || bid.isBlank())) {
            log.info("Buyer information is empty, skipping buyer taxpayer setup");
            return;
        }
        
        TaxpayerEntity buyer = null;
        
        // اول سعی می‌کنیم با tinb (شناسه اقتصادی) پیدا کنیم
        if (tinb != null && !tinb.isBlank()) {
            buyer = taxpayerService.findByTaxId(tinb)
                    .orElse(null);
        }
        
        // اگر با tinb پیدا نشد و bid وجود دارد، با bid (شناسه ملی) جستجو می‌کنیم
        if (buyer == null && bid != null && !bid.isBlank()) {
            buyer = taxpayerService.findByNationalId(bid)
                    .orElse(null);
        }
        
        // اگر هنوز پیدا نشد، یک buyer جدید ایجاد می‌کنیم
        if (buyer == null) {
            String buyerIdentifier = tinb != null && !tinb.isBlank() ? tinb : (bid != null && !bid.isBlank() ? bid : null);
            if (buyerIdentifier != null) {
                TaxpayerEntity newBuyer = TaxpayerEntity.builder()
                        .taxId(buyerIdentifier.startsWith("BUYER-") ? buyerIdentifier : "BUYER-" + buyerIdentifier)
                        .nationalId(bid != null && !bid.isBlank() ? bid : null)
                        .name("UNKNOWN BUYER")
                        .type(null)
                        .build();
                UUID buyerId = taxpayerService.create(newBuyer);
                buyer = taxpayerService.findById(buyerId).orElse(null);
            }
        }
        
        /*if (buyer != null) {
            headerEntity.setBuyerTaxpayer(buyer);
        }*/
    }

    /**
     * Convert InvoiceBodyDto to ElectronicInvoiceItemEntity
     */
    private ElectronicInvoiceItemEntity convertBodyDtoToEntity(
            InvoiceBodyDto bodyDto,
            UUID headerId, 
            int sequenceNumber) {
        ElectronicInvoiceItemEntity entity = new ElectronicInvoiceItemEntity();
        entity.setHeaderId(headerId);
        entity.setLineNo(sequenceNumber); // استفاده از lineNo به جای sequenceNumber
        entity.setSstid(bodyDto.getSstid());
        entity.setSstt(bodyDto.getSstt());
        entity.setMu(bodyDto.getMu());
        entity.setAm(bodyDto.getAm());
        entity.setFee(bodyDto.getFee());
        entity.setPrdis(bodyDto.getPrdis());
        entity.setDis(bodyDto.getTdis());
        entity.setAdis(bodyDto.getAdis());
        entity.setVra(bodyDto.getVra());
        entity.setVam(bodyDto.getVam());
        entity.setCut(bodyDto.getCut());
        entity.setExr(bodyDto.getExr());
        entity.setCfee(bodyDto.getCfee());
        entity.setCpr(bodyDto.getCpr());
        entity.setVba(bodyDto.getVba());
        // فیلدهای زیر در entity وجود ندارند و حذف شدند:
        // - cdis, cadis, cvam, ctonw, ctorv, ctocv
        return entity;
    }

    /**
     * Update invoice status to SENT after successful submission
     */
    @Transactional
    public void updateInvoiceStatusToSent(List<UUID> headerIds) {
        for (UUID headerId : headerIds) {
            try {
                headerService.findById(headerId).ifPresent(header -> {
                    header.setStatus(InvoiceStatus.SENT);
                    headerService.save(header);
                    log.info("Updated invoice status to SENT for headerId: {}", headerId);
                });
                
                // Also update ElectronicInvoiceSystemEntity status
                systemService.findById(headerId).ifPresent(systemEntity -> {
                    systemEntity.setSts("SUBMITTED");
                    systemService.save(systemEntity);
                    log.info("Updated system entity status to SUBMITTED for headerId: {}", headerId);
                });
            } catch (Exception e) {
                log.error("Error updating invoice status to SENT for headerId: {}", headerId, e);
            }
        }
    }

    /**
     * Generate a temporary taxId (22 characters alphanumeric)
     */
    private String generateTaxId() {
        String base = Long.toString(System.currentTimeMillis(), 36).toUpperCase(java.util.Locale.ROOT);
        String rand = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase(java.util.Locale.ROOT);
        String s = (base + rand).replaceAll("[^A-Z0-9]", "");
        int length = Math.min(22, s.length());
        String padded = s.substring(0, length) + "XXXXXXXXXXXXXXXXXXXXXX";
        return padded.substring(0, 22);
    }

    /**
     * ایجاد رکورد TaxpayerBuyer در صورت وجود buyer با taxpayer و buyer ID
     * قبل از ایجاد، بررسی می‌کند که رکوردی با این آیدی‌ها وجود نداشته باشد
     */
    private void createTaxpayerBuyerRelation(ElectronicInvoiceHeaderEntity headerEntity) {
        try {
            TaxpayerEntity sellerTaxpayer = /*headerEntity.getSellerTaxpayer();*/ null;
            TaxpayerEntity buyerTaxpayer = /*headerEntity.getBuyerTaxpayer();*/ null;
            
            // بررسی وجود seller و buyer taxpayer
            if (sellerTaxpayer != null && buyerTaxpayer != null) {
                UUID taxpayerId = sellerTaxpayer.getId();
                UUID buyerId = buyerTaxpayer.getId();
                
                // بررسی وجود رکورد قبلی - اگر وجود داشت، کار خاصی انجام نمی‌دهیم
                taxpayerBuyerService.createRelation(taxpayerId, buyerId);
                log.debug("Created or found existing TaxpayerBuyer relation: taxpayerId={}, buyerId={}", taxpayerId, buyerId);
            } else {
                log.debug("Skipping TaxpayerBuyer relation creation: sellerTaxpayer={}, buyerTaxpayer={}", 
                    sellerTaxpayer != null ? sellerTaxpayer.getId() : null,
                    buyerTaxpayer != null ? buyerTaxpayer.getId() : null);
            }
        } catch (Exception e) {
            log.error("Error creating TaxpayerBuyer relation for invoice headerId: {}", headerEntity.getId(), e);
            // Continue processing even if this fails
        }
    }

    /**
     * ایجاد رکوردهای TaxpayerTaxProduct برای لیست tax products موجود در invoice
     * رکوردهایی که از قبل وجود داشته‌اند را لحاظ نمی‌کند
     */
    private void createTaxpayerTaxProductRelations(ElectronicInvoiceHeaderEntity headerEntity, InvoiceDto invoice) {
        try {
            TaxpayerEntity sellerTaxpayer = /*headerEntity.getSellerTaxpayer();*/ null;
            
            // بررسی وجود seller taxpayer
            if (sellerTaxpayer == null) {
                log.debug("Skipping TaxpayerTaxProduct relation creation: sellerTaxpayer is null");
                return;
            }
            
            UUID taxpayerId = sellerTaxpayer.getId();
            
            // استخراج لیست sstid های منحصر به فرد از invoice items
            Set<String> sstidSet = new HashSet<>();
            if (invoice.getBody() != null) {
                for (var bodyDto : invoice.getBody()) {
                    String sstid = bodyDto.getSstid();
                    if (sstid != null && !sstid.isBlank()) {
                        sstidSet.add(sstid);
                    }
                }
            }
            
            if (sstidSet.isEmpty()) {
                log.debug("No tax products found in invoice items for headerId: {}", headerEntity.getId());
                return;
            }
            
            // یافتن TaxProductEntity ها بر اساس sstid (taxProductId)
            List<UUID> taxProductIds = new ArrayList<>();
            for (String sstid : sstidSet) {
                taxProductRepository.findByTaxProductId(sstid)
                    .ifPresent(taxProduct -> {
                        taxProductIds.add(taxProduct.getId());
                        log.debug("Found TaxProduct: sstid={}, id={}", sstid, taxProduct.getId());
                    });
            }
            
            if (taxProductIds.isEmpty()) {
                log.debug("No TaxProduct entities found for sstids: {}", sstidSet);
                return;
            }
            
            // ایجاد روابط - متد createRelation خودش بررسی می‌کند که رکورد قبلی وجود نداشته باشد
            for (UUID taxProductId : taxProductIds) {
                taxpayerTaxProductService.createRelation(taxpayerId, taxProductId);
                log.debug("Created or found existing TaxpayerTaxProduct relation: taxpayerId={}, taxProductId={}", 
                    taxpayerId, taxProductId);
            }
            
            log.info("Created TaxpayerTaxProduct relations for {} tax products for taxpayerId: {}", 
                taxProductIds.size(), taxpayerId);
        } catch (Exception e) {
            log.error("Error creating TaxpayerTaxProduct relations for invoice headerId: {}", headerEntity.getId(), e);
            // Continue processing even if this fails
        }
    }
}


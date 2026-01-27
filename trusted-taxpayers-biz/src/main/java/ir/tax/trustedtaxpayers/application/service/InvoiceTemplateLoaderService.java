package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * سرویس برای لود کردن الگوهای صورتحساب بر اساس نوع صورتحساب (inty)
 * 
 * <p>این سرویس برای لود کردن الگوهای صورتحساب بر اساس نوع صورتحساب (inty) استفاده می‌شود.</p>
 * 
 * <p>ویژگی‌ها:</p>
 * <ul>
 *   <li>Mapping خودکار بین InvoiceType و TemplateType</li>
 *   <li>Cache برای عملکرد بهتر</li>
 *   <li>Eager loading برای جلوگیری از N+1</li>
 *   <li>فیلتر فقط قالب‌های فعال</li>
 * </ul>
 * 
 * @author Ghazi-a
 * @since 1.0
 */
@Service
public class InvoiceTemplateLoaderService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceTemplateLoaderService.class);
    
    private final TemplateRepository templateRepository;
    
    // Mapping بین InvoiceType و TemplateType
    // InvoiceType.TYPE1_SALES (inty=1) -> TemplateType P01
    // InvoiceType.TYPE2_FX (inty=2) -> TemplateType P02
    // و غیره...
    private static final Map<Integer, String> INVOICE_TYPE_TO_TEMPLATE_TYPE_MAP = Map.ofEntries(
        Map.entry(1, "P01"),  // TYPE1_SALES -> P01 (فروش)
        Map.entry(2, "P02"),  // TYPE2_FX -> P02 (فروش ارز)
        Map.entry(3, "P03"),  // TYPE3_GOLD -> P03 (طلا و جواهر)
        Map.entry(4, "P04"),  // TYPE4_CONTRACT -> P04 (پیمانکاری)
        Map.entry(5, "P05"),  // TYPE5_UTILITY -> P05 (قبوض خدماتی)
        Map.entry(6, "P06"),  // TYPE6_AIR -> P06 (بلیط هواپیما)
        Map.entry(7, "P07"),  // TYPE7_EXPORT -> P07 (صادرات)
        Map.entry(8, "P08"),  // TYPE8_TEMPLATE -> P08
        Map.entry(9, "P09"),  // TYPE9_TEMPLATE -> P09
        Map.entry(10, "P10"), // TYPE10_TEMPLATE -> P10
        Map.entry(11, "P11"), // TYPE11_TEMPLATE -> P11
        Map.entry(12, "P12"), // TYPE12_TEMPLATE -> P12
        Map.entry(13, "P13"), // TYPE13_TEMPLATE -> P13
        Map.entry(14, "P14"), // TYPE14_TEMPLATE -> P14
        Map.entry(15, "P15"), // TYPE15_TEMPLATE -> P15
        Map.entry(16, "P16")  // TYPE16_TEMPLATE -> P16
    );
    
    public InvoiceTemplateLoaderService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * لود کردن templates بر اساس InvoiceType enum
     * 
     * @param invoiceType نوع صورتحساب
     * @return لیست templates مربوط به این نوع
     */
    @Transactional(readOnly = true)
    public List<TemplateEntity> loadTemplatesByInvoiceType(InvoiceType invoiceType) {
        if (invoiceType == null) {
            logger.warn("InvoiceType is null, returning empty list");
            return Collections.emptyList();
        }
        
        return loadTemplatesByInty(invoiceType.getValue());
    }

    /**
     * لود کردن templates بر اساس inty (عدد)
     * این متد اصلی برای لود کردن templates است.
     * 
     * @param inty نوع صورتحساب (1-16)
     * @return لیست templates مربوط به این نوع (فقط فعال)
     */
    @Cacheable(cacheName = "templates", keyPrefix = "inty")
    @Transactional(readOnly = true)
    public List<TemplateEntity> loadTemplatesByInty(Integer inty) {
        if (inty == null) {
            logger.warn("Inty is null, returning empty list");
            return Collections.emptyList();
        }
        
        // پیدا کردن TemplateType مربوط به این inty
        String templateTypeCode = getTemplateTypeCode(inty);
        if (templateTypeCode == null) {
            logger.warn("No TemplateType found for inty: {}", inty);
            return Collections.emptyList();
        }
        
        // توجه: findByTemplateTypeCodeAndIsActiveTrue دیگر وجود ندارد چون templateTypeCode از TemplateEntity حذف شده است
        // این متد نیاز به بازنویسی دارد تا بدون templateTypeCode در TemplateEntity کار کند
        // فعلاً لیست خالی برمی‌گردانیم چون دیگر نمی‌توان templateTypeCode را از TemplateEntity جستجو کرد
        List<TemplateEntity> templates = Collections.emptyList();
        
        logger.debug("Loaded {} active templates for inty={} (templateType={})", 
            templates.size(), inty, templateTypeCode);
        
        return templates;
    }

    /**
     * لود کردن یک template خاص بر اساس inty و template code
     * 
     * @param inty نوع صورتحساب
     * @param templateCode کد template
     * @return Optional template
     */
    @Transactional(readOnly = true)
    public Optional<TemplateEntity> loadTemplateByIntyAndCode(Integer inty, String templateCode) {
        if (inty == null || templateCode == null) {
            return Optional.empty();
        }
        
        List<TemplateEntity> templates = loadTemplatesByInty(inty);
        
        return templates.stream()
            .filter(t -> templateCode.equals(t.getCode()))
            .findFirst();
    }

    /**
     * لود کردن اولین template فعال برای یک نوع صورتحساب
     * 
     * @param inty نوع صورتحساب
     * @return Optional template
     */
    @Transactional(readOnly = true)
    public Optional<TemplateEntity> loadFirstTemplateByInty(Integer inty) {
        List<TemplateEntity> templates = loadTemplatesByInty(inty);
        return templates.stream().findFirst();
    }

    /**
     * بررسی وجود template برای یک نوع صورتحساب
     * 
     * @param inty نوع صورتحساب
     * @return true اگر template وجود دارد
     */
    @Transactional(readOnly = true)
    public boolean hasTemplatesForInty(Integer inty) {
        List<TemplateEntity> templates = loadTemplatesByInty(inty);
        return !templates.isEmpty();
    }

    /**
     * دریافت TemplateType code بر اساس inty
     * 
     * @param inty نوع صورتحساب
     * @return TemplateType code یا null
     */
    public String getTemplateTypeCode(Integer inty) {
        return INVOICE_TYPE_TO_TEMPLATE_TYPE_MAP.get(inty);
    }

    /**
     * دریافت inty بر اساس TemplateType code
     * 
     * @param templateTypeCode کد TemplateType
     * @return inty یا null
     */
    public Integer getIntyByTemplateTypeCode(String templateTypeCode) {
        return INVOICE_TYPE_TO_TEMPLATE_TYPE_MAP.entrySet().stream()
            .filter(entry -> entry.getValue().equals(templateTypeCode))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    /**
     * دریافت تمام mapping ها
     * 
     * @return Map از inty به TemplateType code
     */
    public Map<Integer, String> getAllMappings() {
        return Collections.unmodifiableMap(INVOICE_TYPE_TO_TEMPLATE_TYPE_MAP);
    }

    /**
     * Invalidate cache برای یک نوع صورتحساب
     * 
     * @param inty نوع صورتحساب
     */
    @CacheEvict(cacheName = "templates", allEntries = true)
    public void invalidateCache(Integer inty) {
        logger.debug("Cache invalidated for inty={}", inty);
    }

    /**
     * Invalidate تمام cache ها
     */
    @CacheEvict(cacheName = "templates", allEntries = true)
    public void invalidateAllCache() {
        logger.info("All template caches invalidated");
    }

    /**
     * Preload تمام templates برای performance بهتر
     * این متد می‌تواند در startup یا warmup استفاده شود
     */
    @Transactional(readOnly = true)
    public void preloadAllTemplates() {
        logger.info("Preloading all templates...");
        
        INVOICE_TYPE_TO_TEMPLATE_TYPE_MAP.keySet().forEach(inty -> {
            try {
                loadTemplatesByInty(inty);
                logger.debug("Preloaded templates for inty={}", inty);
            } catch (Exception e) {
                logger.warn("Failed to preload templates for inty={}: {}", inty, e.getMessage());
            }
        });
        
        logger.info("Preloading completed");
    }
}



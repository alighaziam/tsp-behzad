package ir.tax.trustedtaxpayers.core.exception;

import java.io.Serial;

/**
 * Exception برای زمانی که منبع مورد نظر یافت نشود
 * 
 * <p>این exception معمولاً زمانی throw می‌شود که:</p>
 * <ul>
 *   <li>Entity با ID مشخص یافت نشود</li>
 *   <li>منبع مورد نیاز برای عملیات وجود نداشته باشد</li>
 * </ul>
 */
public class ResourceNotFoundException extends RuntimeException {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor با پیام خطا
     * 
     * @param message پیام خطا
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructor با پیام خطا و علت
     * 
     * @param message پیام خطا
     * @param cause علت exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructor برای ایجاد exception با ID
     * 
     * @param resourceType نوع منبع (مثلاً "User", "Order")
     * @param id شناسه منبع
     */
    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s with ID '%s' not found", resourceType, id));
    }
}


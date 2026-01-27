package ir.tax.trustedtaxpayers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner برای flush کردن Redis در صورت نیاز
 * 
 * برای فعال‌سازی، property زیر را در application.yml تنظیم کنید:
 * redis.flush-on-startup: true
 * 
 * ⚠️ هشدار: این عملیات تمام داده‌های Redis را پاک می‌کند!
 */
@Slf4j
@Component
@Order(1) // اجرا در ابتدای startup
@ConditionalOnProperty(name = "redis.flush-on-startup", havingValue = "true", matchIfMissing = false)
public class RedisFlushRunner implements CommandLineRunner {
	
	private final RedisManagementService redisManagementService;

	public RedisFlushRunner(RedisManagementService redisManagementService) {
		this.redisManagementService = redisManagementService;
	}

	@Override
	public void run(String... args) {
		log.warn("==========================================");
		log.warn("Redis FLUSH ON STARTUP is ENABLED!");
		log.warn("This will delete ALL data from Redis!");
		log.warn("==========================================");
		
		try {
			long deletedCount = redisManagementService.flushAll();
			log.info("Redis flush completed successfully. Deleted {} keys.", deletedCount);
		} catch (Exception e) {
			log.error("Failed to flush Redis on startup: {}", e.getMessage(), e);
			// Don't fail startup if Redis flush fails
		}
	}
}


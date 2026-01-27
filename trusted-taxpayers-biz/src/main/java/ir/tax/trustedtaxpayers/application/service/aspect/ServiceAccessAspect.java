package ir.tax.trustedtaxpayers.application.service.aspect;

import ir.tax.trustedtaxpayers.application.exception.ServiceAccessException;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessAuditService;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessMetrics;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.application.service.ServiceQuotaInfo;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.application.service.annotation.RequiresService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserInvoicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Aspect for checking service access before method execution.
 * This aspect checks if the taxpayer has access to the required service
 * based on their active subscription.
 * 
 * Performance improvements:
 * - Caching for user ID and taxpayer ID lookups
 * - Reduced database queries
 */
@Aspect
@Component
@Order(1) // Execute before rate limiting
@RequiredArgsConstructor
public class ServiceAccessAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAccessAspect.class);
    
    private final ServiceAccessService serviceAccessService;
    private final UserRoleService userRoleService;
    private final UserInvoicePreferenceRepository preferenceRepository;
    private final ServiceAccessMetrics metrics;
    private final ServiceAccessAuditService auditService;
    
    // Cache for username -> userId mapping (TTL: 5 minutes)
    private final Map<String, CachedUserId> userIdCache = new ConcurrentHashMap<>();
    private static final long USER_ID_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
    
    // Cache for userId -> taxpayerId mapping (TTL: 5 minutes)
    private final Map<UUID, CachedTaxpayerId> taxpayerIdCache = new ConcurrentHashMap<>();
    private static final long TAXPAYER_ID_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
    
    // Cache for service access checks (TTL: 2 minutes) - reduces DB queries for frequent checks
    private final Map<String, CachedServiceAccess> serviceAccessCache = new ConcurrentHashMap<>();
    private static final long SERVICE_ACCESS_CACHE_TTL = 2 * 60 * 1000; // 2 minutes
    
    // Periodic cache cleanup to prevent memory leaks
    private static final long CACHE_CLEANUP_INTERVAL = 10 * 60 * 1000; // 10 minutes
    private volatile long lastCacheCleanup = System.currentTimeMillis();
    
    private static class CachedUserId {
        final UUID userId;
        final long timestamp;
        
        CachedUserId(UUID userId) {
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > USER_ID_CACHE_TTL;
        }
    }
    
    private static class CachedTaxpayerId {
        final UUID taxpayerId;
        final long timestamp;
        
        CachedTaxpayerId(UUID taxpayerId) {
            this.taxpayerId = taxpayerId;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TAXPAYER_ID_CACHE_TTL;
        }
    }
    
    private static class CachedServiceAccess {
        final boolean hasAccess;
        final long timestamp;
        
        CachedServiceAccess(boolean hasAccess) {
            this.hasAccess = hasAccess;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > SERVICE_ACCESS_CACHE_TTL;
        }
    }

    @Before("@annotation(ir.tax.trustedtaxpayers.application.service.annotation.RequiresService) || " +
            "@within(ir.tax.trustedtaxpayers.application.service.annotation.RequiresService)")
    public void checkServiceAccess(JoinPoint joinPoint) {
        // Periodic cache cleanup to prevent memory leaks
        cleanupExpiredCacheEntries();
        
        long startTime = System.currentTimeMillis();
        RequiresService annotation = getAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }

        String serviceCode = annotation.value();
        boolean checkLimit = annotation.checkLimit();
        
        UUID taxpayerId = getTaxpayerIdFromRequest();
        UUID userId = getUserIdFromRequest();
        
        if (taxpayerId == null) {
            logger.warn("Taxpayer ID not found in request for service access check: {}", serviceCode);
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordAccess(null, serviceCode, false, duration);
            auditService.auditServiceAccess(null, userId, serviceCode, "ACCESS_CHECK", 
                    false, "TAXPAYER_ID_NOT_FOUND", null);
            throw new ServiceAccessException("شناسه مودی در درخواست یافت نشد", serviceCode);
        }

        try {
            // Check cache first for service access
            String accessCacheKey = buildServiceAccessCacheKey(taxpayerId, serviceCode);
            CachedServiceAccess cachedAccess = serviceAccessCache.get(accessCacheKey);
            boolean hasAccess;
            
            if (cachedAccess != null && !cachedAccess.isExpired()) {
                hasAccess = cachedAccess.hasAccess;
                logger.debug("Service access cache HIT for taxpayer {} and service {}", taxpayerId, serviceCode);
            } else {
                // Cache miss - check service access
                hasAccess = serviceAccessService.hasServiceAccess(taxpayerId, serviceCode);
                serviceAccessCache.put(accessCacheKey, new CachedServiceAccess(hasAccess));
                logger.debug("Service access cache MISS for taxpayer {} and service {}", taxpayerId, serviceCode);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (!hasAccess) {
                logger.warn("Taxpayer {} does not have access to service {}", taxpayerId, serviceCode);
                metrics.recordAccess(taxpayerId, serviceCode, false, duration);
                auditService.auditServiceAccess(taxpayerId, userId, serviceCode, "ACCESS_CHECK", 
                        false, "NO_ACCESS", null);
                throw new ServiceAccessException(
                    annotation.message().isEmpty() ? "دسترسی به این سرویس برای شما فعال نیست" : annotation.message(),
                    serviceCode,
                    "NO_ACCESS"
                );
            }

            // Check service limits if required
            if (checkLimit) {
                boolean withinLimit = serviceAccessService.checkServiceLimit(taxpayerId, serviceCode);
                if (!withinLimit) {
                    logger.warn("Taxpayer {} has exceeded limit for service {}", taxpayerId, serviceCode);
                    
                    // Get quota info for auditing
                    ServiceQuotaInfo quota = serviceAccessService.getServiceQuota(taxpayerId, serviceCode);
                    auditService.auditLimitExceeded(taxpayerId, userId, serviceCode, 
                            quota.used(), quota.limitValue().longValue(), quota.limitUnit());
                    
                    metrics.recordLimitExceeded(taxpayerId, serviceCode);
                    metrics.recordAccess(taxpayerId, serviceCode, false, duration);
                    auditService.auditServiceAccess(taxpayerId, userId, serviceCode, "ACCESS_CHECK", 
                            false, "LIMIT_EXCEEDED", 
                            java.util.Map.of("used", quota.used(), "limit", quota.limitValue()));
                    throw new ServiceAccessException(
                        "حد مجاز استفاده از این سرویس به پایان رسیده است",
                        serviceCode,
                        "LIMIT_EXCEEDED"
                    );
                }
            }
            
            // Record successful access
            metrics.recordAccess(taxpayerId, serviceCode, true, duration);
            auditService.auditServiceAccess(taxpayerId, userId, serviceCode, "ACCESS_CHECK", 
                    true, null, null);
            
        } catch (ServiceAccessException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordAccess(taxpayerId, serviceCode, false, duration);
            auditService.auditServiceAccess(taxpayerId, userId, serviceCode, "ACCESS_CHECK", 
                    false, "ERROR: " + e.getMessage(), null);
            logger.error("Error checking service access for taxpayer {} and service {}", taxpayerId, serviceCode, e);
            throw new ServiceAccessException(
                "خطا در بررسی دسترسی به سرویس",
                serviceCode,
                "ERROR"
            );
        }
    }

    private UUID getUserIdFromRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            var request = attributes.getRequest();
            String username = resolveUsername(request);
            if (username == null || username.isBlank()) {
                return null;
            }
            
            // Check cache first
            CachedUserId cached = userIdCache.get(username);
            if (cached != null && !cached.isExpired()) {
                return cached.userId;
            }
            
            // Cache miss - query database
            UUID userId = userRoleService.findUserIdByUsername(username).orElse(null);
            if (userId != null) {
                userIdCache.put(username, new CachedUserId(userId));
            }
            
            return userId;
        } catch (Exception e) {
            logger.debug("Error extracting user ID from request", e);
        }
        
        return null;
    }

    private RequiresService getAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // Check method annotation first
        RequiresService annotation = method.getAnnotation(RequiresService.class);
        if (annotation != null) {
            return annotation;
        }
        
        // Check class annotation
        return joinPoint.getTarget().getClass().getAnnotation(RequiresService.class);
    }

    private UUID getTaxpayerIdFromRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            var request = attributes.getRequest();
            
            // 1. Try to get taxpayer ID from request parameter
            String taxpayerIdParam = request.getParameter("taxpayerId");
            if (taxpayerIdParam != null && !taxpayerIdParam.isBlank()) {
                try {
                    return UUID.fromString(taxpayerIdParam);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid taxpayerId parameter: {}", taxpayerIdParam);
                }
            }
            
            // 2. Try to get from path variable (e.g., /api/invoices/{taxpayerId}/...)
            // Optimized path parsing - check common patterns first
            String path = request.getRequestURI();
            
            // Fast path: check for /taxpayer/{uuid} pattern
            int taxpayerIndex = path.indexOf("/taxpayer/");
            if (taxpayerIndex >= 0) {
                int start = taxpayerIndex + "/taxpayer/".length();
                int end = path.indexOf("/", start);
                if (end < 0) {
                    end = path.length();
                }
                if (end > start) {
                    try {
                        return UUID.fromString(path.substring(start, end));
                    } catch (IllegalArgumentException e) {
                        // Not a valid UUID, continue to next method
                    }
                }
            }
            
            // Fallback: parse path parts (slower but more flexible)
            String[] pathParts = path.split("/");
            for (int i = 0; i < pathParts.length - 1; i++) {
                if ("taxpayer".equals(pathParts[i])) {
                    try {
                        return UUID.fromString(pathParts[i + 1]);
                    } catch (IllegalArgumentException e) {
                        // Not a valid UUID
                    }
                }
            }
            
            // 3. Try to extract from authenticated user context
            String username = resolveUsername(request);
            if (username != null && !username.isBlank()) {
                UUID userId = getUserIdFromRequest(); // Uses cache
                if (userId != null) {
                    // Check cache for taxpayer ID
                    CachedTaxpayerId cached = taxpayerIdCache.get(userId);
                    if (cached != null && !cached.isExpired()) {
                        return cached.taxpayerId;
                    }
                    
                    // Cache miss - query database
                    UUID taxpayerId = preferenceRepository.findByUserId(userId).stream()
                            .findFirst()
                            .map(pref -> pref.getTaxpayerId())
                            .orElse(null);
                    
                    if (taxpayerId != null) {
                        taxpayerIdCache.put(userId, new CachedTaxpayerId(taxpayerId));
                    }
                    
                    return taxpayerId;
                }
            }
            
            // 4. Try to get from request body (for POST/PUT requests)
            // This would require reading the request body, which is complex
            // For now, we skip this and rely on path/parameter extraction
            
        } catch (Exception e) {
            logger.error("Error extracting taxpayer ID from request", e);
        }
        
        return null;
    }
    
    /**
     * Clean up expired cache entries to prevent memory leaks.
     * This method is called periodically during service access checks.
     */
    private void cleanupExpiredCacheEntries() {
        long now = System.currentTimeMillis();
        // Only cleanup every 10 minutes to avoid performance impact
        if (now - lastCacheCleanup < CACHE_CLEANUP_INTERVAL) {
            return;
        }
        
        lastCacheCleanup = now;
        
        // Cleanup userId cache
        userIdCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Cleanup taxpayerId cache
        taxpayerIdCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Cleanup service access cache
        serviceAccessCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        if (logger.isDebugEnabled()) {
            logger.debug("Cache cleanup completed. userIdCache size: {}, taxpayerIdCache size: {}, serviceAccessCache size: {}", 
                    userIdCache.size(), taxpayerIdCache.size(), serviceAccessCache.size());
        }
    }
    
    /**
     * Build cache key for service access checks
     */
    private String buildServiceAccessCacheKey(UUID taxpayerId, String serviceCode) {
        return "service_access:" + taxpayerId + ":" + serviceCode;
    }
    
    /**
     * Invalidate service access cache for a specific taxpayer.
     * This should be called when subscription or service access changes.
     * 
     * @param taxpayerId The taxpayer ID whose cache should be invalidated
     */
    public void invalidateServiceAccessCache(UUID taxpayerId) {
        if (taxpayerId == null) {
            return;
        }
        
        // Remove all cache entries for this taxpayer
        String prefix = "service_access:" + taxpayerId + ":";
        serviceAccessCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        
        logger.debug("Service access cache invalidated for taxpayer: {}", taxpayerId);
    }
    
    /**
     * Invalidate service access cache for a specific taxpayer and service.
     * 
     * @param taxpayerId The taxpayer ID
     * @param serviceCode The service code
     */
    public void invalidateServiceAccessCache(UUID taxpayerId, String serviceCode) {
        if (taxpayerId == null || serviceCode == null || serviceCode.isBlank()) {
            return;
        }
        
        String key = buildServiceAccessCacheKey(taxpayerId, serviceCode);
        serviceAccessCache.remove(key);
        
        logger.debug("Service access cache invalidated for taxpayer: {} and service: {}", taxpayerId, serviceCode);
    }
    
    /**
     * Invalidate all service access caches.
     * Use with caution - this clears all cached service access checks.
     */
    public void invalidateAllServiceAccessCache() {
        int size = serviceAccessCache.size();
        serviceAccessCache.clear();
        logger.info("All service access caches invalidated. Cleared {} entries.", size);
    }
    
    /**
     * Resolve username from request or security context.
     * Similar to CurrentUserResolver but doesn't require REST module dependency.
     */
    private String resolveUsername(jakarta.servlet.http.HttpServletRequest request) {
        if (request != null) {
            String headerUser = request.getHeader("X-User");
            if (headerUser != null && !headerUser.isBlank()) {
                return headerUser;
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}


package ir.tax.trustedtaxpayers.application.cache;

import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class for generating cache keys
 * این کلاس برای ساخت cache key از class name, method name و parameters استفاده می‌کند
 */
public class CacheKeyGenerator {
    
    /**
     * ساخت cache key از class و method
     */
    public static String generateCacheKey(Class<?> clazz, Method method, Object... args) {
        String className = clazz.getSimpleName();
        String methodName = method.getName();
        
        StringBuilder key = new StringBuilder();
        key.append(className).append("::").append(methodName);
        
        if (args != null && args.length > 0) {
            key.append("::");
            String params = Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining("::"));
            key.append(params);
        }
        
        return key.toString();
    }
    
    /**
     * ساخت cache name از class و method
     */
    public static String generateCacheName(Class<?> clazz, Method method) {
        String className = clazz.getSimpleName().toLowerCase();
        String methodName = method.getName().toLowerCase();
        return className + "-" + methodName;
    }
    
    /**
     * ساخت cache name از class
     */
    public static String generateCacheName(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase().replace("service", "")
            .replace("repository", "")
            .replace("controller", "");
    }
    
    /**
     * ساخت cache key با prefix
     */
    public static String generateCacheKey(String prefix, Object... args) {
        if (!StringUtils.hasText(prefix)) {
            return generateKeyFromArgs(args);
        }
        
        if (args != null && args.length > 0) {
            return prefix + "::" + generateKeyFromArgs(args);
        }
        
        return prefix;
    }
    
    /**
     * ساخت key از arguments
     */
    private static String generateKeyFromArgs(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        
        return Arrays.stream(args)
            .map(arg -> arg != null ? arg.toString() : "null")
            .collect(Collectors.joining("::"));
    }
}



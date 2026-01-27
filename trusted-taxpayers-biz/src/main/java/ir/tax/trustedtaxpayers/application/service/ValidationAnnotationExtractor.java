package ir.tax.trustedtaxpayers.application.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * کلاس کمکی برای استخراج اطلاعات از Jakarta Validation Annotations
 */
public class ValidationAnnotationExtractor {

    /**
     * استخراج minLength از @Size
     */
    public static Integer extractMinLength(Field field) {
        try {
            Class<?> sizeClass = Class.forName("jakarta.validation.constraints.Size");
            Annotation sizeAnnotation = field.getAnnotation((Class<? extends Annotation>) sizeClass);
            if (sizeAnnotation != null) {
                try {
                    int min = (Integer) sizeClass.getMethod("min").invoke(sizeAnnotation);
                    return min > 0 ? min : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        // بررسی javax.validation
        try {
            Class<?> sizeClass = Class.forName("javax.validation.constraints.Size");
            Annotation sizeAnnotation = field.getAnnotation((Class<? extends Annotation>) sizeClass);
            if (sizeAnnotation != null) {
                try {
                    int min = (Integer) sizeClass.getMethod("min").invoke(sizeAnnotation);
                    return min > 0 ? min : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return null;
    }

    /**
     * استخراج maxLength از @Size
     */
    public static Integer extractMaxLength(Field field) {
        try {
            Class<?> sizeClass = Class.forName("jakarta.validation.constraints.Size");
            Annotation sizeAnnotation = field.getAnnotation((Class<? extends Annotation>) sizeClass);
            if (sizeAnnotation != null) {
                try {
                    int max = (Integer) sizeClass.getMethod("max").invoke(sizeAnnotation);
                    return max > 0 ? max : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        // بررسی javax.validation
        try {
            Class<?> sizeClass = Class.forName("javax.validation.constraints.Size");
            Annotation sizeAnnotation = field.getAnnotation((Class<? extends Annotation>) sizeClass);
            if (sizeAnnotation != null) {
                try {
                    int max = (Integer) sizeClass.getMethod("max").invoke(sizeAnnotation);
                    return max > 0 ? max : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return null;
    }

    /**
     * استخراج pattern از @Pattern
     */
    public static String extractPattern(Field field) {
        try {
            Class<?> patternClass = Class.forName("jakarta.validation.constraints.Pattern");
            Annotation patternAnnotation = field.getAnnotation((Class<? extends Annotation>) patternClass);
            if (patternAnnotation != null) {
                try {
                    String regex = (String) patternClass.getMethod("regexp").invoke(patternAnnotation);
                    return regex != null && !regex.isEmpty() ? regex : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        // بررسی javax.validation
        try {
            Class<?> patternClass = Class.forName("javax.validation.constraints.Pattern");
            Annotation patternAnnotation = field.getAnnotation((Class<? extends Annotation>) patternClass);
            if (patternAnnotation != null) {
                try {
                    String regex = (String) patternClass.getMethod("regexp").invoke(patternAnnotation);
                    return regex != null && !regex.isEmpty() ? regex : null;
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return null;
    }

    /**
     * استخراج validation message از annotation
     */
    public static String extractValidationMessage(Field field) {
        // بررسی @Size
        try {
            Class<?> sizeClass = Class.forName("jakarta.validation.constraints.Size");
            Annotation sizeAnnotation = field.getAnnotation((Class<? extends Annotation>) sizeClass);
            if (sizeAnnotation != null) {
                try {
                    String message = (String) sizeClass.getMethod("message").invoke(sizeAnnotation);
                    if (message != null && !message.isEmpty() && !message.startsWith("{") && !message.endsWith("}")) {
                        return message;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        // بررسی @Pattern
        try {
            Class<?> patternClass = Class.forName("jakarta.validation.constraints.Pattern");
            Annotation patternAnnotation = field.getAnnotation((Class<? extends Annotation>) patternClass);
            if (patternAnnotation != null) {
                try {
                    String message = (String) patternClass.getMethod("message").invoke(patternAnnotation);
                    if (message != null && !message.isEmpty() && !message.startsWith("{") && !message.endsWith("}")) {
                        return message;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return null;
    }

    /**
     * بررسی وجود @Email
     */
    public static boolean hasEmailAnnotation(Field field) {
        try {
            Class<?> emailClass = Class.forName("jakarta.validation.constraints.Email");
            if (field.isAnnotationPresent((Class<? extends Annotation>) emailClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        try {
            Class<?> emailClass = Class.forName("javax.validation.constraints.Email");
            if (field.isAnnotationPresent((Class<? extends Annotation>) emailClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        return false;
    }

    /**
     * بررسی وجود @NotNull
     */
    public static boolean hasNotNullAnnotation(Field field) {
        try {
            Class<?> notNullClass = Class.forName("jakarta.validation.constraints.NotNull");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notNullClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        try {
            Class<?> notNullClass = Class.forName("javax.validation.constraints.NotNull");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notNullClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        return false;
    }

    /**
     * بررسی وجود @NotBlank (برای String)
     */
    public static boolean hasNotBlankAnnotation(Field field) {
        try {
            Class<?> notBlankClass = Class.forName("jakarta.validation.constraints.NotBlank");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notBlankClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        try {
            Class<?> notBlankClass = Class.forName("javax.validation.constraints.NotBlank");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notBlankClass)) {
                return true;
            }
        } catch (Exception e) {
            // ignore
        }
        
        return false;
    }
}


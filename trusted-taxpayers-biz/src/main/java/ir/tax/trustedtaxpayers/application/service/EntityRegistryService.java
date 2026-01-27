package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.dto.*;
import ir.tax.trustedtaxpayers.application.dto.RelationType;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * سرویس برای ثبت و مدیریت اطلاعات تمام Entity های سیستم
 * این سرویس اطلاعات Entity ها، رابطه‌های آن‌ها و Schema های مربوطه را نگهداری می‌کند
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityRegistryService {

    private final FormSchemaService formSchemaService;
    private final FormBuilderService formBuilderService;
    
    // Package های رایج برای Entity ها
    private static final String[] ENTITY_PACKAGES = {
        "ir.tax.trustedtaxpayers.infrastructure.jpa.entity",
        "ir.tax.trustedtaxpayers.infrastructure.jpa.entity.base",
        "ir.tax.trustedtaxpayers.model.entity",
        "ir.tax.trustedtaxpayers.domain.entity"
    };

    /**
     * دریافت اطلاعات تمام Entity های سیستم
     * شامل Entity های مرتبط و رابطه‌ها
     */
    public AllEntitiesResponseDto getAllEntities() {
        List<EntityInfoDto> entities = new ArrayList<>();
        
        // پیدا کردن تمام Entity classes
        Set<Class<?>> entityClasses = findAllEntityClasses();
        
        for (Class<?> entityClass : entityClasses) {
            try {
                EntityInfoDto entityInfo = buildEntityInfo(entityClass);
                entities.add(entityInfo);
            } catch (Exception e) {
                log.warn("خطا در پردازش Entity {}: {}", entityClass.getName(), e.getMessage());
            }
        }
        
        return AllEntitiesResponseDto.builder()
                .entities(entities)
                .totalEntities(entities.size())
                .build();
    }

    /**
     * دریافت اطلاعات یک Entity خاص به همراه Entity های مرتبط
     */
    public EntityInfoDto getEntityWithRelations(String entityClassName) {
        try {
            Class<?> entityClass = Class.forName(entityClassName);
            return buildEntityInfo(entityClass);
        } catch (ClassNotFoundException e) {
            log.warn("کلاس Entity یافت نشد: {}", entityClassName);
            return null;
        }
    }

    /**
     * ساخت EntityInfoDto از یک Class
     */
    private EntityInfoDto buildEntityInfo(Class<?> entityClass) {
        // استخراج اطلاعات پایه
        String className = entityClass.getName();
        String simpleName = entityClass.getSimpleName();
        String packageName = entityClass.getPackage() != null ? entityClass.getPackage().getName() : "";
        
        // استخراج نام جدول
        String tableName = extractTableName(entityClass);
        
        // شمارش فیلدها
        int fieldCount = countFields(entityClass);
        
        // استخراج رابطه‌ها
        List<EntityRelationDto> relations = extractRelations(entityClass);
        
        // لیست Entity های مرتبط (بدون تکراری)
        Set<String> relatedEntities = relations.stream()
                .map(EntityRelationDto::getTargetEntityClassName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        return EntityInfoDto.builder()
                .className(className)
                .simpleName(simpleName)
                .tableName(tableName)
                .packageName(packageName)
                .fieldCount(fieldCount)
                .hasSchema(true)
                .relations(relations)
                .relatedEntities(new ArrayList<>(relatedEntities))
                .build();
    }

    /**
     * استخراج نام جدول از Entity
     */
    private String extractTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        
        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity != null && !entity.name().isEmpty()) {
            return entity.name();
        }
        
        return entityClass.getSimpleName().toUpperCase();
    }

    /**
     * شمارش فیلدهای Entity
     */
    private int countFields(Class<?> entityClass) {
        int count = 0;
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            count += currentClass.getDeclaredFields().length;
            currentClass = currentClass.getSuperclass();
        }
        return count;
    }

    /**
     * استخراج رابطه‌های Entity
     */
    private List<EntityRelationDto> extractRelations(Class<?> entityClass) {
        List<EntityRelationDto> relations = new ArrayList<>();
        Set<Field> allFields = getAllFields(entityClass);
        
        for (Field field : allFields) {
            field.setAccessible(true);
            
            EntityRelationDto relation = null;
            
            // بررسی ManyToOne
            if (field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                
                relation = EntityRelationDto.builder()
                        .fieldName(field.getName())
                        .relationType(RelationType.MANY_TO_ONE)
                        .targetEntityClassName(field.getType().getName())
                        .targetEntitySimpleName(field.getType().getSimpleName())
                        .required(!manyToOne.optional())
                        .build();
            }
            
            // بررسی OneToMany
            else if (field.isAnnotationPresent(OneToMany.class)) {
                Class<?> relatedType = getGenericType(field);
                
                relation = EntityRelationDto.builder()
                        .fieldName(field.getName())
                        .relationType(RelationType.ONE_TO_MANY)
                        .targetEntityClassName(relatedType != null ? relatedType.getName() : null)
                        .targetEntitySimpleName(relatedType != null ? relatedType.getSimpleName() : null)
                        .required(false)
                        .build();
            }
            
            // بررسی OneToOne
            else if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                
                relation = EntityRelationDto.builder()
                        .fieldName(field.getName())
                        .relationType(RelationType.ONE_TO_ONE)
                        .targetEntityClassName(field.getType().getName())
                        .targetEntitySimpleName(field.getType().getSimpleName())
                        .required(!oneToOne.optional())
                        .build();
            }
            
            // بررسی ManyToMany
            else if (field.isAnnotationPresent(ManyToMany.class)) {
                Class<?> relatedType = getGenericType(field);
                
                relation = EntityRelationDto.builder()
                        .fieldName(field.getName())
                        .relationType(RelationType.MANY_TO_MANY)
                        .targetEntityClassName(relatedType != null ? relatedType.getName() : null)
                        .targetEntitySimpleName(relatedType != null ? relatedType.getSimpleName() : null)
                        .required(false)
                        .build();
            }
            
            if (relation != null) {
                relations.add(relation);
            }
        }
        
        return relations;
    }

    /**
     * دریافت نوع Generic از Field
     */
    private Class<?> getGenericType(Field field) {
        try {
            java.lang.reflect.Type genericType = field.getGenericType();
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) genericType;
                java.lang.reflect.Type[] actualTypes = paramType.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                    return (Class<?>) actualTypes[0];
                }
            }
        } catch (Exception e) {
            log.debug("خطا در استخراج نوع Generic برای فیلد {}: {}", field.getName(), e.getMessage());
        }
        return null;
    }

    /**
     * دریافت تمام فیلدهای کلاس
     */
    private Set<Field> getAllFields(Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * پیدا کردن تمام Entity classes در package های تعریف شده
     */
    private Set<Class<?>> findAllEntityClasses() {
        Set<Class<?>> entityClasses = new LinkedHashSet<>();
        
        for (String packageName : ENTITY_PACKAGES) {
            try {
                Set<Class<?>> classesInPackage = findClassesInPackage(packageName);
                for (Class<?> clazz : classesInPackage) {
                    if (isEntityClass(clazz)) {
                        entityClasses.add(clazz);
                    }
                }
            } catch (Exception e) {
                log.debug("خطا در اسکن package {}: {}", packageName, e.getMessage());
            }
        }
        
        return entityClasses;
    }

    /**
     * بررسی اینکه آیا کلاس یک Entity است
     */
    private boolean isEntityClass(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class) 
            || clazz.isAnnotationPresent(jakarta.persistence.Entity.class);
    }

    /**
     * پیدا کردن کلاس‌ها در یک package
     * این متد از ClassLoader برای پیدا کردن کلاس‌ها استفاده می‌کند
     */
    private Set<Class<?>> findClassesInPackage(String packageName) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        
        try {
            // استفاده از ClassLoader برای پیدا کردن کلاس‌ها
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            java.util.Enumeration<java.net.URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    findClassesInDirectory(new java.io.File(resource.getFile()), packageName, classes);
                }
            }
        } catch (Exception e) {
            log.debug("خطا در پیدا کردن کلاس‌ها در package {}: {}", packageName, e.getMessage());
        }
        
        return classes;
    }

    /**
     * پیدا کردن کلاس‌ها در یک directory
     */
    private void findClassesInDirectory(java.io.File directory, String packageName, Set<Class<?>> classes) {
        if (!directory.exists()) {
            return;
        }
        
        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // ignore
                }
            }
        }
    }
}


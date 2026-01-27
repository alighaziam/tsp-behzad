package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.service.BaseServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.BaseInfoEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.BaseInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * سرویس مدیریت اطلاعات پایه
 */
@Service
public class BaseInfoService extends BaseServiceImpl<BaseInfoEntity, UUID> {

    private final BaseInfoRepository baseInfoRepository;

    public BaseInfoService(BaseInfoRepository baseInfoRepository) {
        super(baseInfoRepository);
        this.baseInfoRepository = baseInfoRepository;
    }

    /**
     * یافتن اطلاعات پایه بر اساس category که active باشند
     *
     * @param category دسته‌بندی مورد نظر
     * @return لیست اطلاعات پایه
     */
    @Transactional(readOnly = true)
    public List<BaseInfoEntity> findByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return Collections.emptyList();
        }
        String formattedCategory = convertToSnakeCase(category);
        return baseInfoRepository.findByCategory(formattedCategory);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<BaseInfoEntity> findByCategoryAndTitle(String category, String title) {
        return baseInfoRepository.findByCategoryAndTitle(category, title);
    }

    private String convertToSnakeCase(String input) {
        String result = null;

        if (input != null) {
            if (input.contains("_")) {
                result = input.toUpperCase();
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (Character.isUpperCase(c) && i > 0) {
                        builder.append('_');
                    }
                    builder.append(Character.toUpperCase(c));
                }
                result = builder.toString();
            }
        }

        return result;
    }

}



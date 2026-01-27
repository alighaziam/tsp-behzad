package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.core.service.BaseServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateFieldRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TemplateFieldService extends BaseServiceImpl<TemplateFieldEntity, UUID> {
    public TemplateFieldService(TemplateFieldRepository repository) {
        super(repository);
    }
}






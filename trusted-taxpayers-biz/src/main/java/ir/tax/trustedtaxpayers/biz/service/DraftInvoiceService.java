package ir.tax.trustedtaxpayers.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.DraftInvoiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.DraftInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class DraftInvoiceService {
    private final DraftInvoiceRepository repository;
    private final ObjectMapper objectMapper;

    public DraftInvoiceService(DraftInvoiceRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public DraftInvoiceEntity create(String source, String rawText, Map<String, Object> parsed) {
        try {
            log.debug("Creating draft invoice with source: {}", source);
            DraftInvoiceEntity e = new DraftInvoiceEntity();
            e.setSource(source != null ? source : "UNKNOWN");
            e.setRawText(rawText != null ? rawText : "");
            e.setParsedJson(toJson(parsed));
            e.setStatus("DRAFT");

            if (repository == null) {
                log.error("Repository is null");
                throw new IllegalStateException("Repository is not initialized");
            }
            DraftInvoiceEntity saved = repository.save(e);
            if (saved == null) {
                log.error("Repository.save() returned null for draft invoice");
                throw new IllegalStateException("Failed to save draft invoice");
            }
            log.debug("Draft invoice saved successfully with id: {}", saved.getId());
            return saved;
        } catch (Exception ex) {
            log.error("Error creating draft invoice", ex);
            throw ex;
        }
    }

    public Optional<DraftInvoiceEntity> get(String id) {
        return repository.findById(id);
    }

    public DraftInvoiceEntity update(String id, Map<String, Object> parsed) {
        DraftInvoiceEntity e = repository.findById(id).orElseThrow();
        e.setParsedJson(toJson(parsed));
        return repository.save(e);
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) {
            return "{}";
        }
        try {
            if (objectMapper == null) {
                log.error("ObjectMapper is null");
                return "{}";
            }
            String json = objectMapper.writeValueAsString(map);
            return json != null ? json : "{}";
        } catch (Exception e) {
            log.error("Error converting map to JSON", e);
            // Return empty JSON instead of throwing exception
            return "{}";
        }
    }
}



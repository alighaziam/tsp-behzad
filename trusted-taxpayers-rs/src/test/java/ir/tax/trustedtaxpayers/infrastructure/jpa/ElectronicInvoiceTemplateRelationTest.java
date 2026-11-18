package ir.tax.trustedtaxpayers.infrastructure.jpa;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ElectronicInvoiceTemplateRelationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void resolvesInvoiceTemplateByStringBusinessCodeInsteadOfUuidPrimaryKey() {
        String templateCode = "1-SALE_TEMPLATE_1_TYPE1";
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        TemplateEntity template = new TemplateEntity();
        template.setCode(templateCode);
        template.setTitle("Sales template");
        template.setIsActive(true);
        template.setCreatedOn(now);
        entityManager.persist(template);

        ElectronicInvoiceHeaderEntity invoice = new ElectronicInvoiceHeaderEntity();
        invoice.setTaxId("test-tax-id");
        invoice.setTemplateCode(templateCode);
        invoice.setIsActive(true);
        invoice.setCreatedOn(now);
        entityManager.persist(invoice);
        entityManager.flush();

        UUID invoiceId = invoice.getId();
        UUID templateId = template.getId();
        entityManager.clear();

        ElectronicInvoiceHeaderEntity reloaded = entityManager.find(ElectronicInvoiceHeaderEntity.class, invoiceId);

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getTemplate()).isNotNull();
        assertThat(reloaded.getTemplate().getCode()).isEqualTo(templateCode);
        assertThat(reloaded.getTemplate().getId()).isEqualTo(templateId);
        assertThat(reloaded.getTemplate().getId()).isNotEqualTo(invoiceId);
    }
}

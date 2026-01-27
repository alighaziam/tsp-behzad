package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "SERVICE_WEBHOOKS")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWebhookEntity extends BaseEntity {

    @Column(name = "TAXPAYER_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Column(name = "WEBHOOK_URL", length = 500, nullable = false)
    private String webhookUrl;

    @Column(name = "EVENT_TYPES", length = 1000, nullable = false)
    private String eventTypes; // Comma-separated list of event types

    @Column(name = "SERVICE_CODE", length = 50)
    private String serviceCode;

    @Column(name = "SECRET", length = 200)
    private String secret;

    @Column(name = "NOTES", length = 1000)
    private String notes;
}


package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Taxpayer DTO")
public record TaxpayerDto(
        @Schema(description = "Unique Identifier") String id,
        @Schema(description = "Taxpayer ID") String taxId, // Changed from taxpayerNo to match Entity
        @Schema(description = "Taxpayer Name") String name,
        @Schema(description = "Taxpayer Type Title") String type, // Matches name, but still needs flattening for Title
        @Schema(description = "Taxpayer Type ID") String typeId,
        @Schema(description = "Active Status") Boolean active,
        @Schema(description = "National ID") String nationalId,
        @Schema(description = "Country") String country, // Added to match Entity
        @Schema(description = "Province") String province, // Added to match Entity
        @Schema(description = "City") String city, // Added to match Entity
        @Schema(description = "Address") String address, // Added to match Entity
        @Schema(description = "Postal Code") String postalCode, // Added to match Entity
        @Schema(description = "Passport Number") String passportNo, // Added to match Entity
        @Schema(description = "Creation Date") OffsetDateTime createdOn,
        @Schema(description = "Update Date") OffsetDateTime updatedOn,
        @Schema(description = "Created By") String createdBy,
        @Schema(description = "Updated By") String updatedBy) {}
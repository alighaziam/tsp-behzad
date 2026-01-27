package ir.tax.trustedtaxpayers.interfaces.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.*;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Optional;

/**
 * Mapper برای تبدیل Entity های صورتحساب الکترونیکی به DTO
 * 
 * @author Ghazi-a
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ElectronicInvoiceMapper {

    // تبدیل TemplateTypeEntity به TemplateTypeDto
    public ElectronicInvoiceDtos.TemplateTypeDto toTemplateTypeDto(TemplateTypeEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.TemplateTypeDto(
            entity.getCode(),
            entity.getTitle(),
            entity.getDescription()
        );
    }

    // تبدیل TemplateEntity به TemplateDto
    public ElectronicInvoiceDtos.TemplateDto toTemplateDto(TemplateEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.TemplateDto(
            entity.getCode(),
            entity.getTitle()
        );
    }

    // تبدیل TemplateRuleEntity به TemplateRuleDto
    public ElectronicInvoiceDtos.TemplateRuleDto toTemplateRuleDto(TemplateRuleEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.TemplateRuleDto(
            entity.getId(),
            entity.getTemplateCode(),
            entity.getFieldName(),
            entity.getRuleType(),
            entity.getRuleParams(),
            entity.getExplanation(),
            entity.getActive()
        );
    }

    // تبدیل ElectronicInvoiceHeaderEntity به ElectronicInvoiceHeaderSummaryDto
    public ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto toSummaryDto(
            ElectronicInvoiceHeaderEntity entity, String sts) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto(
            entity.getId(),
            entity.getTaxId(),
            entity.getTemplateCode(),
            entity.getInty() != null ? entity.getInty().getValue() : null,
            entity.getInno(),
            entity.getIndatim(),
            entity.getTins(),
            entity.getTinb(),
            entity.getTbill(),
            sts,
            entity.getCreatedOn()
        );
    }

    public ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto toDetailDto(
            ElectronicInvoiceHeaderEntity entity,
            List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto> items,
            List<ElectronicInvoiceDtos.ElectronicInvoicePaymentDto> payments,
            ElectronicInvoiceDtos.ElectronicInvoiceSystemDto systemDto) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto(
            entity.getId(),
            entity.getTaxId(),
            entity.getTemplateCode(),
            entity.getTemplateSubtype(),
            entity.getInty() != null ? entity.getInty().getValue() : null,
            entity.getInno(),
            entity.getIndatim(),
            entity.getM2Indati(),
            entity.getInp(),
            entity.getIns() != null ? entity.getIns().getValue() : null,
            entity.getIrtaxid(),
            entity.getTins(),
            entity.getSbc(),
            entity.getTob() != null ? entity.getTob().getValue() : null,
            entity.getBid(),
            entity.getTinb(),
            entity.getBbc(),
            entity.getBpc(),
            entity.getBpn(),
            entity.getFt() != null ? entity.getFt().getValue() : null,
            entity.getScln(),
            entity.getScc(),
            entity.getCdcn(),
            entity.getCdcd(),
            entity.getCrn(),
            entity.getBillid(),
            entity.getBillingId(),
            entity.getTprdis(),
            entity.getTdis(),
            entity.getTadis(),
            entity.getTvam(),
            entity.getTodam(),
            entity.getTbill(),
            entity.getTvop(),
            entity.getTax17(),
            entity.getTonw(),
            entity.getTorv(),
            entity.getTocv(),
            entity.getSetm() != null ? entity.getSetm().getValue() : null,
            entity.getCap(),
            entity.getInsp(),
            entity.getTinc(),
            entity.getInInsuranceId(),
            entity.getAn(),
            entity.getAsn(),
            entity.getAsd(),
            entity.getCreatedOn(),
            entity.getUpdatedOn(),
            entity.getCreatedBy(),
            entity.getUpdatedBy(),
            items,
            payments,
            systemDto
        );
    }

    // تبدیل ElectronicInvoiceItemEntity به ElectronicInvoiceItemDto
    public ElectronicInvoiceDtos.ElectronicInvoiceItemDto toItemDto(ElectronicInvoiceItemEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceItemDto(
            entity.getId(),
            entity.getHeaderId(),
            entity.getLineNo(),
            entity.getSstid(),
            entity.getSstt(),
            entity.getAm(),
            entity.getMu(),
            entity.getNw(),
            entity.getFee(),
            entity.getCfee(),
            entity.getCut(),
            entity.getExr(),
            entity.getCpr(),
            entity.getSsrv(),
            entity.getSscv(),
            entity.getPrdis(),
            entity.getDis(),
            entity.getAdis(),
            entity.getVra(),
            entity.getVam(),
            entity.getVba(),
            entity.getVop(),
            entity.getOdt(),
            entity.getOdr(),
            entity.getOdam(),
            entity.getOlt(),
            entity.getOlr(),
            entity.getOlam(),
            entity.getConsfee(),
            entity.getSpro(),
            entity.getBros(),
            entity.getTcpbs(),
            entity.getBsrn(),
            entity.getCop(),
            entity.getCui(),
            entity.getTsstam(),
            entity.getMedid(),
            entity.getAspr()
        );
    }

    // تبدیل ElectronicInvoicePaymentEntity به ElectronicInvoicePaymentDto
    public ElectronicInvoiceDtos.ElectronicInvoicePaymentDto toPaymentDto(ElectronicInvoicePaymentEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoicePaymentDto(
            entity.getId(),
            entity.getHeaderId(),
            entity.getPmt(),
            entity.getPv(),
            entity.getPdt(),
            entity.getIinn(),
            entity.getAcn(),
            entity.getTrmn(),
            entity.getTrn(),
            entity.getPcn(),
            entity.getPid()
        );
    }

    // تبدیل ElectronicInvoiceReferenceEntity به ElectronicInvoiceReferenceDto
    public ElectronicInvoiceDtos.ElectronicInvoiceReferenceDto toReferenceDto(ElectronicInvoiceReferenceEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceReferenceDto(
            entity.getId(),
            entity.getHeaderId(),
            entity.getIrtaxid(),
            entity.getInref(),
            entity.getReftyp(),
            entity.getAsno(),
            entity.getAsdt(),
            entity.getBsrn(),
            entity.getSovat(),
            entity.getBpn(),
            entity.getAn()
        );
    }

    // تبدیل ElectronicInvoiceShippingEntity به ElectronicInvoiceShippingDto
    public ElectronicInvoiceDtos.ElectronicInvoiceShippingDto toShippingDto(ElectronicInvoiceShippingEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceShippingDto(
            entity.getId(),
            entity.getHeaderId(),
            entity.getLno(),
            entity.getLrno(),
            entity.getOcu(),
            entity.getOci(),
            entity.getDco(),
            entity.getDci(),
            entity.getTid(),
            entity.getRid(),
            entity.getLt() != null ? entity.getLt().getValue() : null,
            entity.getCno(),
            entity.getDid(),
            entity.getSg(),
            entity.getSgid(),
            entity.getSgt(),
            entity.getCdcn(),
            entity.getCdcd(),
            entity.getCnid(),
            entity.getTpwd(),
            entity.getTonw(),
            entity.getTorv(),
            entity.getTocv()
        );
    }

    // تبدیل ElectronicInvoiceSystemEntity به ElectronicInvoiceSystemDto
    public ElectronicInvoiceDtos.ElectronicInvoiceSystemDto toSystemDto(ElectronicInvoiceSystemEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceSystemDto(
            entity.getId(),
            entity.getSts(),
            entity.getSrvdt(),
            entity.getReferenceNumber(),
            entity.getRejcode(),
            entity.getRejdesc(),
            entity.getCreatedOn(),
            entity.getUpdatedOn()
        );
    }

    // تبدیل ElectronicInvoiceExtensionEntity به ElectronicInvoiceExtensionDto
    public ElectronicInvoiceDtos.ElectronicInvoiceExtensionDto toExtensionDto(ElectronicInvoiceExtensionEntity entity) {
        if (entity == null) return null;
        return new ElectronicInvoiceDtos.ElectronicInvoiceExtensionDto(
            entity.getId(),
            entity.getHeaderId(),
            entity.getExtensionKey(),
            entity.getExtensionValue(),
            entity.getExtensionType() != null ? entity.getExtensionType().getValue() : null,
            entity.getCreatedOn()
        );
    }
    
    // تبدیل ElectronicInvoiceSystemEntity به ElectronicInvoiceSystemWithHeaderDto
    public ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto toSystemWithHeaderDto(ElectronicInvoiceSystemEntity entity) {
        if (entity == null) return null;

        ElectronicInvoiceHeaderEntity header = entity.getHeader();
        if (header == null) {
            return new ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto(
                entity.getId(),
                entity.getSts(),
                entity.getSrvdt(),
                entity.getRejcode(),
                entity.getRejdesc(),
                null, null, null, null, null, null, null, null, null
            );
        }
        
        return new ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto(
            entity.getId(),
            entity.getSts(),
            entity.getSrvdt(),
            entity.getRejcode(),
            entity.getRejdesc(),
            header.getTaxId(),
            header.getTemplateCode(),
            header.getInty() != null ? header.getInty().getValue() : null,
            header.getInno(),
            header.getIndatim(),
            header.getTins(),
            header.getTinb(),
            header.getTbill(),
            entity.getCreatedOn()
        );
    }
}



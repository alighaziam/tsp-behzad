package ir.tax.trustedtaxpayers.application.util;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for converting Invoice entities to PacketDto
 * کلاس کمکی برای تبدیل Invoice Entity به PacketDto
 * 
 * این کلاس مسئولیت تبدیل Invoice به فرمت مناسب برای ارسال به سامانه مودیان را دارد.
 */
@Component
public class PacketConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(PacketConverter.class);
    
    /**
     * تبدیل Invoice Header به PacketDto
     */
    public PacketDto convertInvoiceToPacket(ElectronicInvoiceHeaderEntity invoice, String packetType) {
        return convertInvoiceToPacket(invoice, packetType, UUID.randomUUID().toString());
    }

    public PacketDto convertInvoiceToPacket(ElectronicInvoiceHeaderEntity invoice, String packetType, String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("Packet UID must not be blank");
        }
        // استفاده از Tins به عنوان FiscalId در صورت عدم وجود FiscalId
        String fiscalId = invoice.getTins();
        
        PacketDto packet = new PacketDto();
        packet.setUid(uid);
        packet.setFiscalId(fiscalId);
        packet.setPacketType(packetType != null ? packetType : "INVOICE");
        packet.setData(convertInvoiceToMap(invoice));
        packet.setRetry(false);
        
        logger.debug("Converted invoice {} to packet with UID: {}", invoice.getTaxId(), uid);
        return packet;
    }
    
    /**
     * تبدیل Invoice به Map برای استفاده در PacketDto
     */
    public Map<String, Object> convertInvoiceToMap(ElectronicInvoiceHeaderEntity invoice) {
        Map<String, Object> invoiceData = new HashMap<>();
        
        // اطلاعات اصلی صورتحساب
        if (invoice.getTaxId() != null) invoiceData.put("taxId", invoice.getTaxId());
        if (invoice.getTemplateCode() != null) invoiceData.put("templateCode", invoice.getTemplateCode());
        if (invoice.getTemplateSubtype() != null) invoiceData.put("templateSubtype", invoice.getTemplateSubtype());
        if (invoice.getInty() != null) invoiceData.put("inty", invoice.getInty().getValue());
        if (invoice.getInno() != null) invoiceData.put("inno", invoice.getInno());
        if (invoice.getIndatim() != null) invoiceData.put("indatim", invoice.getIndatim());
        if (invoice.getInp() != null) invoiceData.put("inp", invoice.getInp());
        if (invoice.getIns() != null) invoiceData.put("ins", invoice.getIns().getValue());
        if (invoice.getIrtaxid() != null) invoiceData.put("irtaxid", invoice.getIrtaxid());
        
        // اطلاعات فروشنده
        if (invoice.getTins() != null) invoiceData.put("tins", invoice.getTins());
        if (invoice.getSbc() != null) invoiceData.put("sbc", invoice.getSbc());
        
        // اطلاعات خریدار
        if (invoice.getTob() != null) invoiceData.put("tob", invoice.getTob().getValue());
        if (invoice.getBid() != null) invoiceData.put("bid", invoice.getBid());
        if (invoice.getTinb() != null) invoiceData.put("tinb", invoice.getTinb());
        if (invoice.getBbc() != null) invoiceData.put("bbc", invoice.getBbc());
        if (invoice.getBpc() != null) invoiceData.put("bpc", invoice.getBpc());
        if (invoice.getBpn() != null) invoiceData.put("bpn", invoice.getBpn());
        
        // اطلاعات اختصاصی الگوها
        if (invoice.getFt() != null) invoiceData.put("ft", invoice.getFt().getValue());
        if (invoice.getScln() != null) invoiceData.put("scln", invoice.getScln());
        if (invoice.getScc() != null) invoiceData.put("scc", invoice.getScc());
        if (invoice.getCdcn() != null) invoiceData.put("cdcn", invoice.getCdcn());
        if (invoice.getCdcd() != null) invoiceData.put("cdcd", invoice.getCdcd());
        if (invoice.getCrn() != null) invoiceData.put("crn", invoice.getCrn());
        if (invoice.getBillid() != null) invoiceData.put("billid", invoice.getBillid());
        if (invoice.getBillingId() != null) invoiceData.put("billingId", invoice.getBillingId());
        if (invoice.getTonw() != null) invoiceData.put("tonw", invoice.getTonw());
        if (invoice.getTorv() != null) invoiceData.put("torv", invoice.getTorv());
        if (invoice.getTocv() != null) invoiceData.put("tocv", invoice.getTocv());
        if (invoice.getTinc() != null) invoiceData.put("tinc", invoice.getTinc());
        
        // اطلاعات مالی
        if (invoice.getTprdis() != null) invoiceData.put("tprdis", invoice.getTprdis());
        if (invoice.getTdis() != null) invoiceData.put("tdis", invoice.getTdis());
        if (invoice.getTadis() != null) invoiceData.put("tadis", invoice.getTadis());
        if (invoice.getTvam() != null) invoiceData.put("tvam", invoice.getTvam());
        if (invoice.getTodam() != null) invoiceData.put("todam", invoice.getTodam());
        if (invoice.getTbill() != null) invoiceData.put("tbill", invoice.getTbill());
        
        return invoiceData;
    }
    
    /**
     * تبدیل Invoice به JSON String
     */
    public String convertInvoiceToJson(ElectronicInvoiceHeaderEntity invoice) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(convertInvoiceToMap(invoice));
        } catch (Exception e) {
            logger.error("Failed to convert invoice to JSON", e);
            throw new RuntimeException("Failed to convert invoice to JSON", e);
        }
    }
}


package ir.tax.trustedtaxpayers.externalService.integration;

import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter for connecting TaxpayerSystemClient to InvoiceTransmissionService
 * آداپتور برای اتصال کلاینت سامانه مودیان به سرویس انتقال صورتحساب
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Component
public class TaxpayerSystemSubmissionAdapter implements InvoiceTransmissionService.InvoiceSubmissionDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(TaxpayerSystemSubmissionAdapter.class);
    
    private final TaxpayerSystemClient taxpayerSystemClient;
    
    public TaxpayerSystemSubmissionAdapter(TaxpayerSystemClient taxpayerSystemClient) {
        this.taxpayerSystemClient = taxpayerSystemClient;
    }
    
    @Override
    public InvoiceTransmissionService.TransmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity invoice) {
        logger.info("Submitting invoice {} via taxpayer system client", invoice.getTaxId());
        
        TaxpayerSystemClient.InvoiceSubmissionResponse response = taxpayerSystemClient.submitInvoice(invoice);
        
        if (response.isSuccessful()) {
            return InvoiceTransmissionService.TransmissionResponse.success(
                response.getStatus(),
                response.getMessage()
            );
        } else {
            return InvoiceTransmissionService.TransmissionResponse.failure(
                response.getRejectionCode(),
                response.getRejectionDescription()
            );
        }
    }
}


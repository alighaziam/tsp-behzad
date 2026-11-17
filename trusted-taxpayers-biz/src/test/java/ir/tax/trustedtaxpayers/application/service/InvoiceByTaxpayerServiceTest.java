package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceByTaxpayerServiceTest {

    private ElectronicInvoiceHeaderRepository invoiceRepository;
    private TaxpayerRepository taxpayerRepository;
    private InvoiceByTaxpayerService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(ElectronicInvoiceHeaderRepository.class);
        taxpayerRepository = mock(TaxpayerRepository.class);
        service = new InvoiceByTaxpayerService(invoiceRepository, taxpayerRepository, mock(CacheService.class));
    }

    @Test
    void loadsInvoicesByTaxpayerFiscalIdForSellerOrBuyer() {
        UUID taxpayerId = UUID.randomUUID();
        TaxpayerEntity taxpayer = activeTaxpayer(taxpayerId, "1234567890");
        when(taxpayerRepository.findById(taxpayerId)).thenReturn(Optional.of(taxpayer));
        when(invoiceRepository.findByTinsOrTinb(eq("1234567890"), eq("1234567890"), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<?> result = service.loadInvoicesByTaxpayer(taxpayerId, 0, 20, "createdOn", "DESC");

        assertEquals(0, result.getTotalElements());
        verify(invoiceRepository).findByTinsOrTinb(eq("1234567890"), eq("1234567890"), any(Pageable.class));
    }

    @Test
    void doesNotQueryInvoicesForInactiveTaxpayer() {
        UUID taxpayerId = UUID.randomUUID();
        TaxpayerEntity taxpayer = activeTaxpayer(taxpayerId, "1234567890");
        taxpayer.setIsActive(false);
        when(taxpayerRepository.findById(taxpayerId)).thenReturn(Optional.of(taxpayer));

        Page<?> result = service.loadInvoicesByTaxpayer(taxpayerId, 0, 20, null, null);

        assertEquals(0, result.getTotalElements());
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void returnsRealCountForTaxpayer() {
        UUID taxpayerId = UUID.randomUUID();
        TaxpayerEntity taxpayer = activeTaxpayer(taxpayerId, "1234567890");
        when(taxpayerRepository.findById(taxpayerId)).thenReturn(Optional.of(taxpayer));
        when(invoiceRepository.countByTinsOrTinb("1234567890", "1234567890")).thenReturn(17L);

        assertEquals(17L, service.countInvoicesByTaxpayer(taxpayerId));
    }

    private TaxpayerEntity activeTaxpayer(UUID id, String taxId) {
        TaxpayerEntity taxpayer = new TaxpayerEntity();
        taxpayer.setId(id);
        taxpayer.setTaxId(taxId);
        taxpayer.setIsActive(true);
        return taxpayer;
    }
}

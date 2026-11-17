package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserTaxpayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserTaxpayerResolverServiceTest {

    private UserTaxpayerRepository associationRepository;
    private TaxpayerRepository taxpayerRepository;
    private UserRoleService userRoleService;
    private CurrentUserTaxpayerResolverService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        associationRepository = mock(UserTaxpayerRepository.class);
        taxpayerRepository = mock(TaxpayerRepository.class);
        userRoleService = mock(UserRoleService.class);
        service = new CurrentUserTaxpayerResolverService(
                associationRepository, taxpayerRepository, userRoleService);
        userId = UUID.randomUUID();

        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(userId);
        when(user.getIsActive()).thenReturn(true);
        when(user.getStatus()).thenReturn("ACTIVE");
        when(userRoleService.findUserByUsername("test-user")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("test-user", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAssignedActiveTaxpayersOnly() {
        UUID activeId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        UserTaxpayerEntity activeAssociation = association(activeId);
        UserTaxpayerEntity inactiveAssociation = association(inactiveId);
        when(associationRepository.findAllByUserId(userId))
                .thenReturn(List.of(activeAssociation, inactiveAssociation));

        TaxpayerEntity activeTaxpayer = taxpayer(activeId, true);
        TaxpayerEntity inactiveTaxpayer = taxpayer(inactiveId, false);
        when(taxpayerRepository.findAllById(Set.of(activeId, inactiveId)))
                .thenReturn(List.of(activeTaxpayer, inactiveTaxpayer));

        assertEquals(Set.of(activeId), service.findCurrentUserTaxpayerIds());
    }

    @Test
    void doesNotGuessPrimaryWhenUserHasMultipleTaxpayers() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(associationRepository.findAllByUserId(userId))
                .thenReturn(List.of(association(firstId), association(secondId)));
        when(taxpayerRepository.findAllById(Set.of(firstId, secondId)))
                .thenReturn(List.of(taxpayer(firstId, true), taxpayer(secondId, true)));

        assertTrue(service.findCurrentUserPrimaryTaxpayerId().isEmpty());
    }

    @Test
    void returnsPrimaryWhenExactlyOneActiveTaxpayerExists() {
        UUID taxpayerId = UUID.randomUUID();
        when(associationRepository.findAllByUserId(userId)).thenReturn(List.of(association(taxpayerId)));
        when(taxpayerRepository.findAllById(Set.of(taxpayerId)))
                .thenReturn(List.of(taxpayer(taxpayerId, true)));

        assertEquals(Optional.of(taxpayerId), service.findCurrentUserPrimaryTaxpayerId());
    }

    private UserTaxpayerEntity association(UUID taxpayerId) {
        UserTaxpayerEntity association = mock(UserTaxpayerEntity.class);
        when(association.getTaxpayerId()).thenReturn(taxpayerId);
        return association;
    }

    private TaxpayerEntity taxpayer(UUID id, boolean active) {
        TaxpayerEntity taxpayer = mock(TaxpayerEntity.class);
        when(taxpayer.getId()).thenReturn(id);
        when(taxpayer.getIsActive()).thenReturn(active);
        return taxpayer;
    }
}

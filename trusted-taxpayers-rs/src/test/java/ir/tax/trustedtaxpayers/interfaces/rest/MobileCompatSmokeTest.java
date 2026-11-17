package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.gov.tax.tpis.sdk.content.api.TaxApi;
import ir.tax.trustedtaxpayers.application.security.JwtService;
import ir.tax.trustedtaxpayers.config.TaxSdkProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileCompatSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaxApi taxApi;

    @MockBean
    private TaxSdkProperties taxSdkProperties;

    private String authorization(String... roles) {
        String token = jwtService.generate("mobile-test-user", 60_000L,
                Map.of("roles", List.of(roles)));
        return "Bearer " + token;
    }

    private String authorization() {
        return authorization("ADMIN", "USER");
    }

    @Test
    @DisplayName("Taxpayers endpoint rejects unauthenticated requests")
    void taxpayersUnauthorized() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/taxpayers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Taxpayers endpoint accepts a valid JWT")
    void taxpayersListOk() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/taxpayers")
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Settings read requires administrator authority")
    void settingsReadRequiresAdmin() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, authorization("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Settings write requires administrator authority")
    void settingsWriteRequiresAdmin() throws Exception {
        mockMvc.perform(post("/mobile-compat/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, authorization("USER"))
                        .contentType("application/json")
                        .content("{\"key\":\"feature.flag\",\"value\":\"true\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Taxpayer list requires administrator authority")
    void taxpayersRequireAdmin() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/taxpayers")
                        .header(HttpHeaders.AUTHORIZATION, authorization("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Invoices list accepts a valid JWT")
    void invoicesListOk() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/invoices")
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Roles list accepts a valid JWT")
    void rolesListOk() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/user-roles/roles")
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Templates list accepts a valid JWT")
    void templatesListOk() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/templates")
                        .header(HttpHeaders.AUTHORIZATION, authorization()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Outbox list rejects unauthenticated requests")
    void outboxUnauthorized() throws Exception {
        mockMvc.perform(get("/mobile-compat/api/outbox/messages"))
                .andExpect(status().isUnauthorized());
    }
}

package ir.tax.trustedtaxpayers.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MobileCompatSmokeTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String TOKEN = "Bearer demo-token";

	@Test
	@DisplayName("Goods list requires auth")
	void goodsListUnauthorized() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/goods"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Goods list OK with demo token")
	void goodsListOk() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/goods").header(HttpHeaders.AUTHORIZATION, TOKEN))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Invoices list OK with demo token")
	void invoicesListOk() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/invoices").header(HttpHeaders.AUTHORIZATION, TOKEN))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Roles list OK with demo token")
	void rolesListOk() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/user-roles/roles").header(HttpHeaders.AUTHORIZATION, TOKEN))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Templates list OK with demo token")
	void templatesListOk() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/templates").header(HttpHeaders.AUTHORIZATION, TOKEN))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Outbox list unauthorized without token")
	void outboxUnauthorized() throws Exception {
		mockMvc.perform(get("/mobile-compat/api/outbox/messages"))
				.andExpect(status().isUnauthorized());
	}
}



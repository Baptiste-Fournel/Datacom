package com.datacom.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datacom.domain.product.Product;
import com.datacom.domain.product.ProductRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ProductListingIT {

    private static final Instant DATE = Instant.parse("2026-07-27T10:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldListNewestFirstWithMappedFields_whenOperatorRequests() throws Exception {
        // Given
        Product older = Product.createDraft(1L, DATE);
        older.updateIdentification("Older sensor", "REF-OLD", "first", DATE);
        productRepository.save(older);
        Product newer = Product.createDraft(1L, DATE);
        newer.updateIdentification("Newer sensor", "REF-NEW", "second", DATE);
        productRepository.save(newer);

        // When
        mockMvc.perform(get("/api/products"))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(2)),
                        jsonPath("$[0].reference").value("REF-NEW"),
                        jsonPath("$[0].name").value("Newer sensor"),
                        jsonPath("$[0].status").value("DRAFT"),
                        jsonPath("$[0].currentStep").value(1),
                        jsonPath("$[1].reference").value("REF-OLD"));
    }

    @Test
    @WithMockUser(username = "operator", roles = "OPERATOR")
    void shouldReturnEmptyList_whenNoProductExists() throws Exception {
        // When
        mockMvc.perform(get("/api/products"))
                // Then
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$", hasSize(0)));
    }
}

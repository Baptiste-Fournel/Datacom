package com.datacom.testsupport;

import com.datacom.domain.product.Product;
import com.datacom.domain.user.Role;
import com.datacom.domain.user.User;
import java.time.Instant;

public final class ProductFixtures {

    public static final Instant DATE = Instant.parse("2026-07-27T10:00:00Z");
    public static final User SEEDED_VALIDATOR = new User(2L, "validator", "Jane", "Doe", Role.VALIDATOR);

    private ProductFixtures() {
    }

    public static Product draft() {
        return Product.createDraft(1L, DATE);
    }

    public static Product draftAtFinalStep() {
        Product product = draft();
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        product.advanceToNextStep(DATE);
        return product;
    }

    public static Product submitted() {
        Product product = draftAtFinalStep();
        product.submitForValidation(DATE);
        return product;
    }

    public static Product validated() {
        Product product = submitted();
        product.validate(SEEDED_VALIDATOR, DATE);
        return product;
    }
}

package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductEditionTest {

    private static final Instant CREATION_DATE = Instant.parse("2026-07-27T10:00:00Z");
    private static final Instant EDITION_DATE = Instant.parse("2026-07-27T11:30:00Z");

    @Test
    void lIdentificationSeRenseigneEtSeRelit() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        product.updateIdentification("Capteur thermique T-200", "REF-T200-FR", "Capteur agroalimentaire",
                EDITION_DATE);

        assertThat(product.name()).isEqualTo("Capteur thermique T-200");
        assertThat(product.reference()).isEqualTo("REF-T200-FR");
        assertThat(product.description()).isEqualTo("Capteur agroalimentaire");
        assertThat(product.updatedAt()).isEqualTo(EDITION_DATE);
    }

    @Test
    void laClassificationSeRenseigneEtSeRelit() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        product.updateClassification("Instrumentation", "Capteurs", "ThermoWorks SA", "France", EDITION_DATE);

        assertThat(product.category()).isEqualTo("Instrumentation");
        assertThat(product.subcategory()).isEqualTo("Capteurs");
        assertThat(product.manufacturer()).isEqualTo("ThermoWorks SA");
        assertThat(product.country()).isEqualTo("France");
        assertThat(product.updatedAt()).isEqualTo(EDITION_DATE);
    }

    @Test
    void laCertificationSeRenseigneEtSeRelit() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        product.updateCertification("LOT-2026-0417", "CE / RoHS", "Certificats fournisseur joints", EDITION_DATE);

        assertThat(product.lot()).isEqualTo("LOT-2026-0417");
        assertThat(product.certification()).isEqualTo("CE / RoHS");
        assertThat(product.validationComment()).isEqualTo("Certificats fournisseur joints");
        assertThat(product.updatedAt()).isEqualTo(EDITION_DATE);
    }

    @Test
    void lAvancementSuitLaMachineAEtats() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        product.advanceToNextStep(EDITION_DATE);

        assertThat(product.currentStep()).isEqualTo(WorkflowStep.CLASSIFICATION);
        assertThat(product.updatedAt()).isEqualTo(EDITION_DATE);
    }

    @Test
    void lAvancementEstBorneALEtapeFinale() {
        Product product = Product.createDraft(42L, CREATION_DATE);
        product.advanceToNextStep(EDITION_DATE);
        product.advanceToNextStep(EDITION_DATE);
        product.advanceToNextStep(EDITION_DATE);

        Instant failedAttempt = Instant.parse("2026-07-27T12:00:00Z");
        assertThatIllegalStateException()
                .isThrownBy(() -> product.advanceToNextStep(failedAttempt))
                .withMessageContaining("final step");
        assertThat(product.currentStep()).isEqualTo(WorkflowStep.SUMMARY);
        assertThat(product.updatedAt()).isEqualTo(EDITION_DATE);
    }

    @Test
    void unBrouillonEstModifiable() {
        Product product = Product.createDraft(42L, CREATION_DATE);

        assertThat(product.isEditable()).isTrue();
    }
}

package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class WorkflowStepTest {

    @Test
    void shouldFollowDeclaredOrder_whenAdvancingThroughSteps() {
        // Assert
        assertThat(WorkflowStep.IDENTIFICATION.next()).isEqualTo(WorkflowStep.CLASSIFICATION);
        assertThat(WorkflowStep.CLASSIFICATION.next()).isEqualTo(WorkflowStep.CERTIFICATION);
        assertThat(WorkflowStep.CERTIFICATION.next()).isEqualTo(WorkflowStep.SUMMARY);
    }

    @Test
    void shouldRejectProgression_whenAtFinalStep() {
        // Assert
        assertThatExceptionOfType(IllegalTransitionException.class)
                .isThrownBy(WorkflowStep.SUMMARY::next)
                .withMessageContaining("final step");
    }

    @Test
    void shouldMarkOnlyLastStepAsFinal_whenInspectingSteps() {
        // Assert
        assertThat(WorkflowStep.SUMMARY.isFinal()).isTrue();
        assertThat(WorkflowStep.IDENTIFICATION.isFinal()).isFalse();
        assertThat(WorkflowStep.CLASSIFICATION.isFinal()).isFalse();
        assertThat(WorkflowStep.CERTIFICATION.isFinal()).isFalse();
    }

    @Test
    void shouldResolveStepFromNumber_whenNumberIsKnown() {
        // Assert
        assertThat(WorkflowStep.fromNumber(1)).isEqualTo(WorkflowStep.IDENTIFICATION);
        assertThat(WorkflowStep.fromNumber(2)).isEqualTo(WorkflowStep.CLASSIFICATION);
        assertThat(WorkflowStep.fromNumber(3)).isEqualTo(WorkflowStep.CERTIFICATION);
        assertThat(WorkflowStep.fromNumber(4)).isEqualTo(WorkflowStep.SUMMARY);
    }

    @Test
    void shouldRejectResolution_whenNumberIsUnknown() {
        // Assert
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WorkflowStep.fromNumber(0))
                .withMessageContaining("No workflow step");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WorkflowStep.fromNumber(5))
                .withMessageContaining("No workflow step");
    }

    @Test
    void shouldExposeNumbersOneToFour_whenReadingSteps() {
        // Assert
        assertThat(WorkflowStep.IDENTIFICATION.number()).isEqualTo(1);
        assertThat(WorkflowStep.CLASSIFICATION.number()).isEqualTo(2);
        assertThat(WorkflowStep.CERTIFICATION.number()).isEqualTo(3);
        assertThat(WorkflowStep.SUMMARY.number()).isEqualTo(4);
    }
}

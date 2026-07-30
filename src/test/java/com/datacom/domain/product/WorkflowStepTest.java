package com.datacom.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class WorkflowStepTest {

    @Test
    void shouldFollowDeclaredOrder_whenAdvancingThroughSteps() {
        assertThat(WorkflowStep.IDENTIFICATION.next()).isEqualTo(WorkflowStep.CLASSIFICATION);
        assertThat(WorkflowStep.CLASSIFICATION.next()).isEqualTo(WorkflowStep.CERTIFICATION);
        assertThat(WorkflowStep.CERTIFICATION.next()).isEqualTo(WorkflowStep.SUMMARY);
    }

    @Test
    void shouldRejectProgression_whenAtFinalStep() {
        assertThatIllegalStateException()
                .isThrownBy(WorkflowStep.SUMMARY::next)
                .withMessageContaining("final step");
    }

    @Test
    void shouldMarkOnlyLastStepAsFinal_whenInspectingSteps() {
        assertThat(WorkflowStep.SUMMARY.isFinal()).isTrue();
        assertThat(WorkflowStep.IDENTIFICATION.isFinal()).isFalse();
        assertThat(WorkflowStep.CLASSIFICATION.isFinal()).isFalse();
        assertThat(WorkflowStep.CERTIFICATION.isFinal()).isFalse();
    }

    @Test
    void shouldExposeNumbersOneToFour_whenReadingSteps() {
        assertThat(WorkflowStep.IDENTIFICATION.number()).isEqualTo(1);
        assertThat(WorkflowStep.CLASSIFICATION.number()).isEqualTo(2);
        assertThat(WorkflowStep.CERTIFICATION.number()).isEqualTo(3);
        assertThat(WorkflowStep.SUMMARY.number()).isEqualTo(4);
    }
}

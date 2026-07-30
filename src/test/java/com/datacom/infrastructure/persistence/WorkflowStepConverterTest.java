package com.datacom.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.datacom.domain.product.WorkflowStep;
import org.junit.jupiter.api.Test;

class WorkflowStepConverterTest {

    private final WorkflowStepConverter converter = new WorkflowStepConverter();

    @Test
    void shouldRoundTripEveryStep_whenConvertingBothWays() {
        for (WorkflowStep step : WorkflowStep.values()) {
            // Act
            Integer column = converter.convertToDatabaseColumn(step);

            // Assert
            assertThat(column).isEqualTo(step.number());
            assertThat(converter.convertToEntityAttribute(column)).isEqualTo(step);
        }
    }

    @Test
    void shouldReturnNull_whenSourceIsNull() {
        // Assert
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

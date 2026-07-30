package com.datacom.infrastructure.persistence;

import com.datacom.domain.product.WorkflowStep;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkflowStepConverter implements AttributeConverter<WorkflowStep, Integer> {

    @Override
    public Integer convertToDatabaseColumn(WorkflowStep step) {
        return step == null ? null : step.number();
    }

    @Override
    public WorkflowStep convertToEntityAttribute(Integer number) {
        return number == null ? null : WorkflowStep.fromNumber(number);
    }
}

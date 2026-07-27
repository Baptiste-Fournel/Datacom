package com.datacom.domain.product;

public class IncompleteProductException extends RuntimeException {

    public IncompleteProductException(WorkflowStep currentStep) {
        super("The product cannot be submitted before reaching the final step, current step is " + currentStep);
    }
}

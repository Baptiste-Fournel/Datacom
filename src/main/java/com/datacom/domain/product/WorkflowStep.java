package com.datacom.domain.product;

public enum WorkflowStep {
    IDENTIFICATION(1),
    CLASSIFICATION(2),
    CERTIFICATION(3),
    SUMMARY(4);

    private final int number;

    WorkflowStep(int number) {
        this.number = number;
    }

    public static WorkflowStep fromNumber(int number) {
        for (WorkflowStep step : values()) {
            if (step.number == number) {
                return step;
            }
        }
        throw new IllegalArgumentException("No workflow step matches number " + number);
    }

    public WorkflowStep next() {
        if (isFinal()) {
            throw new IllegalStateException("The final step does not allow any progression");
        }
        return values()[ordinal() + 1];
    }

    public boolean isFinal() {
        return this == SUMMARY;
    }

    public int number() {
        return number;
    }
}

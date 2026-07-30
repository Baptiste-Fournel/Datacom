package com.datacom.web.dto;

import jakarta.validation.constraints.Size;

public record IdentificationUpdate(
        @Size(max = 255) String name,
        @Size(max = 100) String reference,
        String description) {
}

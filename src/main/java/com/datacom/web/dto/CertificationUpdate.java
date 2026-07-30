package com.datacom.web.dto;

import jakarta.validation.constraints.Size;

public record CertificationUpdate(
        @Size(max = 100) String lot,
        @Size(max = 255) String certification,
        String validationComment) {
}

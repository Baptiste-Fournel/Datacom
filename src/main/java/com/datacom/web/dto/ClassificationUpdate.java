package com.datacom.web.dto;

import jakarta.validation.constraints.Size;

public record ClassificationUpdate(
        @Size(max = 100) String category,
        @Size(max = 100) String subcategory,
        @Size(max = 255) String manufacturer,
        @Size(max = 100) String country) {
}

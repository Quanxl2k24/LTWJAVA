package org.example.feature.sample.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleResponse {
    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
}


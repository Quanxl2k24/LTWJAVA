package org.example.feature.sample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleItem {
    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
}


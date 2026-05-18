package com.sebastian.roadmaptracker.roadmap;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProgressResponse {
    private String id;
    private Object state;
    private OffsetDateTime updatedAt;
}

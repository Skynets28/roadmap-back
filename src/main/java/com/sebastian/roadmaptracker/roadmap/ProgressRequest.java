package com.sebastian.roadmaptracker.roadmap;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressRequest {

    @NotNull(message = "state is required")
    private Object state;
}

package com.sebastian.roadmaptracker.roadmap;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapProgressService {

    static final String MAIN_ROADMAP_ID = "main-roadmap";

    private final RoadmapProgressRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProgressResponse getProgress() {
        RoadmapProgress progress = repository.findById(MAIN_ROADMAP_ID)
            .orElseThrow(() -> new IllegalStateException("Progress not initialized"));
        return toResponse(progress);
    }

    @Transactional
    public ProgressResponse saveProgress(ProgressRequest request) {
        String stateJson = serialize(request.getState());

        RoadmapProgress progress = repository.findById(MAIN_ROADMAP_ID)
            .map(existing -> {
                existing.setState(stateJson);
                existing.setUpdatedAt(OffsetDateTime.now());
                return existing;
            })
            .orElseGet(() -> RoadmapProgress.builder()
                .id(MAIN_ROADMAP_ID)
                .state(stateJson)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        repository.save(progress);
        log.info("Progress saved");
        return toResponse(progress);
    }

    private String serialize(Object state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state JSON");
        }
    }

    private ProgressResponse toResponse(RoadmapProgress progress) {
        try {
            Object stateObj = objectMapper.readValue(progress.getState(), Object.class);
            return new ProgressResponse(progress.getId(), stateObj, progress.getUpdatedAt());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize state");
        }
    }
}

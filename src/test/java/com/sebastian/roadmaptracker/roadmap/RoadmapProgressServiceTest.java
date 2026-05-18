package com.sebastian.roadmaptracker.roadmap;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapProgressServiceTest {

    @Mock
    private RoadmapProgressRepository repository;

    private RoadmapProgressService service;

    @BeforeEach
    void setUp() {
        service = new RoadmapProgressService(repository, new ObjectMapper());
    }

    @Test
    void getProgress_returnsExistingProgress() {
        RoadmapProgress stored = RoadmapProgress.builder()
            .id(RoadmapProgressService.MAIN_ROADMAP_ID)
            .state("{\"selectedWeek\":1}")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(repository.findById(RoadmapProgressService.MAIN_ROADMAP_ID)).thenReturn(Optional.of(stored));

        ProgressResponse response = service.getProgress();

        assertThat(response.getId()).isEqualTo(RoadmapProgressService.MAIN_ROADMAP_ID);
        assertThat(response.getState()).isNotNull();
    }

    @Test
    void saveProgress_persistsState() {
        Map<String, Object> stateMap = Map.of("selectedWeek", 2, "selectedPhase", 1);
        ProgressRequest request = new ProgressRequest(stateMap);

        RoadmapProgress stored = RoadmapProgress.builder()
            .id(RoadmapProgressService.MAIN_ROADMAP_ID)
            .state("{\"selectedWeek\":1}")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        when(repository.findById(RoadmapProgressService.MAIN_ROADMAP_ID)).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgressResponse response = service.saveProgress(request);

        assertThat(response.getId()).isEqualTo(RoadmapProgressService.MAIN_ROADMAP_ID);
        verify(repository).save(any(RoadmapProgress.class));
    }
}

package com.sebastian.roadmaptracker.roadmap;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapProgressController {

    private final RoadmapProgressService progressService;

    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgress() {
        return ResponseEntity.ok(progressService.getProgress());
    }

    @PutMapping("/progress")
    public ResponseEntity<ProgressResponse> saveProgress(@Valid @RequestBody ProgressRequest request) {
        return ResponseEntity.ok(progressService.saveProgress(request));
    }
}

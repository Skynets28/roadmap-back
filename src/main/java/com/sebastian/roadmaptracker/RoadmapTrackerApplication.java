package com.sebastian.roadmaptracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.sebastian.roadmaptracker.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class RoadmapTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoadmapTrackerApplication.class, args);
    }
}

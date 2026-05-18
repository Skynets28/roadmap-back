package com.sebastian.roadmaptracker.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedUserResponse {
    private String email;
    private boolean authenticated;
}

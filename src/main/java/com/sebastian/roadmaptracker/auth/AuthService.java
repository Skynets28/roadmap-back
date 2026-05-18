package com.sebastian.roadmaptracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sebastian.roadmaptracker.config.JwtProperties;
import com.sebastian.roadmaptracker.user.AppUser;
import com.sebastian.roadmaptracker.user.AppUserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
            .filter(AppUser::isEnabled)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());
        log.info("User {} logged in", user.getEmail());
        return new LoginResponse(token, "Bearer", jwtProperties.getExpirationSeconds(), user.getEmail());
    }
}

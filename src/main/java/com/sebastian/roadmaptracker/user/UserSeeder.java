package com.sebastian.roadmaptracker.user;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;

    @Value("${app.users.user1.email:}") private String user1Email;
    @Value("${app.users.user1.password-hash:}") private String user1Hash;
    @Value("${app.users.user2.email:}") private String user2Email;
    @Value("${app.users.user2.password-hash:}") private String user2Hash;
    @Value("${app.users.user3.email:}") private String user3Email;
    @Value("${app.users.user3.password-hash:}") private String user3Hash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser(user1Email, user1Hash);
        seedUser(user2Email, user2Hash);
        seedUser(user3Email, user3Hash);
    }

    private void seedUser(String email, String hash) {
        if (email == null || email.isBlank() || hash == null || hash.isBlank()) {
            return;
        }
        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                existing.setPasswordHash(hash);
                existing.setUpdatedAt(OffsetDateTime.now());
                userRepository.save(existing);
                log.info("Updated user: {}", email);
            },
            () -> {
                userRepository.save(AppUser.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .passwordHash(hash)
                    .enabled(true)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build());
                log.info("Created user: {}", email);
            }
        );
    }
}

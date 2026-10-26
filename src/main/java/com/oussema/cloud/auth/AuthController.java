package com.oussema.cloud.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public record AuthRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record AuthResponse(String token, String username, String role) {}

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        User u = users.save(User.builder()
                .username(req.username())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.USER)
                .build());
        return toResponse(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        User u = users.findByUsername(req.username())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return toResponse(u);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole().name()),
                u.getUsername(), u.getRole().name());
    }
}
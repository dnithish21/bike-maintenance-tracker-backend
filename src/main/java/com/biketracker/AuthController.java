
package com.biketracker;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String chassisNumber = request.get("chassisNumber");
        String password = request.get("password");

        if (chassisNumber == null || password == null
                || chassisNumber.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Chassis number and password are required")
            );
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    chassisNumber.trim(), password
                )
            );

            SecurityContext context =
                SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                context, httpRequest, httpResponse
            );

            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "chassisNumber", authentication.getName(),
                "role", authentication.getAuthorities()
                    .iterator().next().getAuthority()
            ));

        } catch (org.springframework.security.core.AuthenticationException ex) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Invalid credentials")
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("authenticated", false)
            );
        }

        String role = authentication.getAuthorities()
            .stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority.equals("ROLE_ADMIN")
                || authority.equals("ROLE_USER"))
            .findFirst()
            .orElse(null);

        if (role == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("message", "Unsupported user role")
            );
        }

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "chassisNumber", authentication.getName(),
            "role", role
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        new SecurityContextLogoutHandler()
            .logout(request, response, authentication);

        return ResponseEntity.ok(
            Map.of("message", "Logout successful")
        );
    }
}

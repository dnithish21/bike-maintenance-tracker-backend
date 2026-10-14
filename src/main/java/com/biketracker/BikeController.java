
package com.biketracker;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/bikes")
public class BikeController {

    private final BikeRepository bikeRepository;
    private final UserRepository userRepository;

    public BikeController(
            BikeRepository bikeRepository,
            UserRepository userRepository) {
        this.bikeRepository = bikeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Bike> getAllBikes(Authentication authentication) {
        if (isAdmin(authentication)) {
            return bikeRepository.findAll();
        }

        User user = findCurrentUser(authentication);
        return bikeRepository.findAll().stream()
                .filter(bike -> bike.getUserId() != null
                        && bike.getUserId().equals(user.getId()))
                .toList();
    }

    @PostMapping
    public Bike createBike(
            @RequestBody Bike bike,
            Authentication authentication) {

        if (isAdmin(authentication)) {
            return bikeRepository.save(bike);
        }

        User user = findCurrentUser(authentication);
        bike.setUserId(user.getId());

        return bikeRepository.save(bike);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private User findCurrentUser(Authentication authentication) {
        User user = userRepository.findByChassisNumber(
                authentication.getName());

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "User account not found");
        }

        return user;
    }
}

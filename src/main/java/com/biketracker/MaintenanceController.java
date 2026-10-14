
package com.biketracker;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final BikeRepository bikeRepository;
    private final UserRepository userRepository;

    public MaintenanceController(
            MaintenanceRepository maintenanceRepository,
            BikeRepository bikeRepository,
            UserRepository userRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.bikeRepository = bikeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Maintenance> getAllMaintenance(
            Authentication authentication) {

        if (isAdmin(authentication)) {
            return maintenanceRepository.findAll();
        }

        User user = findCurrentUser(authentication);

        Set<Long> ownedBikeIds = bikeRepository.findAll().stream()
                .filter(bike -> bike.getUserId() != null
                        && bike.getUserId().equals(user.getId()))
                .filter(bike -> bike.getId() != null)
                .map(bike -> bike.getId().longValue())
                .collect(Collectors.toSet());

        return maintenanceRepository.findAll().stream()
                .filter(record -> record.getBikeId() != null
                        && ownedBikeIds.contains(record.getBikeId()))
                .toList();
    }

    @PostMapping
    public Maintenance createMaintenance(
            @RequestBody Maintenance maintenance,
            Authentication authentication) {

        if (maintenance.getBikeId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bike ID is required");
        }

        if (isAdmin(authentication)) {
            return maintenanceRepository.save(maintenance);
        }

        User user = findCurrentUser(authentication);

        Bike bike = bikeRepository.findAll().stream()
                .filter(item -> item.getId() != null
                        && item.getId().longValue()
                                == maintenance.getBikeId().longValue())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bike not found"));

        if (bike.getUserId() == null
                || !bike.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only add maintenance to your own bike");
        }

        return maintenanceRepository.save(maintenance);
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

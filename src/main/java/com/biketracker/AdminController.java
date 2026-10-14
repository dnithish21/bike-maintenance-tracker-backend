
package com.biketracker;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;
    private final MaintenanceRepository maintenanceRepository;

    public AdminController(
            UserRepository userRepository,
            BikeRepository bikeRepository,
            MaintenanceRepository maintenanceRepository) {
        this.userRepository = userRepository;
        this.bikeRepository = bikeRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {

        BigDecimal totalCost = maintenanceRepository.findAll()
                .stream()
                .map(Maintenance::getCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalBikes", bikeRepository.count());
        stats.put("totalMaintenanceRecords", maintenanceRepository.count());
        stats.put("totalMaintenanceCost", totalCost);

        return stats;
    }
}

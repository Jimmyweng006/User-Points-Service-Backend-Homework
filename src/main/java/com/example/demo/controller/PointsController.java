package com.example.demo.controller;

import com.example.demo.model.dto.AddPointsRequest;
import com.example.demo.model.dto.LeaderboardEntry;
import com.example.demo.model.dto.UpdateReasonRequest;
import com.example.demo.model.entity.PointRecord;
import com.example.demo.model.entity.UserPoints;
import com.example.demo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @PostMapping
    public ResponseEntity<PointRecord> addPoints(@RequestBody AddPointsRequest request) {
        PointRecord newRecord = pointsService.addPoints(request);
        return ResponseEntity.ok(newRecord);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPoints> getTotalPoints(@PathVariable String userId) {
        UserPoints userPoints = pointsService.getTotalPoints(userId);
        if (userPoints != null) {
            return ResponseEntity.ok(userPoints);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        List<LeaderboardEntry> leaderboard = pointsService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PointRecord> updateReason(@PathVariable Long id, @RequestBody UpdateReasonRequest request) {
        try {
            PointRecord updatedRecord = pointsService.updateReason(id, request);
            return ResponseEntity.ok(updatedRecord);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUserPoints(@PathVariable String userId) {
        pointsService.deleteUserPoints(userId);
        return ResponseEntity.noContent().build();
    }
}

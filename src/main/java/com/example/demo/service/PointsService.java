package com.example.demo.service;

import com.example.demo.model.dto.AddPointsRequest;
import com.example.demo.model.dto.LeaderboardEntry;
import com.example.demo.model.dto.UpdateReasonRequest;
import com.example.demo.model.entity.PointRecord;
import com.example.demo.model.entity.UserPoints;

import java.util.List;

public interface PointsService {

    PointRecord addPoints(AddPointsRequest request);

    UserPoints getTotalPoints(String userId);

    List<LeaderboardEntry> getLeaderboard();

    PointRecord updateReason(Long id, UpdateReasonRequest request);

    void deleteUserPoints(String userId);
}

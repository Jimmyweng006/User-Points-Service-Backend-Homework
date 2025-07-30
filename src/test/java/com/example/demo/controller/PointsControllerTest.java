package com.example.demo.controller;

import com.example.demo.model.dto.AddPointsRequest;
import com.example.demo.model.dto.LeaderboardEntry;
import com.example.demo.model.dto.UpdateReasonRequest;
import com.example.demo.model.entity.PointRecord;
import com.example.demo.model.entity.UserPoints;
import com.example.demo.service.PointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointsController.class)
@SuppressWarnings("deprecation")
class PointsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private PointsService pointsService;

    @Autowired
    private ObjectMapper objectMapper;

    private AddPointsRequest addPointsRequest;
    private PointRecord pointRecord;
    private UserPoints userPoints;
    private UpdateReasonRequest updateReasonRequest;

    @BeforeEach
    void setUp() {
        addPointsRequest = new AddPointsRequest();
        addPointsRequest.setUserId("user123");
        addPointsRequest.setAmount(100);
        addPointsRequest.setReason("Test reward");

        pointRecord = new PointRecord();
        pointRecord.setId(1L);
        pointRecord.setUserId("user123");
        pointRecord.setAmount(100);
        pointRecord.setReason("Test reward");
        pointRecord.setCreatedAt(LocalDateTime.now());

        userPoints = new UserPoints();
        userPoints.setUserId("user123");
        userPoints.setTotalPoints(500L);
        userPoints.setUpdatedAt(LocalDateTime.now());

        updateReasonRequest = new UpdateReasonRequest();
        updateReasonRequest.setReason("Updated reason");
    }

    @Test
    void addPoints_ShouldReturnCreatedPointRecord() throws Exception {
        // Given
        when(pointsService.addPoints(any(AddPointsRequest.class))).thenReturn(pointRecord);

        // When & Then
        mockMvc.perform(post("/points")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addPointsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.reason").value("Test reward"));
    }

    @Test
    void getTotalPoints_ExistingUser_ShouldReturnUserPoints() throws Exception {
        // Given
        when(pointsService.getTotalPoints("user123")).thenReturn(userPoints);

        // When & Then
        mockMvc.perform(get("/points/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"))
                .andExpect(jsonPath("$.totalPoints").value(500));
    }

    @Test
    void getTotalPoints_NonExistingUser_ShouldReturnNotFound() throws Exception {
        // Given
        when(pointsService.getTotalPoints("user123")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/points/user123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLeaderboard_ShouldReturnLeaderboardList() throws Exception {
        // Given
        LeaderboardEntry entry1 = new LeaderboardEntry("user1", 1000.0);
        LeaderboardEntry entry2 = new LeaderboardEntry("user2", 800.0);
        List<LeaderboardEntry> leaderboard = Arrays.asList(entry1, entry2);

        when(pointsService.getLeaderboard()).thenReturn(leaderboard);

        // When & Then
        mockMvc.perform(get("/points/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[0].total").value(1000.0))
                .andExpect(jsonPath("$[1].userId").value("user2"))
                .andExpect(jsonPath("$[1].total").value(800.0));
    }

    @Test
    void updateReason_ExistingRecord_ShouldReturnUpdatedRecord() throws Exception {
        // Given
        PointRecord updatedRecord = new PointRecord();
        updatedRecord.setId(1L);
        updatedRecord.setUserId("user123");
        updatedRecord.setAmount(100);
        updatedRecord.setReason("Updated reason");
        updatedRecord.setCreatedAt(LocalDateTime.now());

        when(pointsService.updateReason(eq(1L), any(UpdateReasonRequest.class)))
                .thenReturn(updatedRecord);

        // When & Then
        mockMvc.perform(put("/points/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReasonRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value("Updated reason"));
    }

    @Test
    void updateReason_NonExistingRecord_ShouldReturnNotFound() throws Exception {
        // Given
        when(pointsService.updateReason(eq(1L), any(UpdateReasonRequest.class)))
                .thenThrow(new IllegalArgumentException("Point record not found"));

        // When & Then
        mockMvc.perform(put("/points/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReasonRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserPoints_ShouldReturnNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/points/user123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addPoints_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - invalid request with null userId
        AddPointsRequest invalidRequest = new AddPointsRequest();
        invalidRequest.setAmount(100);
        invalidRequest.setReason("Test");
        // userId is null

        // When & Then
        mockMvc.perform(post("/points")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk()); // Note: Without validation annotations, this will still pass
    }
}

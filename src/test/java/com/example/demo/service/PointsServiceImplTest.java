package com.example.demo.service;

import com.example.demo.model.dto.AddPointsRequest;
import com.example.demo.model.dto.LeaderboardEntry;
import com.example.demo.model.dto.UpdateReasonRequest;
import com.example.demo.model.entity.PointRecord;
import com.example.demo.model.entity.UserPoints;
import com.example.demo.repository.PointRecordRepository;
import com.example.demo.repository.UserPointsRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private PointRecordRepository pointRecordRepository;

    @Mock
    private UserPointsRepository userPointsRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private PointsServiceImpl pointsService;

    private AddPointsRequest addPointsRequest;
    private UserPoints existingUserPoints;
    private PointRecord pointRecord;

    @BeforeEach
    void setUp() {
        addPointsRequest = new AddPointsRequest();
        addPointsRequest.setUserId("user123");
        addPointsRequest.setAmount(100);
        addPointsRequest.setReason("Test reward");

        existingUserPoints = new UserPoints();
        existingUserPoints.setUserId("user123");
        existingUserPoints.setTotalPoints(500L);

        pointRecord = new PointRecord();
        pointRecord.setId(1L);
        pointRecord.setUserId("user123");
        pointRecord.setAmount(100);
        pointRecord.setReason("Test reward");
        pointRecord.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void addPoints_ExistingUser_ShouldUpdatePointsSuccessfully() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(userPointsRepository.findById("user123")).thenReturn(Optional.of(existingUserPoints));
        when(pointRecordRepository.save(any(PointRecord.class))).thenReturn(pointRecord);
        when(userPointsRepository.save(any(UserPoints.class))).thenReturn(existingUserPoints);

        // When
        PointRecord result = pointsService.addPoints(addPointsRequest);

        // Then
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(100, result.getAmount());
        assertEquals("Test reward", result.getReason());

        verify(pointRecordRepository).save(any(PointRecord.class));
        verify(userPointsRepository).save(argThat(userPoints -> 
            userPoints.getTotalPoints() == 600)); // 500 + 100
        verify(zSetOperations).add(eq("leaderboard"), eq("user123"), eq(600.0));
        verify(rocketMQTemplate).convertAndSend(eq("user-points-topic"), any(PointRecord.class));
    }

    @Test
    void addPoints_NewUser_ShouldCreateNewUserAndAddPoints() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(userPointsRepository.findById("user123")).thenReturn(Optional.empty());
        when(pointRecordRepository.save(any(PointRecord.class))).thenReturn(pointRecord);
        when(userPointsRepository.save(any(UserPoints.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PointRecord result = pointsService.addPoints(addPointsRequest);

        // Then
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(100, result.getAmount());
        assertEquals("Test reward", result.getReason());

        verify(pointRecordRepository).save(any(PointRecord.class));
        verify(userPointsRepository).save(argThat(userPoints -> 
            userPoints.getTotalPoints() == 100));
        verify(zSetOperations).add(eq("leaderboard"), eq("user123"), eq(100.0));
        verify(rocketMQTemplate).convertAndSend(eq("user-points-topic"), any(PointRecord.class));
    }

    @Test
    void getTotalPoints_ExistingUser_ShouldReturnUserPoints() {
        // Given
        when(userPointsRepository.findById("user123")).thenReturn(Optional.of(existingUserPoints));

        // When
        UserPoints result = pointsService.getTotalPoints("user123");

        // Then
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(500, result.getTotalPoints());
        verify(userPointsRepository).findById("user123");
    }

    @Test
    void getTotalPoints_NonExistingUser_ShouldReturnNull() {
        // Given
        when(userPointsRepository.findById("user123")).thenReturn(Optional.empty());

        // When
        UserPoints result = pointsService.getTotalPoints("user123");

        // Then
        assertNull(result);
        verify(userPointsRepository).findById("user123");
    }

    @Test
    void getLeaderboard_WithData_ShouldReturnSortedLeaderboard() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        tuples.add(ZSetOperations.TypedTuple.of("user1", 1000.0));
        tuples.add(ZSetOperations.TypedTuple.of("user2", 800.0));
        when(zSetOperations.reverseRangeWithScores("leaderboard", 0, 9)).thenReturn(tuples);

        // When
        List<LeaderboardEntry> result = pointsService.getLeaderboard();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(zSetOperations).reverseRangeWithScores("leaderboard", 0, 9);
    }

    @Test
    void getLeaderboard_EmptyData_ShouldReturnEmptyList() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("leaderboard", 0, 9)).thenReturn(Collections.emptySet());

        // When
        List<LeaderboardEntry> result = pointsService.getLeaderboard();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateReason_ExistingRecord_ShouldUpdateSuccessfully() {
        // Given
        UpdateReasonRequest updateRequest = new UpdateReasonRequest();
        updateRequest.setReason("Updated reason");
        
        when(pointRecordRepository.findById(1L)).thenReturn(Optional.of(pointRecord));
        when(pointRecordRepository.save(any(PointRecord.class))).thenReturn(pointRecord);

        // When
        PointRecord result = pointsService.updateReason(1L, updateRequest);

        // Then
        assertNotNull(result);
        verify(pointRecordRepository).findById(1L);
        verify(pointRecordRepository).save(argThat(record -> 
            record.getReason().equals("Updated reason")));
    }

    @Test
    void updateReason_NonExistingRecord_ShouldThrowException() {
        // Given
        UpdateReasonRequest updateRequest = new UpdateReasonRequest();
        updateRequest.setReason("Updated reason");
        
        when(pointRecordRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> pointsService.updateReason(1L, updateRequest)
        );
        
        assertEquals("Point record not found with id: 1", exception.getMessage());
        verify(pointRecordRepository).findById(1L);
        verify(pointRecordRepository, never()).save(any());
    }

    @Test
    void deleteUserPoints_ShouldDeleteAllUserData() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        String userId = "user123";
        doNothing().when(pointRecordRepository).deleteByUserId(userId);
        doNothing().when(userPointsRepository).deleteById(userId);

        // When
        pointsService.deleteUserPoints(userId);

        // Then
        verify(pointRecordRepository).deleteByUserId(userId);
        verify(userPointsRepository).deleteById(userId);
        verify(zSetOperations).remove("leaderboard", userId);
    }

    @Test
    void addPoints_ShouldHandleNegativePoints() {
        // Given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        addPointsRequest.setAmount(-50);
        when(userPointsRepository.findById("user123")).thenReturn(Optional.of(existingUserPoints));
        when(pointRecordRepository.save(any(PointRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PointRecord result = pointsService.addPoints(addPointsRequest);

        // Then
        assertNotNull(result);
        verify(pointRecordRepository).save(any(PointRecord.class));
        verify(userPointsRepository).save(argThat(userPoints -> 
            userPoints.getTotalPoints() == 450)); // 500 - 50
        verify(zSetOperations).add(eq("leaderboard"), eq("user123"), eq(450.0));
        verify(rocketMQTemplate).convertAndSend(eq("user-points-topic"), any(PointRecord.class));
    }
}

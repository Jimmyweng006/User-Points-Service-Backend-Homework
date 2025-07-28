package com.example.demo.service;

import com.example.demo.model.dto.AddPointsRequest;
import com.example.demo.model.dto.LeaderboardEntry;
import com.example.demo.model.dto.UpdateReasonRequest;
import com.example.demo.model.entity.PointRecord;
import com.example.demo.model.entity.UserPoints;
import com.example.demo.repository.PointRecordRepository;
import com.example.demo.repository.UserPointsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private static final String LEADERBOARD_KEY = "leaderboard";
    private static final String USER_POINTS_CACHE_KEY = "user_points";
    private static final String POINTS_TOPIC = "user-points-topic";

    private final PointRecordRepository pointRecordRepository;
    private final UserPointsRepository userPointsRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional
    @CacheEvict(value = USER_POINTS_CACHE_KEY, key = "#request.userId")
    public PointRecord addPoints(AddPointsRequest request) {
        // 1. Store the points record in MySQL
        PointRecord pointRecord = new PointRecord();
        pointRecord.setUserId(request.getUserId());
        pointRecord.setAmount(request.getAmount());
        pointRecord.setReason(request.getReason());
        pointRecordRepository.save(pointRecord);

        // 2. Update the user's total points
        UserPoints userPoints = userPointsRepository.findById(request.getUserId())
                .orElseGet(() -> {
                    UserPoints newUser = new UserPoints();
                    newUser.setUserId(request.getUserId());
                    return newUser;
                });

        userPoints.setTotalPoints(userPoints.getTotalPoints() + request.getAmount());
        userPointsRepository.save(userPoints);

        // 3. Update the leaderboard in Redis
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, request.getUserId(), userPoints.getTotalPoints());

        // 4. Publish a message to RocketMQ
        rocketMQTemplate.convertAndSend(POINTS_TOPIC, pointRecord);

        log.info("Added {} points to user {} for reason: {}",
                request.getAmount(), request.getUserId(), request.getReason());
        return pointRecord;
    }

    @Override
    @Cacheable(value = USER_POINTS_CACHE_KEY, key = "#userId")
    public UserPoints getTotalPoints(String userId) {
        log.info("Fetching points for user {} from database", userId);
        return userPointsRepository.findById(userId).orElse(null);
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard() {
        Set<ZSetOperations.TypedTuple<String>> rangeWithScores =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, 9);
        if (rangeWithScores == null) {
            return List.of();
        }
        return rangeWithScores.stream()
                .map(tuple -> new LeaderboardEntry(tuple.getValue(), tuple.getScore()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PointRecord updateReason(Long id, UpdateReasonRequest request) {
        PointRecord pointRecord = pointRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Point record not found with id: " + id));

        pointRecord.setReason(request.getReason());
        PointRecord updatedRecord = pointRecordRepository.save(pointRecord);
        log.info("Updated reason for point record id {}: {}", id, request.getReason());
        return updatedRecord;
    }

    @Override
    @Transactional
    @CacheEvict(value = USER_POINTS_CACHE_KEY, key = "#userId")
    public void deleteUserPoints(String userId) {
        // 1. Remove all point records for the user from MySQL
        pointRecordRepository.deleteByUserId(userId);

        // 2. Remove user's total points summary
        userPointsRepository.deleteById(userId);

        // 3. Remove user from Redis leaderboard
        redisTemplate.opsForZSet().remove(LEADERBOARD_KEY, userId);

        log.info("Deleted all points and data for user {}", userId);
    }
}

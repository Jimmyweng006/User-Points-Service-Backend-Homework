package com.example.demo.repository;

import com.example.demo.model.entity.PointRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {
    void deleteByUserId(String userId);
    List<PointRecord> findByUserId(String userId);
}

package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_points")
public class UserPoints implements Serializable {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "total_points", nullable = false)
    private Long totalPoints = 0L;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.example.demo.model.dto;

import lombok.Data;

@Data
public class AddPointsRequest {
    private String userId;
    private Integer amount;
    private String reason;
}

package com.example.minion.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Task {
    private UUID taskId;
    private String hash;
    private long startRange;
    private long endRange;
}

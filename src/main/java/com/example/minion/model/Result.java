package com.example.minion.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Result {
    private Task task;
    private Boolean isSuccess;
    private String password;
    private UUID minionId;
}

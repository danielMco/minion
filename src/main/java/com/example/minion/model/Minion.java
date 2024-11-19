package com.example.minion.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Minion {
    private UUID id;
    private String url;
}

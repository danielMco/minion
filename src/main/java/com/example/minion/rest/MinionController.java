package com.example.minion.rest;

import com.example.minion.model.Task;
import com.example.minion.service.MinionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/minion")
public class MinionController {

    private final MinionService minionService;


    public MinionController(MinionService minionService) {
        this.minionService = minionService;
    }

    @PostMapping("/receiveTask")
    public ResponseEntity<String> receiveTask(@RequestBody Task task) {
        try {
            minionService.enqueueTask(task);
            return ResponseEntity.ok("Task received and processing started.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error receiving task: " + e.getMessage());
        }
    }

    @GetMapping("/heartbeat")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Minion is healthy and running.");
    }
}

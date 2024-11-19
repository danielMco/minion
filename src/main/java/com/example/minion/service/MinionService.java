package com.example.minion.service;

import com.example.minion.client.MasterClient;
import com.example.minion.model.Minion;
import com.example.minion.model.Result;
import com.example.minion.model.Task;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.minion.utils.PhoneHashUtil.formatPhoneNumber;
import static com.example.minion.utils.PhoneHashUtil.md5;

@Service
public class MinionService {
    private final MasterClient masterClient;
    private final ExecutorService executorService;
    private final String minionUrl;
    private final UUID minionId = UUID.randomUUID();
    private static final Logger logger = LoggerFactory.getLogger(MinionService.class);
    private static final int MAX_ITERATIONS_PER_THREAD = 100_000;

    public MinionService(MasterClient masterClient, @Value("${server.port}") int port) {
        this.masterClient = masterClient;
        this.executorService = Executors.newFixedThreadPool(10); // Adjust pool size as needed
        this.minionUrl = "http://localhost:" + port + "/minion";
    }

    @PostConstruct
    public void registerWithMaster() {
        Minion minion = new Minion(minionId, minionUrl);
        try {
            masterClient.registerMinion(minion);
            logger.debug("Minion registered with Master.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to register with Master: " + e.getMessage());
        }
    }

    public void enqueueTask(Task task) {
        long rangeSize = task.getEndRange() - task.getStartRange() + 1;
        int threadCount = (int) Math.ceil((double) rangeSize / MAX_ITERATIONS_PER_THREAD);

        logger.info("Task {}: Total range = {}, Threads = {}, Max iterations per thread = {}",
                task.getTaskId(), rangeSize, threadCount, MAX_ITERATIONS_PER_THREAD);

        AtomicReference<String> matchedPhone = new AtomicReference<>(null);
        AtomicBoolean isMatchFound = new AtomicBoolean(false);
        List<Future<?>> futures = new ArrayList<>();

        long chunkSize = rangeSize / threadCount;

        for (int i = 0; i < threadCount; i++) {
            long start = task.getStartRange() + i * chunkSize;
            long end = (i == threadCount - 1) ? task.getEndRange() : start + chunkSize - 1;

            futures.add(executorService.submit(() -> processChunk(task, start, end, matchedPhone, isMatchFound)));
        }

        executorService.submit(() -> {
            boolean success = waitForCompletion(futures);
            handleTaskResult(task, matchedPhone, success);
        });

        logger.info("Task {} enqueued with {} chunks.", task.getTaskId(), futures.size());
    }

    private void processChunk(Task task, long startRange, long endRange, AtomicReference<String> matchedPhone, AtomicBoolean isMatchFound) {
        logger.info("Processing chunk: Task {}, Range {}-{}", task.getTaskId(), startRange, endRange);

        try {
            for (long i = startRange; i <= endRange; i++) {
                if (isMatchFound.get()) { // Stop processing if a match is already found
                    return;
                }

                String phoneNumber = formatPhoneNumber(i);
                String hashed = md5(phoneNumber);

                if (hashed.equals(task.getHash())) {
                    matchedPhone.set(phoneNumber);
                    isMatchFound.set(true);
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing chunk for Task {}: {}", task.getTaskId(), e.getMessage());
        }
    }

    private boolean waitForCompletion(List<Future<?>> futures) {
        boolean success = true;

        for (Future<?> future : futures) {
            try {
                future.get(); // Wait for each thread to complete
            } catch (Exception e) {
                logger.error("Error waiting for task completion: {}", e.getMessage());
                success = false;
            }
        }
        return success;
    }

    private void handleTaskResult(Task task, AtomicReference<String> matchedPhone, boolean success) {
        String phoneNumber = matchedPhone.get();

        try {
            if (success) {
                if (phoneNumber != null) {
                    sendResultToMaster(new Result(task, true, phoneNumber, minionId));
                } else {
                    sendResultToMaster(new Result(task, true, null, minionId));
                }
            } else {
                sendResultToMaster(new Result(task, false, null, minionId));
            }
        } catch (Exception e) {
            logger.error("Failed to send result to Master: {}", e.getMessage());
        }
    }

    private void sendResultToMaster(Result result) {
        try {
            masterClient.sendResult(result);
            logger.info("Result sent to Master: {}", result);
        } catch (Exception e) {
            logger.error("Client failed to send result to Master: {}", e.getMessage());
        }
    }
}


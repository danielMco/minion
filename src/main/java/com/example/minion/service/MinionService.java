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

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.minion.utils.PhoneHashUtil.formatPhoneNumber;
import static com.example.minion.utils.PhoneHashUtil.md5;

@Service
public class MinionService {
    private final MasterClient masterClient;
    private final ExecutorService executorService;
    private final String minionUrl;
    private final UUID minionId = UUID.randomUUID();
    private static final Logger logger = LoggerFactory.getLogger(MinionService.class);

    public MinionService(MasterClient masterClient, @Value("${server.port}") int port) {
        this.masterClient = masterClient;
        this.executorService = Executors.newFixedThreadPool(10);
        this.minionUrl = "http://localhost:" + port + "/minion";
    }

    @PostConstruct
    public void registerWithMaster() {
        Minion minion = new Minion(minionId,minionUrl);
        try {
            masterClient.registerMinion(minion);
            logger.info("Minion registered with Master.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to register with Master: " + e.getMessage());
        }
    }

    public void enqueueTask(Task task) {
        executorService.submit(() -> processTask(task));
    }

    private void processTask(Task task) {
        try {
            logger.info("Processing task asynchronously: {}", task.getTaskId());
            for (long i = task.getStartRange(); i <= task.getEndRange(); i++) {
                String phoneNumber = formatPhoneNumber(i);
                String hashed = md5(phoneNumber);

                if (hashed.equals(task.getHash())) {
                    sendResultToMaster(new Result(task,true, phoneNumber,minionId));
                    return;
                }
            }
            // If no match is found, notify the Master
            sendResultToMaster(new Result(task,true, null,minionId));
        } catch (Exception e) {
            logger.error("failure during process task {}: {}", task.getTaskId(), e.getMessage());
            sendResultToMaster(new Result(task,false,null,minionId));
        }

    }

    private void sendResultToMaster(Result result) {
        try {
            masterClient.sendResult(result);
            logger.info("Result sent to Master: {}", result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send result to Master: " + e.getMessage());
        }
    }
}

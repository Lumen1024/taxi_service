package com.lumen1024.notification_service.service;

import com.lumen1024.notification_service.entity.NotificationStatus;
import com.lumen1024.notification_service.entity.NotificationTask;
import com.lumen1024.notification_service.repository.NotificationTaskRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private static final int THREAD_COUNT = 4;
    private static final int MAX_RETRIES = 3;

    private final NotificationTaskRepository taskRepository;
    private final PlatformTransactionManager transactionManager;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(this::processTasks);
        }
        log.info("Notification worker started with {} threads", THREAD_COUNT);
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification worker stopped");
    }

    private void processTasks() {
        while (running.get()) {
            try {
                Long taskId = claimTask();
                if (taskId == null) {
                    Thread.sleep(5000);
                    continue;
                }

                boolean success = simulateSend(taskId);
                completeTask(taskId, success);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    private Long claimTask() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            var opt = taskRepository.findNextPending(NotificationStatus.PENDING);
            if (opt.isEmpty()) {
                return null;
            }
            NotificationTask task = opt.get();
            task.setStatus(NotificationStatus.PROCESSING);
            task.setAttempts(task.getAttempts() + 1);
            taskRepository.save(task);
            return task.getId();
        });
    }

    private boolean simulateSend(Long taskId) {
        try {
            // refresh to get latest state (so we can log the message)
            NotificationTask task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                log.info("Sending notification #{}: {} (attempt {})",
                    taskId, task.getMessage(), task.getAttempts());
            }
            long delay = 1000 + (long) (Math.random() * 2000);
            Thread.sleep(delay);
            log.info("Notification #{} sent successfully", taskId);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Notification #{} failed: {}", taskId, e.getMessage());
            return false;
        }
    }

    private void completeTask(Long taskId, boolean success) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            NotificationTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                return;
            }
            if (success) {
                task.setStatus(NotificationStatus.SENT);
            } else if (task.getAttempts() >= MAX_RETRIES) {
                task.setStatus(NotificationStatus.FAILED);
            } else {
                task.setStatus(NotificationStatus.PENDING);
            }
            taskRepository.save(task);
        });
    }
}

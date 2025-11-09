package com.example.demo.dispatcher;


import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import com.example.demo.service.JobExecutionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class QueryJobDispatcher {
    private final QueryRepository queryRepository;
    private final QueryJobRepository jobRepo;
    private final JobExecutionService executor;

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private Thread consumer;

    public QueryJobDispatcher(QueryJobRepository jobRepo, JobExecutionService executor,QueryRepository queryRepository) {
        this.jobRepo = jobRepo;
        this.executor = executor;
        this.queryRepository=queryRepository;
    }

    @PostConstruct
    public void start() {
        // Recovery: RUNNING -> QUEUED
        jobRepo.resetRunningToQueued();

        // Preload QUEUED jobs into memory queue
        List<QueryJob> queued = jobRepo.findByStatusIn(List.of(QueryJobStatus.QUEUED));
        queued.forEach(j -> queue.offer(j.getId()));

        // Start consumer loop
        consumer = new Thread(this::consumeLoop, "JobDispatcher");
        consumer.start();
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        running = false;
        if (consumer != null) {
            consumer.interrupt();
            consumer.join(2000);
        }
    }

    private void consumeLoop() {
        while (running) {
            try {
                //executing query async
                Long jobId = queue.take();
                executor.executeJobAsync(jobId);
            } catch (InterruptedException e) {
                // exit or continue based on running flag
                if (!running) return;
            }
        }
    }

    /** External API: enqueue a job-id for processing */
    public void enqueue(long jobId) {
        queue.offer(jobId);
    }
}

package com.example.demo.dispatcher;


import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.service.JobExecutionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class QueryJobDispatcher {
    private final QueryJobRepository jobRepo;
    private final JobExecutionService executor;
    //we chose BlockingQueue so that a thread stays blocked as much as the queue empty
    //it's also the standard for multi threaded envs
    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    //volatile here is crucial ,it ensures that every thread reads from RAM and writes to RAM
    //that way the thread sees instantly when running is set to false
    private volatile boolean running = true;
    //the consumer that's going to poll the queue and executes them
    private Thread consumer;

    public QueryJobDispatcher(QueryJobRepository jobRepo, JobExecutionService executor) {
        this.jobRepo = jobRepo;
        this.executor = executor;
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
        consumer.interrupt();
        consumer.join(2000);
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

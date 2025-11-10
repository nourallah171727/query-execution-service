package com.example.demo.dispatcher;


import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class QueryJobDispatcherTest {
    @Autowired
    private QueryJobRepository jobRepo;
    @Autowired private QueryRepository queryRepo;
    @Autowired private QueryResultCache cache;

    @Test
    void onStartup_recoveryAndExecutionCompleted() throws Exception {
        // 🔹 Wait a bit for the dispatcher thread to consume all jobs
        Thread.sleep(2000);

        // 1️⃣ No RUNNING or QUEUED jobs should remain
        long stillRunning = jobRepo.countByStatus(QueryJobStatus.RUNNING);
        long stillQueued  = jobRepo.countByStatus(QueryJobStatus.QUEUED);
        assertEquals(0, stillRunning, "All RUNNING should have been reset & executed");
        assertEquals(0, stillQueued, "All QUEUED should have been dispatched");

        // 2️⃣ There should be only SUCCEEDED or FAILED jobs
        List<QueryJob> jobs = jobRepo.findAll();
        assertTrue(jobs.stream().allMatch(j ->
                j.getStatus() == QueryJobStatus.SUCCEEDED ||
                        j.getStatus() == QueryJobStatus.FAILED));

        // 3️⃣ Cache should contain results for succeeded queries
        jobs.stream()
                .filter(j -> j.getStatus() == QueryJobStatus.SUCCEEDED)
                .forEach(j -> assertTrue(
                        cache.contains(j.getQueryId()),
                        "Cache must contain result for query " + j.getQueryId()
                ));

        // 4️⃣ Optional: verify failed jobs have error messages
        jobs.stream()
                .filter(j -> j.getStatus() == QueryJobStatus.FAILED)
                .forEach(j -> assertNotNull(j.getError()));
    }
}

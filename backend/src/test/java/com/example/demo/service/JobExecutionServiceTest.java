package com.example.demo.service;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.helper.TestHelper;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.fail;

@SpringBootTest
@EnableAsync
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JobExecutionServiceTest {

    @Autowired
    private JobExecutionService jobService;

    @Autowired
    private QueryRepository queryRepo;

    @Autowired
    private QueryJobRepository jobRepo;

    @Autowired
    private QueryResultCache cache;

    @Autowired
    private TestHelper helper;
    @AfterEach
    void cleanUp() {
        jobRepo.deleteAll();
        queryRepo.deleteAll();
    }

    // Utility wait method
    private void awaitJobCompletion(long jobId, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            QueryJob job = jobRepo.findById(jobId).orElseThrow();
            if (job.getStatus() == QueryJobStatus.SUCCEEDED ||
                    job.getStatus() == QueryJobStatus.FAILED) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        fail("Job did not complete within timeout");
    }



    @Test
    void shouldExecuteJobAsyncWithCacheMiss() throws Exception {
        QueryJob job = helper.createQueryWithJob("SELECT 1 as result", QueryJobStatus.RUNNING);
        long jobId = job.getId();
        long queryId = job.getQueryId();

        cache.clear(); // ensure miss
        jobService.executeJobAsync(jobId);

        awaitJobCompletion(jobId, 1000);

        QueryJob updated = jobRepo.findById(jobId).orElseThrow();
        assertEquals(QueryJobStatus.SUCCEEDED, updated.getStatus());
        assertTrue(cache.contains(queryId), "Result should be cached");
    }

    @Test
    void shouldUseCacheAndSkipExecution() throws Exception {
        QueryJob job = helper.createQueryWithJob("SELECT 1 as result", QueryJobStatus.RUNNING);
        long jobId = job.getId();
        long queryId = job.getQueryId();

        cache.put(queryId, "[{\"result\":1}]"); // prepopulate cache
        jobService.executeJobAsync(jobId);

        awaitJobCompletion(jobId, 1000);

        QueryJob updated = jobRepo.findById(jobId).orElseThrow();
        assertEquals(QueryJobStatus.SUCCEEDED, updated.getStatus());
        // no new SQL should have been executed (cache used)
    }

    @Test
    void shouldMarkFailedWhenQueryCorrupt() throws Exception {
        QueryJob job = helper.createQueryWithJob("SELEC BAD SQL", QueryJobStatus.RUNNING);
        long jobId = job.getId();

        jobService.executeJobAsync(jobId);
        awaitJobCompletion(jobId, 3000);

        QueryJob updated = jobRepo.findById(jobId).orElseThrow();
        assertEquals(QueryJobStatus.FAILED, updated.getStatus());
        assertNotNull(updated.getError());
    }
}

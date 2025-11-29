package com.example.demo.unit;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import com.example.demo.service.JobResultService;
import com.example.demo.util.QueryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobResultServiceUnitTest {
    private QueryJobRepository jobRepo;
    private QueryRepository queryRepo;
    private QueryResultCache cache;
    private JobResultService service;

    @BeforeEach
    void setUp() {
        jobRepo = mock(QueryJobRepository.class);
        queryRepo = mock(QueryRepository.class);
        cache = mock(QueryResultCache.class);

        service = new JobResultService(jobRepo, queryRepo, cache);
    }

    // ---------------------------------------------------------
    // 1. RUNNING
    // ---------------------------------------------------------
    @Test
    void testRunningJob() {
        QueryJob job = new QueryJob();
        job.setId(1L);
        job.setStatus(QueryJobStatus.RUNNING);

        when(jobRepo.findById(1L)).thenReturn(Optional.of(job));

        Map<String, Object> result = service.getResult(1L);

        assertEquals("RUNNING", result.get("status"));
        assertEquals("Job still processing, please retry later.", result.get("message"));
    }

    // ---------------------------------------------------------
    // 2. FAILED
    // ---------------------------------------------------------
    @Test
    void testFailedJob() {
        QueryJob job = new QueryJob();
        job.setId(2L);
        job.setStatus(QueryJobStatus.FAILED);
        job.setError("Some failure");

        when(jobRepo.findById(2L)).thenReturn(Optional.of(job));

        Map<String, Object> result = service.getResult(2L);

        assertEquals("FAILED", result.get("status"));
        assertEquals("Some failure", result.get("error"));
    }

    // ---------------------------------------------------------
    // 3. SUCCEEDED (WRITE QUERY)
    // ---------------------------------------------------------
    @Test
    void testSucceededWriteQuery() {
        QueryJob job = new QueryJob();
        job.setId(3L);
        job.setStatus(QueryJobStatus.SUCCEEDED);
        job.setQueryId(10L);

        when(jobRepo.findById(3L)).thenReturn(Optional.of(job));
        when(queryRepo.findSqlById(10L)).thenReturn("UPDATE passengers SET Age = 10");

        // Simulate write query
        assertTrue(QueryUtil.isWriteQuery("UPDATE passengers SET Age = 10"));

        Map<String, Object> result = service.getResult(3L);

        assertEquals("SUCCEEDED", result.get("status"));
        assertEquals("Dataset updated successfully!", result.get("result"));
    }

    // ---------------------------------------------------------
    // 4. SUCCEEDED (READ QUERY + CACHE HIT)
    // ---------------------------------------------------------
    @Test
    void testSucceededReadQuery_cacheHit() {
        QueryJob job = new QueryJob();
        job.setId(4L);
        job.setStatus(QueryJobStatus.SUCCEEDED);
        job.setQueryId(20L);

        when(jobRepo.findById(4L)).thenReturn(Optional.of(job));
        when(queryRepo.findSqlById(20L)).thenReturn("SELECT * FROM passengers");
        when(cache.get(20L)).thenReturn("[{\"id\":1}]");

        Map<String, Object> result = service.getResult(4L);

        assertEquals("SUCCEEDED", result.get("status"));
        assertEquals("[{\"id\":1}]", result.get("result"));  // hit
    }

    // ---------------------------------------------------------
    // 5. SUCCEEDED (READ QUERY + CACHE MISS)
    // ---------------------------------------------------------
    @Test
    void testSucceededReadQuery_cacheMiss() {
        QueryJob job = new QueryJob();
        job.setId(5L);
        job.setStatus(QueryJobStatus.SUCCEEDED);
        job.setQueryId(30L);

        when(jobRepo.findById(5L)).thenReturn(Optional.of(job));
        when(queryRepo.findSqlById(30L)).thenReturn("SELECT * FROM passengers");
        when(cache.get(30L)).thenReturn(null);  // cache expired

        Map<String, Object> result = service.getResult(5L);

        assertEquals("SUCCEEDED", result.get("status"));
        assertEquals("(cached result expired — please re-execute query)", result.get("result"));
    }

    // ---------------------------------------------------------
    // 6. Job Not Found
    // ---------------------------------------------------------
    @Test
    void testJobNotFound() {
        when(jobRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getResult(999L)
        );
    }
}

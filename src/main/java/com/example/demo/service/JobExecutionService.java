package com.example.demo.service;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JobExecutionService {

    private final QueryJobRepository jobRepo;
    private final QueryRepository queryRepo;
    private final JdbcTemplate jdbc;
    private final QueryResultCache cache;
    @Autowired
    public JobExecutionService(QueryJobRepository jobRepo,
                               QueryRepository queryRepo,
                               JdbcTemplate jdbc,
                               QueryResultCache cache) {
        this.jobRepo = jobRepo;
        this.queryRepo = queryRepo;
        this.jdbc = jdbc;
        this.cache = cache;
    }

    @Async("queryExecutor")
    public void executeJobAsync(long jobId) {
        System.out.println("Async thread executing job " + jobId +
                " on thread: " + Thread.currentThread().getName());
        // 1) load job
        QueryJob job = jobRepo.findById(jobId).orElse(null);

        if(job==null){
            return;
        }

        long queryId = job.getQueryId();

        try {
            // 2) cache short-circuit
            if (cache.contains(queryId)) {
                markSucceeded(job);
                return;
            }

            // 3) get SQL text
            String sql = queryRepo.findSqlById(queryId);

            // 4) run SQL (serialize result to JSON-ish)
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            String json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().writeValueAsString(rows);

            // 5) cache + mark success
            cache.put(queryId, json);
            markSucceeded(job);
        } catch (Exception e) {
            markFailed(job, e.getMessage());
        }
    }
    //mark a job as a success in the db
    protected void markSucceeded(QueryJob job) {
        job.setStatus(QueryJobStatus.SUCCEEDED);
        job.setError(null);
        jobRepo.save(job);
    }
    //mark a job as a failure in the db
    protected void markFailed(QueryJob job, String message) {
        job.setStatus(QueryJobStatus.FAILED);
        job.setError(message);
        jobRepo.save(job);
    }
}
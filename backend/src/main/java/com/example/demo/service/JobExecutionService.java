package com.example.demo.service;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import com.example.demo.util.QueryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        QueryJob job = jobRepo.findById(jobId).orElse(null);

        if(job==null){
            return;
        }
        long queryId = job.getQueryId();

        try {
            if(QueryUtil.isWriteQuery(queryRepo.findSqlById(queryId))){
                executeWriteQuery(queryId);
            }else{
                executeReadQuery(queryId);
            }
            markSucceeded(job);
        } catch (Exception e) {
            markFailed(job, e.getMessage());
        }
    }

    public void executeReadQuery(long queryId) throws Exception{
        // 1) cache short-circuit
        if (cache.contains(queryId)) {
            return;
        }

        // 2) get SQL text
        String sql = queryRepo.findSqlById(queryId);

        // 3) run SQL (serialize result to JSON-ish)
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build().writeValueAsString(rows);

        // 4) cache + mark success
        cache.put(queryId, json);
    }

    //must be a transactional since it's a write operation
    @Transactional
    public void executeWriteQuery(long queryId) throws Exception{
        // 1) Get SQL text
        String sql = queryRepo.findSqlById(queryId);

        // 2) Execute write statement
        jdbc.update(sql);

        // 3) Clear cache because dataset changed
        cache.clear();
    }


    //mark a job as a success in the db
    public  void markSucceeded(QueryJob job) {
        job.setStatus(QueryJobStatus.SUCCEEDED);
        job.setError(null);
        jobRepo.save(job);
    }
    //mark a job as a failure in the db
    public  void markFailed(QueryJob job, String message) {
        job.setStatus(QueryJobStatus.FAILED);
        job.setError(message);
        jobRepo.save(job);
    }
}
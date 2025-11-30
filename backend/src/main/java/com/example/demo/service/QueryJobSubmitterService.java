package com.example.demo.service;

import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
@Service
public class QueryJobSubmitterService {

    private final QueryJobRepository jobRepo;
    private final JobExecutionService jobExecutionService;
    private final QueryRepository queryRepo;
    private final QuerySecurityService querySecurity;

    public QueryJobSubmitterService(QueryJobRepository jobRepo,
                                    JobExecutionService jobExecutionService,
                                    QueryRepository queryRepo,
                                    QuerySecurityService querySecurity) {
        this.jobRepo = jobRepo;
        this.jobExecutionService = jobExecutionService;
        this.queryRepo = queryRepo;
        this.querySecurity = querySecurity;
    }

    public long submit(long queryId) {

        // 1. Load SQL
        Query query=queryRepo.findById(queryId).orElseThrow(()->new EntityNotFoundException("no query with such ID exists"));
        // 2. Ask the security service to validate permissions
        querySecurity.enforcePermission(query.getText());

        // 3. Normal job submission
        QueryJob job = new QueryJob(queryId, QueryJobStatus.RUNNING);
        job = jobRepo.save(job);
        jobExecutionService.executeJobAsync(job.getId());

        return job.getId();
    }
}
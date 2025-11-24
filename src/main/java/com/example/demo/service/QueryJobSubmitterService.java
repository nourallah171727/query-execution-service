package com.example.demo.service;

import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import org.springframework.stereotype.Service;
@Service
public class QueryJobSubmitterService {

        private final QueryJobRepository jobRepo;
        private final JobExecutionService jobExecutionService;

        public QueryJobSubmitterService(QueryJobRepository jobRepo, JobExecutionService jobExecutionService) {
            this.jobRepo = jobRepo;
            this.jobExecutionService = jobExecutionService;
        }
        //stores the Job in db and immediately starts a thread for execution
        public long submit(long queryId) {
            QueryJob job = new QueryJob(queryId, QueryJobStatus.RUNNING);
            job = jobRepo.save(job);
            jobExecutionService.executeJobAsync(job.getId());
            return job.getId();}
}

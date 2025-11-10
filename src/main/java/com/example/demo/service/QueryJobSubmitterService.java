package com.example.demo.service;

import com.example.demo.dispatcher.QueryJobDispatcher;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import org.springframework.stereotype.Service;
@Service
public class QueryJobSubmitterService {

        private final QueryJobRepository jobRepo;
        private final QueryJobDispatcher dispatcher;

        public QueryJobSubmitterService(QueryJobRepository jobRepo, QueryJobDispatcher dispatcher) {
            this.jobRepo = jobRepo;
            this.dispatcher = dispatcher;
        }

        public long submit(long queryId) {
            QueryJob job = new QueryJob(queryId, QueryJobStatus.QUEUED);
            job = jobRepo.save(job);
            dispatcher.enqueue(job.getId());
            return job.getId();}

}

package com.example.demo.recovery;


import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.service.JobExecutionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;


import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class QueryJobRecoveryService {

    private final QueryJobRepository jobRepo;
    private final JobExecutionService executor;

    public QueryJobRecoveryService(QueryJobRepository jobRepo, JobExecutionService executor) {
        this.jobRepo = jobRepo;
        this.executor = executor;
    }

    @PostConstruct
    public void onStartup() {
        // find unfinished jobs
        List<QueryJob> runningJobs = jobRepo.findByStatusIn(List.of(QueryJobStatus.RUNNING));

        // re-execute each one asynchronously
        for (QueryJob job : runningJobs) {
            executor.executeJobAsync(job.getId());
        }
    }
}
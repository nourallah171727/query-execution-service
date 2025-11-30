package com.example.demo.helper;

import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class TestHelper {
    private final QueryRepository queryRepo;
    private final QueryJobRepository jobRepo;

    public TestHelper(QueryRepository queryRepo, QueryJobRepository jobRepo) {
        this.queryRepo = queryRepo;
        this.jobRepo = jobRepo;
    }
    public QueryJob createQueryWithJob(String text, QueryJobStatus status) {
        Query query = new Query();
        query.setText(text);
        query = queryRepo.save(query);

        QueryJob job = new QueryJob();
        job.setQueryId(query.getId());
        job.setStatus(status);
        job.setError(null);
        job = jobRepo.save(job);
        return job;
    }
    public List<QueryJob> createMultipleQueryJobs(int count) {
        List<QueryJob> jobs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String text = "SELECT * FROM passengers WHERE PassengerId = " + i;
            QueryJob job = createQueryWithJob(text, QueryJobStatus.RUNNING);
            jobs.add(job);
        }
        return jobs;
    }

    /** Deletes all data (useful if rollback disabled). */
    public void clearAll() {
        jobRepo.deleteAll();
        queryRepo.deleteAll();
    }

    /** Counts how many queries exist. */
    public long countQueries() {
        return queryRepo.count();
    }

    /** Counts how many jobs exist. */
    public long countJobs() {
        return jobRepo.count();
    }

}

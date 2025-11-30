package com.example.demo.service;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.QueryJob;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import com.example.demo.util.QueryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JobResultService {

    private final QueryJobRepository jobRepo;
    private final QueryRepository queryRepo;
    private final QueryResultCache cache;

    @Autowired
    public JobResultService(QueryJobRepository jobRepo,
                            QueryRepository queryRepo,
                            QueryResultCache cache) {
        this.jobRepo = jobRepo;
        this.queryRepo = queryRepo;
        this.cache = cache;
    }

    public Map<String, Object> getResult(long jobId) {

        QueryJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("No job found with ID " + jobId));

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", job.getStatus().name());

        switch (job.getStatus()) {
            case RUNNING -> {
                response.put("message", "Job still processing.");
            }

            case FAILED -> {
                response.put("error", job.getError());
            }

            case SUCCEEDED -> {
                response.put("message", "Job completed successfully.");
                long queryId = job.getQueryId();
                String sql = queryRepo.findSqlById(queryId);

                boolean isWrite = QueryUtil.isWriteQuery(sql);

                if (isWrite) {
                    // Write queries do not have a result set
                    response.put("result", "Dataset updated successfully!");
                } else {
                    // Read query → fetch from cache
                    String cached = cache.get(queryId);
                    if (cached == null) {
                        response.put("result", "(cached result expired — please re-execute query)");
                    } else {
                        response.put("result", cached);
                    }
                }
            }
        }
        return response;
    }
}
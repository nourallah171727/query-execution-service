package com.example.demo.controller;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.service.QueryJobSubmitterService;
import com.example.demo.service.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueryController {

    private final QueryService queryService;
    private final QueryJobSubmitterService jobSubmitter;
    private final QueryJobRepository jobRepo;
    private final QueryResultCache cache;

    public QueryController(QueryService queryService,
                           QueryJobSubmitterService jobSubmitter,
                           QueryJobRepository jobRepo,
                           QueryResultCache cache) {
        this.queryService = queryService;
        this.jobSubmitter = jobSubmitter;
        this.jobRepo = jobRepo;
        this.cache = cache;
    }

    // ---------- 1️⃣ Add new query ----------
    @PostMapping
    public Map<String, Object> addQuery(@RequestBody String queryText) {
        Query q = queryService.saveQuery(queryText);
        return Map.of("id", q.getId(), "query", q.getText());
    }

    // ---------- 2️⃣ List all queries ----------
    @GetMapping
    public List<Map<String, Object>> listQueries() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Query q : queryService.getAllQueries()) {
            result.add(Map.of("id", q.getId(), "query", q.getText()));
        }
        return result;
    }

    // ---------- 3️⃣ Submit a query job ----------
    @PostMapping("/execute")
    public Map<String, Object> executeQuery(@RequestParam("queryId") Long queryId) {
        long jobId = jobSubmitter.submit(queryId);
        return Map.of("jobId", jobId, "status", "QUEUED");
    }

    // ---------- 4️⃣ Check job status or result ----------
    @GetMapping("/job/{id}")
    public Map<String, Object> getJobResult(@PathVariable("id") Long jobId) {
        QueryJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("No job found with ID " + jobId));

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", job.getStatus());

        switch (job.getStatus()) {
            case SUCCEEDED -> {
                String result = cache.get(job.getQueryId());
                response.put("result", result != null ? result : "(cached result expired)");
            }
            case FAILED -> {
                response.put("error", job.getError());
            }
            case QUEUED, RUNNING -> {
                response.put("message", "Job still processing, please retry later.");
            }
        }
        return response ;
    }
}
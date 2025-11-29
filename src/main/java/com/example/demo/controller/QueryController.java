package com.example.demo.controller;

import com.example.demo.cache.QueryResultCache;
import com.example.demo.entity.Query;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.service.JobResultService;
import com.example.demo.service.QueryJobSubmitterService;
import com.example.demo.service.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueryController {

    private final QueryService queryService;
    private final QueryJobSubmitterService jobSubmitter;
    private final QueryJobRepository jobRepo;
    private final QueryResultCache cache;
    private final JobResultService jobResultService;

    public QueryController(QueryService queryService,
                           QueryJobSubmitterService jobSubmitter,
                           QueryJobRepository jobRepo,
                           QueryResultCache cache,
                           JobResultService jobResultService) {
        this.queryService = queryService;
        this.jobSubmitter = jobSubmitter;
        this.jobRepo = jobRepo;
        this.cache = cache;
        this.jobResultService=jobResultService;
    }

    // ---------- 1. Add new query ----------
    @PostMapping
    public Map<String, Object> addQuery(@RequestBody String queryText) {
        Query q = queryService.saveQuery(queryText);
        return Map.of("id", q.getId(), "query", q.getText());
    }

    // ---------- 2. List all queries ----------
    @GetMapping
    public List<Map<String, Object>> listQueries() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Query q : queryService.getAllQueries()) {
            result.add(Map.of("id", q.getId(), "query", q.getText()));
        }
        return result;
    }

    // ---------- 3. Submit a query job ----------
    @PostMapping("/execute")
    public Map<String, Object> executeQuery(@RequestParam("queryId") Long queryId) {
        long jobId = jobSubmitter.submit(queryId);
        return Map.of("jobId", jobId, "status", "RUNNING");
    }

    // ---------- 4. Check job status or result ----------
    @GetMapping("/job/{id}")
    public Map<String, Object> getJobResult(@PathVariable("id") Long jobId) {
        return jobResultService.getResult(jobId);
    }
}
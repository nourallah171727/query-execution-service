package com.example.demo.controller;

import com.example.demo.entity.Query;
import com.example.demo.service.QueryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueryController {

    private final QueryService service;

    public QueryController(QueryService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> addQuery(@RequestBody String queryText) {
        Query q = service.saveQuery(queryText);
        return Map.of("id", q.getId());
    }

    @GetMapping
    public List<Map<String, Object>> listQueries() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Query q : service.getAllQueries()) {
            result.add(Map.of("id", q.getId(), "query", q.getText()));
        }
        return result;
    }
    @GetMapping("/execute")
    public List<List<Object>> executeQuery(@RequestParam Long query) {
        return service.executeQuery(query);
    }
}
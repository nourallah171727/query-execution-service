package com.example.demo.service;

import com.example.demo.entity.Query;
import com.example.demo.repository.QueryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final QueryRepository repository;

    public QueryService(QueryRepository repository) {
        this.repository = repository;
    }

    public Query saveQuery(String text) {
        return repository.save(new Query(text));
    }

    public List<Query> getAllQueries() {
        return repository.findAll();
    }
}
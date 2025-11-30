package com.example.demo.service;

import com.example.demo.entity.Query;
import com.example.demo.repository.QueryRepository;
import com.example.demo.util.QueryUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final QueryRepository repository;

    public QueryService(QueryRepository repository) {
        this.repository = repository;
    }

    public Query saveQuery(String text) {
        //only store queries that are correct in syntax
        if(!QueryUtil.isValidSql(text)){
            throw new IllegalArgumentException("invalid SQL syntax");
        }
        //only store queries related to the passengers table (Dataset)
        List<String>tablesInQuery=QueryUtil.extractTables(text);
        boolean dataSetOnlyRelatedQuery=tablesInQuery.size()==1 && tablesInQuery.contains("passengers");
        if(!dataSetOnlyRelatedQuery){
            throw new IllegalArgumentException("either you are not querying over passengers table " +
                    "or you are querying over more than the passengers table");
        }
        //save the query
        return repository.save(new Query(text));
    }

    public List<Query> getAllQueries() {
        return repository.findAll();
    }
}
package com.example.demo.service;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.entity.Query;
import com.example.demo.repository.QueryRepository;
import com.example.demo.role.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService {

    private final QueryRepository repository;
    private final DataSource adminDataSource;
    private final DataSource userDataSource;
    private final JdbcTemplate jdbcTemplate;
    public QueryService(QueryRepository repository, DataSource adminDataSource, DataSource userDataSource,JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.adminDataSource=adminDataSource;
        this.userDataSource=userDataSource;
        this.jdbcTemplate=jdbcTemplate;
    }

    public Query saveQuery(String text) {
        return repository.save(new Query(text));
    }

    public List<Query> getAllQueries() {
        return repository.findAll();
    }
    public List<List<Object>> executeQuery(Long id, Role role) {
        Query q = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        return jdbcTemplate.query(q.getText(), rs -> {
            List<List<Object>> rows = new ArrayList<>();
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        });
    }


}
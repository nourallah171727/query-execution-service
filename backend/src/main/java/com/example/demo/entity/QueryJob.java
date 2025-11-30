package com.example.demo.entity;

import com.example.demo.entity.enums.QueryJobStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "query_job")
public class QueryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_id", nullable = false)
    private Long queryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryJobStatus status;

    @Column(columnDefinition = "TEXT")
    private String error;

    public QueryJob() {}

    public QueryJob(Long queryId, QueryJobStatus status) {
        this.queryId = queryId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQueryId() {
        return queryId;
    }

    public void setQueryId(Long queryId) {
        this.queryId = queryId;
    }

    public QueryJobStatus getStatus() {
        return status;
    }

    public void setStatus(QueryJobStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
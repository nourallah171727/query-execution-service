package com.example.demo.repository;

import com.example.demo.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT text FROM query WHERE id = :id", nativeQuery = true)
    String findSqlById(@Param("id") Long id);
}
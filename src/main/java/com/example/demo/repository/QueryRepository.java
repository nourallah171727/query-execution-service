package com.example.demo.repository;

import com.example.demo.entity.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {
}
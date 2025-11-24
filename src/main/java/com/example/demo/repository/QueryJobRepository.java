package com.example.demo.repository;

import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface QueryJobRepository extends JpaRepository<QueryJob, Long> {
    List<QueryJob> findByStatusIn(Collection<QueryJobStatus> statuses);
}

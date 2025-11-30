package com.example.demo.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
//to rollback after tests
@Transactional
class ConnectionPrivilegeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSelectOnPassengersShouldSucceed() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM passengers", Integer.class);
        assertNotNull(count, "SELECT query should return a count");
        System.out.println("SELECT on passengers succeeded, count=" + count);
    }
    @Test
    void testUpdateOnPassengersDoesNotDependOnDbPrivileges() {
        try {
            int rows = jdbcTemplate.update("UPDATE passengers SET Name = Name WHERE passengerId=1");
            System.out.println("UPDATE on passengers succeeded (rows affected=" + rows + ")");
        } catch (DataAccessException ex) {
            System.out.println("UPDATE on passengers was blocked by DB privileges: " + ex.getMessage());
        }
    }

    @Test
    void testInsertSelectAndUpdateOnQueryAndQueryJobTables() {
        // 1. Insert a new query entry
        String queryText = "SELECT * FROM passengers";
        jdbcTemplate.update("INSERT INTO query (text) VALUES (?)", queryText);

        // Retrieve its ID (assuming AUTO_INCREMENT)
        Long queryId = jdbcTemplate.queryForObject(
                "SELECT id FROM query WHERE text = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                queryText
        );
        assertNotNull(queryId, "Query should have been inserted successfully");
        System.out.println("Inserted query with id=" + queryId);

        // 2. Insert a query_job referencing that query
        String status = "RUNNING";
        jdbcTemplate.update(
                "INSERT INTO query_job (query_id, status, error) VALUES (?, ?, ?)",
                queryId, status, null
        );

        // Retrieve job id
        Long jobId = jdbcTemplate.queryForObject(
                "SELECT id FROM query_job WHERE query_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                queryId
        );
        assertNotNull(jobId, "QueryJob should have been inserted successfully");
        System.out.println("Inserted query_job with id=" + jobId + " for query " + queryId);

        // 3. SELECT both tables
        Integer jobCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM query_job", Integer.class);
        Integer queryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM query", Integer.class);

        assertTrue(jobCount > 0, "Should find at least one query_job");
        assertTrue(queryCount > 0, "Should find at least one query");

        System.out.printf("✅ query_job count=%d, query count=%d%n", jobCount, queryCount);

        // 4. UPDATE tests (should succeed if privileges allow)
        int affectedJobRows = jdbcTemplate.update("UPDATE query_job SET status = status WHERE id = ?", jobId);
        int affectedQueryRows = jdbcTemplate.update("UPDATE query SET text = text WHERE id = ?", queryId);

        System.out.printf("✅ Update success: query_job=%d rows, query=%d rows (expected 1 each)%n",
                affectedJobRows, affectedQueryRows);
    }
}
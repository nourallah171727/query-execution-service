package com.example.demo.recovery;

import com.example.demo.DemoApplication;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

class ManualRecoveryTest {

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void setupDB() throws Exception {
        // Manually create JDBC connection
        com.mysql.cj.jdbc.MysqlDataSource ds = new com.mysql.cj.jdbc.MysqlDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/analytics");
        ds.setUser("user");
        ds.setPassword("analytics");
        jdbc = new JdbcTemplate(ds);

        // 1. Prepopulate database before booting Spring
        jdbc.execute("DELETE FROM query_job");
        jdbc.execute("DELETE FROM query");

        jdbc.execute("INSERT INTO query (id, text) VALUES (1, 'SELECT 1 AS result')");
        jdbc.execute("INSERT INTO query (id, text) VALUES (2, 'SELECT bad_sql')");
        jdbc.execute("INSERT INTO query_job (id, query_id, status) VALUES (10, 1, 'RUNNING')");
        jdbc.execute("INSERT INTO query_job (id, query_id, status) VALUES (11, 2, 'RUNNING')");
    }

    private void awaitJob(long jobId, QueryJobRepository repo) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis()-start < 4000) {
            QueryJob job = repo.findById(jobId).orElseThrow();
            if (job.getStatus() != QueryJobStatus.RUNNING) return;
            TimeUnit.MILLISECONDS.sleep(100);
        }
        fail("Job " + jobId + " never completed");
    }
    @AfterEach
    void cleanUpDb() {
        jdbc.execute("DELETE FROM query_job");
        jdbc.execute("DELETE FROM query");
    }

    @Test
    void manualRecovery_testFullLifecycle() throws Exception {

        // 2. Start Spring Boot manually
        ConfigurableApplicationContext ctx =
                org.springframework.boot.SpringApplication.run(DemoApplication.class);

        QueryJobRepository jobRepo = ctx.getBean(QueryJobRepository.class);

        // 3. Wait for recovery jobs to finish
        awaitJob(10L, jobRepo);
        awaitJob(11L, jobRepo);

        // 4. Validate states
        QueryJob j10 = jobRepo.findById(10L).orElseThrow();
        QueryJob j11 = jobRepo.findById(11L).orElseThrow();

        assertNotEquals(QueryJobStatus.RUNNING, j10.getStatus());
        assertNotEquals(QueryJobStatus.RUNNING, j11.getStatus());

        // 5. Shut down Spring cleanly
        ctx.close();

        // ---------------------------------------
        // 6. Boot Spring AGAIN (second startup)
        // ---------------------------------------

        ConfigurableApplicationContext ctx2 =
                org.springframework.boot.SpringApplication.run(DemoApplication.class);

        QueryJobRepository repo2 = ctx2.getBean(QueryJobRepository.class);
    }
}
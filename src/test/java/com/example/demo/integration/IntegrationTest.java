package com.example.demo.integration;

import com.example.demo.config.NoSecurityConfig;
import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
//a true integration E2E test!

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(NoSecurityConfig.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;
    @Autowired private QueryRepository queryRepo;
    @Autowired private QueryJobRepository jobRepo;

    private String baseUrl() {
        return "http://localhost:" + port + "/queries";
    }

    @AfterEach
    void cleanUp() {
        jobRepo.deleteAll();
        queryRepo.deleteAll();
    }

    // ---------- 1. POST /queries ----------
    @Test
    void addQuery_shouldPersistQuery() {
        String sql = "SELECT 1 AS result";
        ResponseEntity<Map> response = rest.postForEntity(baseUrl(), sql, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("id"));

        long id = ((Number) response.getBody().get("id")).longValue();
        assertTrue(queryRepo.existsById(id));
    }

    // ---------- 2. GET /queries ----------
    @Test
    void listQueries_shouldReturnAllSavedQueries() {
        queryRepo.save(new Query("SELECT 1"));
        queryRepo.save(new Query("SELECT 2"));

        ResponseEntity<List> response = rest.getForEntity(baseUrl(), List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 2);
    }

    // ---------- 3. POST /queries/execute ----------
    @Test
    void executeQuery_shouldCreateJobAndReturnJobId() {
        // Create query first
        Query q = queryRepo.save(new Query("SELECT 1 AS result"));

        // Submit job
        String url = baseUrl() + "/execute?queryId=" + q.getId();
        ResponseEntity<Map> response = rest.postForEntity(url, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("jobId"));

        long jobId = ((Number) body.get("jobId")).longValue();
        QueryJob job = jobRepo.findById(jobId).orElse(null);
        assertNotNull(job);


    }

    // ---------- 4. GET /queries/job/{id} ----------
    @Test
    void getJob_shouldReturnStatusAndResultEventually() throws Exception {
        // 1. Create and store a simple query
        Query q = queryRepo.save(new Query("SELECT 1 AS result"));

        // 2. Submit job
        String executeUrl = baseUrl() + "/execute?queryId=" + q.getId();
        Map<String, Object> execResponse = rest.postForEntity(executeUrl, null, Map.class).getBody();
        assertNotNull(execResponse);

        long jobId = ((Number) execResponse.get("jobId")).longValue();

        // 3. Wait for async execution to finish
        Thread.sleep(1000);

        // 4. GET job result
        String statusUrl = baseUrl() + "/job/" + jobId;
        ResponseEntity<Map> response = rest.getForEntity(statusUrl, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);

        // Basic fields
        assertEquals(jobId, ((Number) body.get("jobId")).longValue());
        assertTrue(body.containsKey("status"));
        assertEquals("SUCCEEDED", body.get("status"));

        // 5. Validate presence of the result
        assertTrue(body.containsKey("result"), "Response should contain 'result' when job succeeded");

        Object result = body.get("result");
        assertNotNull(result, "Result should not be null");
        assertTrue(result.toString().contains("1"), "Result should contain SQL return value");
    }

    // ---------- 6. Invalid query handling ----------
    @Test
    void invalidQuery_shouldResultInFailedJob() throws Exception {
        Query q = queryRepo.save(new Query("SELEC BAD SQL")); // invalid intentionally
        String executeUrl = baseUrl() + "/execute?queryId=" + q.getId();

        long jobId = ((Number) rest.postForEntity(executeUrl, null, Map.class)
                .getBody().get("jobId")).longValue();

        // Wait long enough for async execution
        Thread.sleep(1500);

        QueryJob job = jobRepo.findById(jobId).orElseThrow();
        assertEquals(QueryJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getError());
    }
}
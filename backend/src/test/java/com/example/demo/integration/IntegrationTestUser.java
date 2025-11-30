package com.example.demo.integration;

import com.example.demo.auth.dto.CreateUserRequest;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.repo.UserRepository;
import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
//a true integration E2E test!

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

class IntegrationTestUser {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;
    @Autowired private QueryRepository queryRepo;
    @Autowired private QueryJobRepository jobRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired ThreadPoolTaskExecutor queryExecutor;
    private String authToken;
    private static final String TEST_USERNAME = "integration-user";
    private static final String TEST_PASSWORD = "integration-password";



    private String baseUrl() {
        return "http://localhost:" + port + "/queries";
    }
    private String adminUsersUrl() {
        return "http://localhost:" + port + "/admin/users";
    }


    @BeforeEach
    void setUp() {
        ensureTestUser();
        authToken = authenticate();
    }

    @AfterEach
    void cleanUp() {
        try{
            queryExecutor.shutdown();
            queryExecutor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);}catch(Exception ignored){};
        jobRepo.deleteAll();
        queryRepo.deleteAll();
        userRepo.deleteAll();
    }

    private void ensureTestUser() {
        userRepo.findByUsername(TEST_USERNAME).orElseGet(() ->
                userRepo.save(new User(TEST_USERNAME, passwordEncoder.encode(TEST_PASSWORD), Role.USER))
        );
    }
    private String authenticate() {
        String loginUrl = "http://localhost:" + port + "/auth/login";
        ResponseEntity<LoginResponse> response = rest.postForEntity(
                loginUrl,
                new LoginRequest(TEST_USERNAME, TEST_PASSWORD),
                LoginResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody().token();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }

    // ---------- 1. POST /queries ----------
    @Test
    void addQuery_shouldPersistQuery() {
        String sql = "SELECT * FROM passengers";
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> entity = new HttpEntity<>(sql, headers);

        ResponseEntity<Map> response = rest.postForEntity(baseUrl(), entity, Map.class);
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

        ResponseEntity<List> response = rest.exchange(
                baseUrl(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                List.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 2);
    }

    // ---------- 3. POST /queries/execute (read query)----------
    @Test
    void executeQuery_shouldCreateJobAndReturnJobId() {
        // Create query first
        Query q = queryRepo.save(new Query("SELECT * FROM passengers WHERE PassengerId = 1"));

        // Submit job
        String url = baseUrl() + "/execute?queryId=" + q.getId();
        ResponseEntity<Map> response = rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("jobId"));

        long jobId = ((Number) body.get("jobId")).longValue();
        QueryJob job = jobRepo.findById(jobId).orElse(null);
        assertNotNull(job);
    }
    // ---------- 4. POST /queries/execute (write query)----------
    @Test
    void userCannotExecuteWriteQuery() {
        Query writeQuery = queryRepo.save(new Query("DELETE from passengers WHERE PassengerId = 1;"));
        String executeUrl = baseUrl() + "/execute?queryId=" + writeQuery.getId();

        ResponseEntity<Map> response = rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("You cannot execute write queries", response.getBody().get("message"));
        assertEquals(0, jobRepo.count());
    }

    // ---------- 5. GET /queries/job/{id} (SUCCEED job state) ----------
    @Test
    void getJob_shouldReturnStatusAndResultEventually() throws Exception {
        // 1. Create and store a simple query
        Query q = queryRepo.save(new Query("SELECT * FROM passengers where PassengerId=2"));


        // 2. Submit job
        String executeUrl = baseUrl() + "/execute?queryId=" + q.getId();
        Map<String, Object> execResponse = rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        ).getBody();
        assertNotNull(execResponse);

        long jobId = ((Number) execResponse.get("jobId")).longValue();

        // 3. Wait for async execution to finish
        Thread.sleep(1000);

        // 4. GET job result
        String statusUrl = baseUrl() + "/job/" + jobId;
        ResponseEntity<Map> response = rest.exchange(
                statusUrl,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);

        // Basic fields
        assertEquals(jobId, ((Number) body.get("jobId")).longValue());
        assertEquals("SUCCEEDED", body.get("status"));
        assertTrue(body.containsKey("result"));


        Object result = body.get("result");
        assertNotNull(result, "Result should not be null");

        // result is actually a JSON string → first cast to String
        String json = (String) result;

        // Parse JSON into List<Map<String,Object>>
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> rows = mapper.readValue(
                json,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        // Should return EXACTLY 1 row
        assertEquals(1, rows.size(), "Query should return exactly one passenger");

        Map<String, Object> row = rows.get(0);

        // Check PassengerId
        assertEquals(2, ((Number) row.get("PassengerId")).intValue());

        assertTrue(row.containsKey("Name"));
        assertTrue(row.containsKey("Age"));
        assertTrue(row.containsKey("Sex"));
    }
    // ---------- 6. GET /queries/job/{id} (RUNNING job state) ----------
    //this test may sometimes execute the query fast enough , that it has an already SUCCEEDED state when it's being checked
    @Test
    void getJob_shouldReturnRunningStatusImmediatelyAfterSubmission() {
        // 1. Store a simple, fast query (fast is good—we WANT to hit the running window)
        Query q = queryRepo.save(new Query("SELECT * FROM passengers WHERE PassengerId = 1"));

        // 2. Submit job
        String executeUrl = baseUrl() + "/execute?queryId=" + q.getId();
        Map<String, Object> execResponse = rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        ).getBody();

        assertNotNull(execResponse);
        long jobId = ((Number) execResponse.get("jobId")).longValue();

        // 3. Immediately query the job state (no sleep!)
        String statusUrl = baseUrl() + "/job/" + jobId;
        ResponseEntity<Map> response = rest.exchange(
                statusUrl,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map body = response.getBody();
        assertNotNull(body);

        // 4. Validate RUNNING state
        assertEquals(jobId, ((Number) body.get("jobId")).longValue());
        assertEquals("RUNNING", body.get("status"));
        assertEquals("Job still processing.", body.get("message"));

        System.out.println("Job is still running: " + body);
    }
    // ---------- 7. GET /queries/job/{id} (FAILED job state) ----------
    @Test
    void getJob_shouldReturnFailedStatusWhenQueryExecutionFails() throws Exception {
        // 1. Create a syntactically valid query that references passengers
        //    but fails at execution time because the column does not exist
        Query q = queryRepo.save(new Query("SELECT NonExistingColumn FROM passengers"));

        // 2. Submit job
        String executeUrl = baseUrl() + "/execute?queryId=" + q.getId();
        Map<String, Object> execResponse = rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        ).getBody();

        assertNotNull(execResponse);
        long jobId = ((Number) execResponse.get("jobId")).longValue();

        // 3. Wait for async execution to finish
        Thread.sleep(1000);

        // 4. Ask for job result
        String statusUrl = baseUrl() + "/job/" + jobId;
        ResponseEntity<Map> response = rest.exchange(
                statusUrl,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map body = response.getBody();
        assertNotNull(body);

        // 5. Verify FAILED state
        assertEquals(jobId, ((Number) body.get("jobId")).longValue());
        assertEquals("FAILED", body.get("status"));

        // 6. Verify error message exists
        assertTrue(body.containsKey("error"));
        assertNotNull(body.get("error"));

    }


    // ---------- 8. POST /admin/users ----------
    @Test
    void userCannotCreateUserThroughAdminEndpoint() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateUserRequest request = new CreateUserRequest("new-user", "password123", Role.USER);

        ResponseEntity<Map> response = rest.postForEntity(
                adminUsersUrl(),
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
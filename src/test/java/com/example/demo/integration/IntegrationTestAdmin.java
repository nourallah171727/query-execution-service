package com.example.demo.integration;


import com.example.demo.auth.dto.CreateUserRequest;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.repo.UserRepository;
import com.example.demo.entity.Query;
import com.example.demo.entity.QueryJob;
import com.example.demo.entity.enums.QueryJobStatus;
import com.example.demo.repository.QueryJobRepository;
import com.example.demo.repository.QueryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
//a true integration E2E test!

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTestAdmin {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;
    @Autowired private QueryRepository queryRepo;
    @Autowired private QueryJobRepository jobRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    private String authToken;
    private static final String ADMIN_USERNAME = "integration-admin";
    private static final String ADMIN_PASSWORD = "integration-admin-password";



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
        jobRepo.deleteAll();
        queryRepo.deleteAll();
        userRepo.deleteAll();
    }

    private void ensureTestUser() {
        userRepo.findByUsername(ADMIN_USERNAME).orElseGet(() ->
                userRepo.save(new User(ADMIN_USERNAME, passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN))
        );
    }
    private String authenticate() {
        String loginUrl = "http://localhost:" + port + "/auth/login";
        ResponseEntity<LoginResponse> response = rest.postForEntity(
                loginUrl,
                new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD),
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
        String sql = "SELECT 1 AS result";
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

    // ---------- 3. POST /queries/execute ----------
    @Test
    void executeQuery_shouldCreateJobAndReturnJobId() {
        // Create query first
        Query q = queryRepo.save(new Query("SELECT 1 AS result"));

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

    // ---------- 4. GET /queries/job/{id} ----------
    @Test
    void getJob_shouldReturnStatusAndResultEventually() throws Exception {
        // 1. Create and store a simple query
        Query q = queryRepo.save(new Query("SELECT 1 AS result"));

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

        long jobId = ((Number) rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        ).getBody().get("jobId")).longValue();

        // Wait long enough for async execution
        Thread.sleep(1500);

        QueryJob job = jobRepo.findById(jobId).orElseThrow();
        assertEquals(QueryJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getError());
    }
    @Test
    void adminCanExecuteWriteQuery() {
        Query writeQuery = queryRepo.save(new Query("INSERT INTO queries(text) VALUES ('admin')"));
        String executeUrl = baseUrl() + "/execute?queryId=" + writeQuery.getId();

        ResponseEntity<Map> response = rest.exchange(
                executeUrl,
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("jobId"));

        long jobId = ((Number) response.getBody().get("jobId")).longValue();
        assertTrue(jobRepo.findById(jobId).isPresent());
    }
    @Test
    void adminCanCreateUserThroughAdminEndpoint() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateUserRequest request = new CreateUserRequest("new-admin-user", "password123", Role.USER);

        ResponseEntity<Map> response = rest.postForEntity(
                adminUsersUrl(),
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        boolean exists = userRepo.findAll().stream()
                .anyMatch(u -> u.getUsername().equals("new-admin-user"));

        assertTrue(exists, "Expected a user with the new-admin-user name");
    }
}
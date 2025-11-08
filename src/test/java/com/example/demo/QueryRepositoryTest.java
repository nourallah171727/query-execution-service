package com.example.demo;

import com.example.demo.repository.QueryRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QueryRepositoryTest {

    @Autowired
    private QueryRepository queryRepository;

    @Test
    void findSqlById_shouldReturnQueryText() {
        // assuming you already have a row in `query` table
        Long existingId = 6L;

        String sql = queryRepository.findSqlById(existingId);

        Assertions.assertNotNull(sql);
        System.out.println("SQL text for id=1 -> " + sql);
    }
}
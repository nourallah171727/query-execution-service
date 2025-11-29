package com.example.demo.unit;

import com.example.demo.entity.Query;
import com.example.demo.repository.QueryRepository;
import com.example.demo.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class QueryServiceUnitTest {
    private QueryRepository repository;
    private QueryService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(QueryRepository.class);
        service = new QueryService(repository);
    }

    // ---------------------- getAllQueries ----------------------

    @Test
    void testGetAllQueries() {
        List<Query> mockQueries = List.of(
                new Query("SELECT * FROM passengers"),
                new Query("SELECT Name FROM passengers")
        );

        when(repository.findAll()).thenReturn(mockQueries);

        List<Query> result = service.getAllQueries();

        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }


    // ---------------------- saveQuery tests ----------------------

    @Test
    void testSaveQuery_invalidSqlSyntax() {
        assertThrows(IllegalArgumentException.class, () ->
                service.saveQuery("SELEC WRONG")
        );
    }


    @Test
    void testSaveQuery_notQueryingPassengersTable() {
        // valid syntax but wrong table
        assertThrows(IllegalArgumentException.class, () ->
                service.saveQuery("SELECT * FROM tickets")
        );
    }


    @Test
    void testSaveQuery_multipleTablesIncludingPassengers() {
        // valid SQL but references multiple tables
        assertThrows(IllegalArgumentException.class, () ->
                service.saveQuery(
                        "SELECT * FROM passengers p JOIN tickets t ON p.id = t.pid"
                )
        );
    }


    @Test
    void testSaveQuery_validQuery() {
        String sql = "SELECT * FROM passengers";

        Query saved = new Query(sql);
        when(repository.save(any(Query.class))).thenReturn(saved);

        Query result = service.saveQuery(sql);

        assertEquals(sql, result.getText());
        verify(repository, times(1)).save(any(Query.class));
    }
}

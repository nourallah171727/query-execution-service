package com.example.demo.unit;

import com.example.demo.util.QueryUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryUtilTest {

    // -------------------- isWriteQuery --------------------

    @Test
    void testIsWriteQuery() {
        assertTrue(QueryUtil.isWriteQuery("UPDATE passengers SET Name = 'Y' WHERE PassengerId = 1"));
        assertTrue(QueryUtil.isWriteQuery("INSERT INTO passengers(Name) VALUES('X')"));
        assertFalse(QueryUtil.isWriteQuery("SELECT * FROM passengers"));
    }


    // -------------------- isValidSql --------------------

    @Test
    void testIsValidSql() {
        assertTrue(QueryUtil.isValidSql("SELECT * FROM passengers WHERE Age > 20"));
        assertFalse(QueryUtil.isValidSql("SELEC WRONG SYNTAX"));
    }


    // -------------------- extractTables --------------------

    @Test
    void testExtractTables() {
        List<String> tables = QueryUtil.extractTables(
                "SELECT p.Name FROM passengers p JOIN tickets t ON p.Ticket = t.Id"
        );

        assertEquals(2, tables.size());
        assertTrue(tables.contains("passengers"));
        assertTrue(tables.contains("tickets"));
    }
    // ---------------- should succeed even if passengers table is called multiple times within same query
    @Test
    void testExtractTables_multiplePassengersReferences() {
        List<String> tables = QueryUtil.extractTables(
                "SELECT p1.Name, p2.Age " +
                        "FROM passengers p1, passengers p2 " +
                        "WHERE p1.id != p2.id"
        );

        assertEquals(1, tables.size());
        assertTrue(tables.contains("passengers"));
    }
}
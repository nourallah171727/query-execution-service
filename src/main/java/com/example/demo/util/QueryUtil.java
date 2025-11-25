package com.example.demo.util;

public class QueryUtil {

    public static boolean isWriteQuery(String sql) {
        String s = sql.trim().toUpperCase();

        return s.startsWith("INSERT")
                || s.startsWith("UPDATE")
                || s.startsWith("DELETE")
                || s.startsWith("DROP")
                || s.startsWith("ALTER")
                || s.startsWith("CREATE");
    }
}
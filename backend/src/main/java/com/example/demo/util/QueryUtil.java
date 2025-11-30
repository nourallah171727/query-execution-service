package com.example.demo.util;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.Collections;
import java.util.List;

public class QueryUtil {
    //ensures that a query is correct in terms of syntax
    public static boolean isValidSql(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    //check if a query is a write Query
    public static boolean isWriteQuery(String sql) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);

            return
                    stmt instanceof Insert ||
                            stmt instanceof Update ||
                            stmt instanceof Delete ||
                            stmt instanceof Merge ||
                            stmt instanceof Truncate ||
                            stmt instanceof CreateTable ||
                            stmt instanceof CreateView ||
                            stmt instanceof Alter ||
                            stmt instanceof Drop;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SQL");
        }
    }
    //extract all tables used within a query
    public static List<String> extractTables(String sql) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);

            TablesNamesFinder finder = new TablesNamesFinder();
            List<String> tables = finder.getTableList(stmt);

            return tables == null ? Collections.emptyList() : tables;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SQL: " + e.getMessage());
        }
    }
}
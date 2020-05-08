package com.pain.red.factory.simple;

import com.pain.red.factory.HbaseSqlParser;
import com.pain.red.factory.HiveSqlParser;
import com.pain.red.factory.KuduSqlParser;
import com.pain.red.factory.SqlParser;

import java.util.HashMap;
import java.util.Map;

public class SqlParserFactory {

    private static final Map<String, SqlParser> sqlParserCache = new HashMap<String, SqlParser>();

    static {
        sqlParserCache.put("kudu", new KuduSqlParser());
        sqlParserCache.put("hbase", new HbaseSqlParser());
        sqlParserCache.put("hive", new HiveSqlParser());
    }

    public static SqlParser createSqlParserV1(String dbType) {
        SqlParser sqlParser = null;

        if ("kudu".equalsIgnoreCase(dbType)) {
            sqlParser = new KuduSqlParser();
        } else if ("hbase".equalsIgnoreCase(dbType)) {
            sqlParser = new HbaseSqlParser();
        } else if ("hive".equalsIgnoreCase(dbType)) {
            sqlParser = new HiveSqlParser();
        }

        return sqlParser;
    }

    public static SqlParser createSqlParserV2(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return null;
        }

        return sqlParserCache.get(dbType.toLowerCase());
    }
}

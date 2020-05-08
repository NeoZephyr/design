package com.pain.red.factory.method;

import java.util.HashMap;
import java.util.Map;

public class SqlParserFactoryMap {
    private static final Map<String, SqlParserFactory> sqlParserFactoryCache = new HashMap<>();

    static {
        sqlParserFactoryCache.put("kudu", new KuduSqlParserFactory());
        sqlParserFactoryCache.put("hbase", new HbaseSqlParserFactory());
        sqlParserFactoryCache.put("hive", new HiveSqlParserFactory());
    }

    public static SqlParserFactory getSqlParserFactory(String dbType) {
        if (dbType == null || dbType.isBlank()) {
            return null;
        }

        return sqlParserFactoryCache.get(dbType.toLowerCase());
    }
}

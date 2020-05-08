package com.pain.red.factory.method;

import com.pain.red.factory.HiveSqlParser;
import com.pain.red.factory.SqlParser;

public class HiveSqlParserFactory implements SqlParserFactory {
    @Override
    public SqlParser createSqlParser() {
        return new HiveSqlParser();
    }
}

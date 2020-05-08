package com.pain.red.factory.method;

import com.pain.red.factory.HbaseSqlParser;
import com.pain.red.factory.SqlParser;

public class HbaseSqlParserFactory implements SqlParserFactory {
    @Override
    public SqlParser createSqlParser() {
        return new HbaseSqlParser();
    }
}

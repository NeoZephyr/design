package com.pain.red.factory.method;

import com.pain.red.factory.KuduSqlParser;
import com.pain.red.factory.SqlParser;

public class KuduSqlParserFactory implements SqlParserFactory {
    @Override
    public SqlParser createSqlParser() {
        return new KuduSqlParser();
    }
}

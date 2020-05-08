package com.pain.red.factory.method;

import com.pain.red.factory.SqlParser;

public interface SqlParserFactory {
    SqlParser createSqlParser();
}

/**
 * 在简单工厂和工厂方法中，类只有一种分类方式。现在，既可以按照配置数据库类型分类，也可以按照 sql 语句存储文件格式分类
 * 再这种情况下，可以让一个工厂负责创建多个不同类 型的对象
 *
 * interface SqlParserFactory {
 *     XmlSqlParser createXmlSqlParser();
 *     JsonSqlParser createJsonSqlParser();
 * }
 */

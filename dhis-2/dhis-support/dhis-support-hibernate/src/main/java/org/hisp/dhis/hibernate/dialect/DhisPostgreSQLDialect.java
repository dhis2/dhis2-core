package org.hisp.dhis.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQLDialect;

public class DhisPostgreSQLDialect extends PostgreSQLDialect{
    
    
    public DhisPostgreSQLDialect() {
        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
        this.registerColumnType(Types.ARRAY, "jsonb[]");
    }

}

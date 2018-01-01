package org.hisp.dhis.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQLDialect;

public class DhisPostgreSQLDialect extends PostgreSQLDialect{
    
    
    public DhisPostgreSQLDialect() {
        super();
        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
    }

}

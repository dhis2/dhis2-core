/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.jdbc.dialect;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.hisp.quick.StatementDialect;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Lars Helge Overland
 */
public class StatementDialectFactoryBean
    implements FactoryBean<StatementDialect>
{
    private static Map<String, StatementDialect> dialectMap;

    static
    {
        dialectMap = new HashMap<>();
        dialectMap.put( "org.hibernate.dialect.MySQLDialect", StatementDialect.MYSQL );
        dialectMap.put( "org.hibernate.dialect.PostgreSQLDialect", StatementDialect.POSTGRESQL );
        dialectMap.put( "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect", StatementDialect.POSTGRESQL );
        dialectMap.put( "org.hibernate.dialect.HSQLDialect", StatementDialect.HSQL );
        dialectMap.put( "org.hibernate.dialect.H2Dialect", StatementDialect.H2 );
        dialectMap.put( "org.hisp.dhis.hibernate.dialect.DhisH2Dialect", StatementDialect.H2 );
    }

    private final String dialectTypeKey;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    public StatementDialectFactoryBean( String dialectTypeKey )
    {
        checkNotNull( dialectTypeKey );
        this.dialectTypeKey = dialectTypeKey;
    }

    private StatementDialect statementDialect;

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public void init()
    {
        statementDialect = dialectMap.get( dialectTypeKey );

        if ( statementDialect == null )
        {
            throw new RuntimeException( "Unsupported dialect: " + dialectTypeKey );
        }
    }

    // -------------------------------------------------------------------------
    // FactoryBean implementation
    // -------------------------------------------------------------------------

    @Override
    public StatementDialect getObject()
    {
        return statementDialect;
    }

    @Override
    public Class<StatementDialect> getObjectType()
    {
        return StatementDialect.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}

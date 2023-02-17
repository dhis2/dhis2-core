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
package org.hisp.dhis.sqlview.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewStore;
import org.hisp.dhis.sqlview.SqlViewType;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Dang Duy Hieu
 */
@Slf4j
@Repository( "org.hisp.dhis.sqlview.SqlViewStore" )
public class HibernateSqlViewStore
    extends HibernateIdentifiableObjectStore<SqlView>
    implements SqlViewStore
{
    private static final Map<SqlViewType, String> TYPE_CREATE_PREFIX_MAP = Map.of(
        SqlViewType.VIEW, "CREATE VIEW ",
        SqlViewType.MATERIALIZED_VIEW, "CREATE MATERIALIZED VIEW " );

    private static final Map<SqlViewType, String> TYPE_DROP_PREFIX_MAP = Map.of(
        SqlViewType.VIEW, "DROP VIEW ",
        SqlViewType.MATERIALIZED_VIEW, "DROP MATERIALIZED VIEW " );

    private final StatementBuilder statementBuilder;

    private final JdbcTemplate readOnlyJdbcTemplate;

    private final SystemSettingManager systemSettingManager;

    public HibernateSqlViewStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService,
        AclService aclService, StatementBuilder statementBuilder,
        @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate readOnlyJdbcTemplate,
        SystemSettingManager systemSettingManager )
    {
        super( sessionFactory, jdbcTemplate, publisher, SqlView.class, currentUserService, aclService, false );

        checkNotNull( statementBuilder );
        checkNotNull( readOnlyJdbcTemplate );
        checkNotNull( systemSettingManager );

        this.statementBuilder = statementBuilder;
        this.readOnlyJdbcTemplate = readOnlyJdbcTemplate;
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // Implementing methods
    // -------------------------------------------------------------------------

    @Override
    public boolean viewTableExists( String viewTableName )
    {
        try
        {
            jdbcTemplate
                .queryForRowSet( "select * from " + statementBuilder.columnQuote( viewTableName ) + " limit 1" );

            return true;
        }
        catch ( BadSqlGrammarException ex )
        {
            return false; // View does not exist
        }
    }

    @Override
    public String createViewTable( SqlView sqlView )
    {
        dropViewTable( sqlView );

        final String sql = TYPE_CREATE_PREFIX_MAP.get( sqlView.getType() )
            + statementBuilder.columnQuote( sqlView.getViewName() ) + " AS " + sqlView.getSqlQuery();

        log.debug( "Create view SQL: " + sql );

        try
        {
            jdbcTemplate.execute( sql );

            return null;
        }
        catch ( BadSqlGrammarException ex )
        {
            return ex.getCause().getMessage();
        }
    }

    @Override
    public void populateSqlViewGrid( Grid grid, String sql )
    {
        SqlRowSet rs = readOnlyJdbcTemplate.queryForRowSet( sql );

        int maxLimit = systemSettingManager.getIntSetting( SettingKey.SQL_VIEW_MAX_LIMIT );

        log.debug( "Get view SQL: " + sql + ", max limit: " + maxLimit );

        grid.addHeaders( rs );
        grid.addRows( rs, maxLimit );
    }

    @Override
    public String testSqlGrammar( String sql )
    {
        String viewName = SqlView.PREFIX_VIEWNAME + System.currentTimeMillis();

        sql = "CREATE VIEW " + viewName + " AS " + sql;

        log.debug( "Test view SQL: " + sql );

        try
        {
            jdbcTemplate.execute( sql );

            jdbcTemplate.execute( "DROP VIEW IF EXISTS " + viewName );
        }
        catch ( BadSqlGrammarException | UncategorizedSQLException ex )
        {
            return ex.getCause().getMessage();
        }

        return null;
    }

    @Override
    public void dropViewTable( SqlView sqlView )
    {
        String viewName = sqlView.getViewName();

        try
        {
            final String sql = TYPE_DROP_PREFIX_MAP.get( sqlView.getType() ) + " IF EXISTS "
                + statementBuilder.columnQuote( viewName );

            log.debug( "Drop view SQL: " + sql );

            jdbcTemplate.update( sql );
        }
        catch ( Exception ex )
        {
            log.warn( "Could not drop view: " + viewName, ex );
        }
    }

    @Override
    public boolean refreshMaterializedView( SqlView sqlView )
    {
        final String sql = "REFRESH MATERIALIZED VIEW " + sqlView.getViewName();

        log.debug( "Refresh materialized view: " + sql );

        try
        {
            jdbcTemplate.update( sql );

            return true;
        }
        catch ( Exception ex )
        {
            log.warn( "Could not refresh materialized view: " + sqlView.getViewName(), ex );

            return false;
        }
    }
}

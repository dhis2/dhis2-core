/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataitem.query;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.skipValueType;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of
 * ProgramIndicator objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class ProgramIndicatorQuery implements DataItemQuery
{
    private static final String COMMON_COLUMNS = "programindicator.\"name\", programindicator.uid, programindicator.code,"
        + " program.\"name\" AS program_name, program.uid AS program_uid, program.sharing AS program_sharing,"
        + " programindicator.sharing AS programindicator_sharing";

    private static final String COMMON_UIDS = "program.uid, programindicator.uid";

    private static final String JOINS = "JOIN program ON program.programid = programindicator.programid";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProgramIndicatorQuery( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    public List<DataItem> find( final MapSqlParameterSource paramsMap )
    {
        final List<DataItem> dataItems = new ArrayList<>();

        // Very specific case, for Indicator objects, needed to handle filter by
        // value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        // It returns an empty instead.
        if ( skipValueType( NUMBER, paramsMap ) )
        {
            return dataItems;
        }

        final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet(
            getProgramIndicatorQuery( paramsMap ), paramsMap );

        while ( rowSet.next() )
        {
            final DataItem viewItem = new DataItem();

            final String programName = trimToEmpty( rowSet.getString( "program_name" ) );
            final String displayProgramName = defaultIfBlank( trimToEmpty( rowSet.getString( "p_i18n_name" ) ),
                programName );

            final String name = trimToEmpty( rowSet.getString( "name" ) );
            final String displayName = defaultIfBlank( trimToEmpty( rowSet.getString( "pi_i18n_name" ) ),
                trimToEmpty( rowSet.getString( "name" ) ) );

            viewItem.setName( name );
            viewItem.setDisplayName( displayName );
            viewItem.setProgramName( programName );
            viewItem.setProgramDisplayName( displayProgramName );
            viewItem.setProgramId( rowSet.getString( "program_uid" ) );
            viewItem.setId( rowSet.getString( "uid" ) );
            viewItem.setCode( rowSet.getString( "code" ) );
            viewItem.setDimensionItemType( PROGRAM_INDICATOR.name() );

            // Specific case where we have to force a vale type. Program
            // Indicators don't have a value type but they always evaluate to
            // numbers.
            viewItem.setValueType( NUMBER.name() );
            viewItem.setSimplifiedValueType( NUMBER.name() );

            dataItems.add( viewItem );
        }

        return dataItems;
    }

    @Override
    public int count( final MapSqlParameterSource paramsMap )
    {
        // Very specific case, for Indicator objects, needed to handle filter by
        // value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        // It returns ZERO.
        if ( skipValueType( NUMBER, paramsMap ) )
        {
            return 0;
        }

        final StringBuilder sql = new StringBuilder();

        sql.append( "SELECT COUNT(*) FROM (" )
            .append( getProgramIndicatorQuery( paramsMap ).replace( maxLimit( paramsMap ), EMPTY ) )
            .append( ") t" );

        return namedParameterJdbcTemplate.queryForObject( sql.toString(), paramsMap, Integer.class );
    }

    @Override
    public Class<? extends BaseIdentifiableObject> getRootEntity()
    {
        return QueryableDataItem.PROGRAM_INDICATOR.getEntity();
    }

    private String getProgramIndicatorQuery( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        // Creating a temp translated table to be queried.
        sql.append( "SELECT * FROM (" );

        if ( hasStringPresence( paramsMap, LOCALE ) )
        {
            // Selecting only rows that contains both programs and program
            // indicators translations at the same time.
            sql.append( selectRowsContainingBothTranslatedNames( false ) )

                .append( " UNION" )

                // Selecting only rows with translated programs.
                .append( selectRowsContainingOnlyTranslatedProgramNames( false ) )

                .append( " UNION" )

                // Selecting only rows with translated program indicators.
                .append( selectRowsContainingOnlyTranslatedProgramIndicatorNames( false ) )

                .append( " UNION" )

                // Selecting ALL rows, not taking into consideration
                // translations.
                .append( selectAllRowsIgnoringAnyTranslation() )

                /// AND excluding ALL translated rows previously selected
                /// (translated programs and translated program indicators).
                .append( " WHERE (" + COMMON_UIDS + ") NOT IN (" )

                .append( selectRowsContainingBothTranslatedNames( true ) )

                .append( " UNION" )

                // Selecting only rows with translated programs.
                .append( selectRowsContainingOnlyTranslatedProgramNames( true ) )

                .append( " UNION" )

                // Selecting only rows with translated program indicators.
                .append( selectRowsContainingOnlyTranslatedProgramIndicatorNames( true ) )

                // Closing NOT IN exclusions.
                .append( ")" );
        }
        else
        {
            // Retrieving all rows ignoring translation as no locale is defined.
            sql.append( selectAllRowsIgnoringAnyTranslation() );
        }

        sql.append(
            " GROUP BY program.\"name\", programindicator.\"name\", " + COMMON_UIDS
                + ", programindicator.code, p_i18n_name, pi_i18n_name, program_sharing, programindicator_sharing" );

        // Closing the temp table.
        sql.append( " ) t" );

        sql.append( " WHERE" );

        // Applying filters, ordering and limits.

        // Mandatory filters. They do not respect the root junction filtering.
        sql.append( always( sharingConditions( "t.program_sharing",
            "t.programindicator_sharing", paramsMap ) ) );

        // Optional filters, based on the current root junction.
        final OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder( paramsMap );
        optionalFilters.append( ifSet( displayFiltering( "t.p_i18n_name", "t.pi_i18n_name", paramsMap ) ) );
        optionalFilters.append( ifSet( nameFiltering( "t.program_name", "t.name", paramsMap ) ) );
        optionalFilters.append( ifSet( programIdFiltering( "t.program_uid", paramsMap ) ) );
        optionalFilters.append( ifSet( uidFiltering( "t.uid", paramsMap ) ) );
        sql.append( ifAny( optionalFilters.toString() ) );

        sql.append(
            ifSet( ordering( "t.pi_i18n_name, t.uid", "t.name, t.uid", paramsMap ) ) );
        sql.append( ifSet( maxLimit( paramsMap ) ) );

        final String fullStatement = sql.toString();

        log.trace( "Full SQL: " + fullStatement );

        return fullStatement;
    }

    private String selectRowsContainingBothTranslatedNames( final boolean onlyUidColumns )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumns )
        {
            sql.append( " SELECT " + COMMON_UIDS );
        }
        else
        {
            sql.append( " SELECT " + COMMON_COLUMNS )
                .append( ", p_displayname.value AS p_i18n_name, pi_displayname.value AS pi_i18n_name" );
        }

        sql.append( " FROM programindicator " )
            .append( JOINS )
            .append(
                " JOIN jsonb_to_recordset(program.translations) AS p_displayname(value TEXT, locale TEXT, property TEXT) ON p_displayname.locale = :"
                    + LOCALE + " AND p_displayname.property = 'NAME'" )
            .append(
                " JOIN jsonb_to_recordset(programindicator.translations) AS pi_displayname(value TEXT, locale TEXT, property TEXT) ON pi_displayname.locale = :"
                    + LOCALE + " AND pi_displayname.property = 'NAME'" );

        return sql.toString();
    }

    private String selectRowsContainingOnlyTranslatedProgramNames( final boolean onlyUidColumns )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumns )
        {
            sql.append( " SELECT " + COMMON_UIDS );
        }
        else
        {
            sql.append( " SELECT " + COMMON_COLUMNS )
                .append( ", p_displayname.value AS p_i18n_name, programindicator.\"name\" AS pi_i18n_name" );
        }

        sql.append( " FROM programindicator " )
            .append( JOINS )
            .append(
                " JOIN jsonb_to_recordset(program.translations) AS p_displayname(value TEXT, locale TEXT, property TEXT) ON p_displayname.locale = :"
                    + LOCALE + " AND p_displayname.property = 'NAME'" )
            .append( " WHERE (" + COMMON_UIDS + ")" )
            .append( " NOT IN (" )

            // Exclude rows already fully translated.
            .append( selectRowsContainingBothTranslatedNames( true ) )
            .append( ")" );

        return sql.toString();
    }

    private String selectRowsContainingOnlyTranslatedProgramIndicatorNames( final boolean onlyUidColumns )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumns )
        {
            sql.append( " SELECT " + COMMON_UIDS );
        }
        else
        {
            sql.append( " SELECT " + COMMON_COLUMNS )
                .append( ", program.\"name\" AS p_i18n_name, pi_displayname.value AS pi_i18n_name" );
        }

        sql.append( " FROM programindicator " )
            .append( JOINS )
            .append(
                " JOIN jsonb_to_recordset(programindicator.translations) AS pi_displayname(value TEXT, locale TEXT, property TEXT) ON pi_displayname.locale = :"
                    + LOCALE + " AND pi_displayname.property = 'NAME'" )
            .append( " WHERE (" + COMMON_UIDS + ")" )
            .append( " NOT IN (" )

            // Exclude rows already fully translated.
            .append( selectRowsContainingBothTranslatedNames( true ) )
            .append( ")" );

        return sql.toString();
    }

    private String selectAllRowsIgnoringAnyTranslation()
    {
        return new StringBuilder()
            .append( " SELECT " + COMMON_COLUMNS )
            .append( ", program.\"name\" AS p_i18n_name, programindicator.\"name\" AS pi_i18n_name" )
            .append( " FROM programindicator " )
            .append( JOINS ).toString();
    }
}

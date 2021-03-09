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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.DataItem.builder;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayShortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.shortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.skipValueType;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringNonBlankPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
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
    private static final String COMMON_COLUMNS = "programindicator.name, programindicator.uid, programindicator.code,"
        + " program.name as program_name, program.uid as program_uid, program.sharing as program_sharing,"
        + " programindicator.sharing as programindicator_sharing,"
        + " programindicator.shortname, program.shortname as program_shortname";

    private static final String COMMON_UIDS = "program.uid, programindicator.uid";

    private static final String JOINS = "join program on program.programid = programindicator.programid";

    private static final String SPACED_FROM_PROGRAM_INDICATOR = " from programindicator ";

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
            final String name = trimToEmpty( rowSet.getString( "name" ) );
            final String displayName = defaultIfBlank( trimToEmpty( rowSet.getString( "i18n_name" ) ),
                name );
            final String shortName = trimToNull( rowSet.getString( "shortname" ) );
            final String displayShortName = defaultIfBlank( trimToNull( rowSet.getString( "i18n_shortname" ) ),
                shortName );

            // Specific case where we have to force a vale type. Program
            // Indicators don't have a value type but they always evaluate to
            // numbers.
            dataItems.add( builder().name( name ).displayName( displayName ).id( rowSet.getString( "uid" ) )
                .shortName( shortName ).displayShortName( displayShortName )
                .code( rowSet.getString( "code" ) ).dimensionItemType( PROGRAM_INDICATOR )
                .programId( rowSet.getString( "program_uid" ) ).valueType( NUMBER ).build() );
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

        sql.append( SPACED_SELECT + "count(*) from (" )
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
        sql.append( SPACED_SELECT + "* from (" );

        if ( hasStringNonBlankPresence( paramsMap, LOCALE ) )
        {
            // Selecting translated names.
            sql.append( selectRowsContainingTranslatedName() );
        }
        else
        {
            // Retrieving all rows ignoring translation as no locale is defined.
            sql.append( selectAllRowsIgnoringAnyTranslation() );
        }

        sql.append(
            " group by program.name, program.shortname, programindicator.name, " + COMMON_UIDS
                + ", programindicator.code, p_i18n_name, i18n_name, program_sharing, programindicator_sharing, programindicator.shortname,"
                + " i18n_shortname, p_i18n_shortname" );

        // Closing the temp table.
        sql.append( " ) t" );

        sql.append( SPACED_WHERE );

        // Applying filters, ordering and limits.

        // Mandatory filters. They do not respect the root junction filtering.
        sql.append( always( sharingConditions( "t.program_sharing",
            "t.programindicator_sharing", READ_ACCESS, paramsMap ) ) );

        // Optional filters, based on the current root junction.
        final OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder( paramsMap );
        optionalFilters.append( ifSet( displayNameFiltering( "t.i18n_name", paramsMap ) ) );
        optionalFilters.append( ifSet( displayShortNameFiltering( "t.i18n_shortname", paramsMap ) ) );
        optionalFilters.append( ifSet( nameFiltering( "t.name", paramsMap ) ) );
        optionalFilters.append( ifSet( shortNameFiltering( "t.shortname", paramsMap ) ) );
        optionalFilters.append( ifSet( programIdFiltering( "t.program_uid", paramsMap ) ) );
        optionalFilters.append( ifSet( uidFiltering( "t.uid", paramsMap ) ) );
        sql.append( ifAny( optionalFilters.toString() ) );

        final String identifiableStatement = identifiableTokenFiltering( "t.uid", "t.code", "t.i18n_name",
            null, paramsMap );

        if ( isNotBlank( identifiableStatement ) )
        {
            sql.append( rootJunction( paramsMap ) );
            sql.append( identifiableStatement );
        }

        sql.append( ifSet( ordering( "t.i18n_name, t.uid",
            "t.name, t.uid", "t.i18n_shortname, t.uid", "t.shortname, t.uid", paramsMap ) ) );
        sql.append( ifSet( maxLimit( paramsMap ) ) );

        final String fullStatement = sql.toString();

        log.trace( "Full SQL: " + fullStatement );

        return fullStatement;
    }

    private String selectRowsContainingTranslatedName()
    {
        return new StringBuilder()
            .append( SPACED_SELECT + COMMON_COLUMNS )
            .append( translationNamesColumnsFor( "programindicator", true ) )
            .append( SPACED_FROM_PROGRAM_INDICATOR )
            .append( JOINS )
            .append( translationNamesJoinsOn( "programindicator", true ) ).toString();
    }

    private String selectAllRowsIgnoringAnyTranslation()
    {
        return new StringBuilder()
            .append( SPACED_SELECT + COMMON_COLUMNS )
            .append( ", program.name as p_i18n_name, programindicator.name as i18n_name," +
                " program.shortname as p_i18n_shortname, programindicator.shortname as i18n_shortname" )
            .append( SPACED_FROM_PROGRAM_INDICATOR )
            .append( JOINS ).toString();
    }
}

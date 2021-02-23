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
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.ValueType.fromString;
import static org.hisp.dhis.dataitem.DataItem.builder;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.valueTypeFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringNonBlankPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_UNION;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;
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
 * ProgramDataElementDimensionItem objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class ProgramStageDataElementQuery implements DataItemQuery
{
    private static final String COMMON_COLUMNS = "program.\"name\" as program_name, program.uid as program_uid,"
        + " dataelement.uid, dataelement.\"name\", dataelement.valuetype, dataelement.code,"
        + " dataelement.sharing as dataelement_sharing, program.sharing as program_sharing";

    private static final String COMMON_UIDS = "program.uid, dataelement.uid";

    private static final String JOINS = "join programstagedataelement on programstagedataelement.dataelementid = dataelement.dataelementid"
        + " join programstage on programstagedataelement.programstageid = programstage.programstageid"
        + " join program on program.programid = programstage.programid";

    private static final String SPACED_FROM_DATA_ELEMENT = " from dataelement ";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProgramStageDataElementQuery( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    public List<DataItem> find( final MapSqlParameterSource paramsMap )
    {
        final List<DataItem> dataItems = new ArrayList<>();

        final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet( getProgramDataElementQuery( paramsMap ),
            paramsMap );

        while ( rowSet.next() )
        {
            final ValueType valueType = fromString( rowSet.getString( "valuetype" ) );

            final String name = trimToEmpty(
                rowSet.getString( "program_name" ) ) + SPACE + trimToEmpty( rowSet.getString( "name" ) );
            final String displayName = defaultIfBlank( trimToEmpty( rowSet.getString( "p_i18n_name" ) ),
                rowSet.getString( "program_name" ) ) + SPACE
                + defaultIfBlank( trimToEmpty( rowSet.getString( "de_i18n_name" ) ),
                    trimToEmpty( rowSet.getString( "name" ) ) );
            final String uid = rowSet.getString( "program_uid" ) + "." + rowSet.getString( "uid" );

            dataItems.add( builder().name( name ).displayName( displayName ).id( uid )
                .code( rowSet.getString( "code" ) ).dimensionItemType( PROGRAM_DATA_ELEMENT )
                .programId( rowSet.getString( "program_uid" ) ).valueType( valueType ).build() );
        }

        return dataItems;
    }

    @Override
    public int count( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        sql.append( SPACED_SELECT + "count(*) from (" )
            .append( getProgramDataElementQuery( paramsMap ).replace( maxLimit( paramsMap ), EMPTY ) )
            .append( ") t" );

        return namedParameterJdbcTemplate.queryForObject( sql.toString(), paramsMap, Integer.class );
    }

    @Override
    public Class<? extends BaseIdentifiableObject> getRootEntity()
    {
        return QueryableDataItem.PROGRAM_DATA_ELEMENT.getEntity();
    }

    private String getProgramDataElementQuery( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        // Creating a temp translated table to be queried.
        sql.append( SPACED_SELECT + "* from (" );

        if ( hasStringNonBlankPresence( paramsMap, LOCALE ) )
        {
            // Selecting only rows that contains both programs and data elements
            // translations at the same time.
            sql.append( selectRowsContainingBothTranslatedNames( false ) )

                .append( SPACED_UNION )

                // Selecting only rows with translated programs.
                .append( selectRowsContainingOnlyTranslatedProgramNames( false ) )

                .append( SPACED_UNION )

                // Selecting only rows with translated data elements.
                .append( selectRowsContainingOnlyTranslatedDataElementNames( false ) )

                .append( SPACED_UNION )

                // Selecting ALL rows, not taking into consideration
                // translations.
                .append( selectAllRowsIgnoringAnyTranslation() )

                /// AND excluding ALL translated rows previously selected
                /// (translated programs and translated data elements).
                .append( SPACED_WHERE + "(" + COMMON_UIDS + ") not in (" )

                .append( selectRowsContainingBothTranslatedNames( true ) )

                .append( SPACED_UNION )

                // Selecting only rows with translated programs.
                .append( selectRowsContainingOnlyTranslatedProgramNames( true ) )

                .append( SPACED_UNION )

                // Selecting only rows with translated data elements.
                .append( selectRowsContainingOnlyTranslatedDataElementNames( true ) )

                // Closing NOT IN exclusions.
                .append( ")" );
        }
        else
        {
            // Retrieving all rows ignoring translation as no locale is defined.
            sql.append( selectAllRowsIgnoringAnyTranslation() );
        }

        sql.append(
            " group by program.\"name\", dataelement.\"name\", " + COMMON_UIDS
                + ", dataelement.valuetype, dataelement.code, p_i18n_name, de_i18n_name, program_sharing, dataelement_sharing" );

        // Closing the temp table.
        sql.append( " ) t" );

        sql.append( SPACED_WHERE );

        // Applying filters, ordering and limits.

        // Mandatory filters. They do not respect the root junction filtering.
        sql.append( always( sharingConditions( "t.program_sharing",
            "t.dataelement_sharing", paramsMap ) ) );
        sql.append( " and" );
        sql.append( ifSet( valueTypeFiltering( "t.valuetype", paramsMap ) ) );

        // Optional filters, based on the current root junction.
        final OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder( paramsMap );
        optionalFilters.append( ifSet( displayFiltering( "t.p_i18n_name", "t.de_i18n_name", paramsMap ) ) );
        optionalFilters.append( ifSet( nameFiltering( "t.program_name", "t.name", paramsMap ) ) );
        optionalFilters.append( ifSet( programIdFiltering( "t.program_uid", paramsMap ) ) );
        optionalFilters.append( ifSet( uidFiltering( "t.uid", paramsMap ) ) );
        sql.append( ifAny( optionalFilters.toString() ) );

        final String identifiableStatement = identifiableTokenFiltering( "t.uid", "t.code", "t.de_i18n_name",
            "t.p_i18n_name", paramsMap );

        if ( isNotBlank( identifiableStatement ) )
        {
            sql.append( rootJunction( paramsMap ) );
            sql.append( identifiableStatement );
        }

        sql.append( ifSet( ordering( "t.p_i18n_name, t.de_i18n_name, t.uid",
            "t.program_name, t.name, t.uid", paramsMap ) ) );
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
            sql.append( SPACED_SELECT + COMMON_UIDS );
        }
        else
        {
            sql.append( SPACED_SELECT + COMMON_COLUMNS )
                .append( ", p_displayname.value as p_i18n_name, de_displayname.value as de_i18n_name" );
        }

        sql.append( SPACED_FROM_DATA_ELEMENT )
            .append( JOINS )
            .append(
                " join jsonb_to_recordset(program.translations) as p_displayname(value TEXT, locale TEXT, property TEXT) on p_displayname.locale = :"
                    + LOCALE + " and p_displayname.property = 'NAME'" )
            .append(
                " join jsonb_to_recordset(dataelement.translations) as de_displayname(value TEXT, locale TEXT, property TEXT) on de_displayname.locale = :"
                    + LOCALE + " and de_displayname.property = 'NAME'" );

        return sql.toString();
    }

    private String selectRowsContainingOnlyTranslatedProgramNames( final boolean onlyUidColumns )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumns )
        {
            sql.append( SPACED_SELECT + COMMON_UIDS );
        }
        else
        {
            sql.append( SPACED_SELECT + COMMON_COLUMNS )
                .append( ", p_displayname.value as p_i18n_name, dataelement.\"name\" as de_i18n_name" );
        }

        sql.append( SPACED_FROM_DATA_ELEMENT )
            .append( JOINS )
            .append(
                " join jsonb_to_recordset(program.translations) as p_displayname(value TEXT, locale TEXT, property TEXT) on p_displayname.locale = :"
                    + LOCALE + " and p_displayname.property = 'NAME'" )
            .append( SPACED_WHERE + "(" + COMMON_UIDS + ")" )
            .append( " not in (" )

            // Exclude rows already fully translated.
            .append( selectRowsContainingBothTranslatedNames( true ) )
            .append( ")" );

        return sql.toString();
    }

    private String selectRowsContainingOnlyTranslatedDataElementNames( final boolean onlyUidColumns )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumns )
        {
            sql.append( SPACED_SELECT + COMMON_UIDS );
        }
        else
        {
            sql.append( SPACED_SELECT + COMMON_COLUMNS )
                .append( ", program.\"name\" as p_i18n_name, de_displayname.value as de_i18n_name" );
        }

        sql.append( SPACED_FROM_DATA_ELEMENT )
            .append( JOINS )
            .append(
                " join jsonb_to_recordset(dataelement.translations) as de_displayname(value TEXT, locale TEXT, property TEXT) on de_displayname.locale = :"
                    + LOCALE + " and de_displayname.property = 'NAME'" )
            .append( SPACED_WHERE + "(" + COMMON_UIDS + ")" )
            .append( " not in (" )

            // Exclude rows already fully translated.
            .append( selectRowsContainingBothTranslatedNames( true ) )
            .append( ")" );

        return sql.toString();
    }

    private String selectAllRowsIgnoringAnyTranslation()
    {
        return new StringBuilder()
            .append( SPACED_SELECT + COMMON_COLUMNS )
            .append( ", program.\"name\" as p_i18n_name, dataelement.\"name\" as de_i18n_name" )
            .append( SPACED_FROM_DATA_ELEMENT )
            .append( JOINS ).toString();
    }
}

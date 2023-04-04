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
package org.hisp.dhis.dataitem.query;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.ValueType.getAggregatables;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayShortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.shortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.valueTypeFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.PROGRAM_ID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of
 * DataElement objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class DataElementQuery implements DataItemQuery
{
    private static final String COMMON_COLUMNS = "cast (null as text) as program_name, cast (null as text) as program_uid,"
        + " cast (null as text) as program_shortname, dataelement.uid as item_uid, dataelement.name as item_name,"
        + " dataelement.shortname as item_shortname, dataelement.valuetype as item_valuetype,"
        + " dataelement.code as item_code, dataelement.sharing as item_sharing, dataelement.domaintype as item_domaintype,"
        + " cast ('DATA_ELEMENT' as text) as item_type,"
        + " cast (null as text) as expression";

    @Override
    public String getStatement( MapSqlParameterSource paramsMap )
    {
        StringBuilder sql = new StringBuilder();

        sql.append( "(" );

        // Creating a temp translated table to be queried.
        sql.append( SPACED_SELECT + "* from (" );

        if ( hasNonBlankStringPresence( paramsMap, LOCALE ) )
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
            " group by item_name, item_uid, item_valuetype, item_code, item_domaintype, item_sharing, item_shortname,"
                + " i18n_first_name, i18n_first_shortname, i18n_second_name, i18n_second_shortname" );

        // Closing the temp table.
        sql.append( " ) t" );

        sql.append( SPACED_WHERE );

        // Applying filters, ordering and limits.

        // Mandatory filters. They do not respect the root junction filtering.
        sql.append( always( sharingConditions( "t.item_sharing", READ_ACCESS, paramsMap ) ) );
        sql.append( " and " );
        // ONLY aggregates
        sql.append( always( "t.item_domaintype = 'AGGREGATE'" ) );

        // Optional filters, based on the current root junction.
        OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder( paramsMap );
        Set<String> aggregatableTypes = getAggregatables().stream().map( type -> type.name() ).collect( toSet() );

        // This condition is needed to enable value types filtering only when
        // they are actually specified as filters. Otherwise we should only
        // consider the domainType = 'AGGREGATE'. Very specific to DataElements.
        if ( paramsMap != null && paramsMap.hasValue( VALUE_TYPES )
            && paramsMap.getValue( VALUE_TYPES ) != null
            && !((Set) paramsMap.getValue( VALUE_TYPES )).containsAll( aggregatableTypes ) )
        {
            optionalFilters.append( ifSet( valueTypeFiltering( "t.item_valuetype", paramsMap ) ) );
        }

        optionalFilters.append( ifSet( displayNameFiltering( "t.i18n_first_name", paramsMap ) ) );
        optionalFilters.append( ifSet( displayShortNameFiltering( "t.i18n_first_shortname", paramsMap ) ) );
        optionalFilters.append( ifSet( nameFiltering( "t.item_name", paramsMap ) ) );
        optionalFilters.append( ifSet( shortNameFiltering( "t.item_shortname", paramsMap ) ) );
        optionalFilters.append( ifSet( uidFiltering( "t.item_uid", paramsMap ) ) );
        sql.append( ifAny( optionalFilters.toString() ) );

        String identifiableStatement = identifiableTokenFiltering( "t.item_uid", "t.item_code",
            "t.i18n_first_name",
            null, paramsMap );

        if ( isNotBlank( identifiableStatement ) )
        {
            sql.append( rootJunction( paramsMap ) );
            sql.append( identifiableStatement );
        }

        sql.append( ifSet( ordering( "t.i18n_first_name, t.i18n_second_name, t.item_uid", "t.item_name, t.item_uid",
            "t.i18n_first_shortname, t.i18n_second_shortname, t.item_uid", "t.item_shortname, t.item_uid",
            paramsMap ) ) );
        sql.append( ifSet( maxLimit( paramsMap ) ) );
        sql.append( ")" );

        String fullStatement = sql.toString();

        log.trace( "Full SQL: " + fullStatement );

        return fullStatement;
    }

    /**
     * If we have a program id filter, we should not return any data element
     * because data elements don't have programs directly associated with. Hence
     * we skip this query.
     *
     * @param paramsMap
     * @return true if rules are matched.
     */
    @Override
    public boolean matchQueryRules( MapSqlParameterSource paramsMap )
    {
        return !hasNonBlankStringPresence( paramsMap, PROGRAM_ID );
    }

    @Override
    public Class<? extends BaseIdentifiableObject> getRootEntity()
    {
        return QueryableDataItem.DATA_ELEMENT.getEntity();
    }

    private String selectRowsContainingTranslatedName()
    {
        StringBuilder sql = new StringBuilder();

        sql.append( SPACED_SELECT + COMMON_COLUMNS )
            .append( translationNamesColumnsFor( "dataelement" ) );

        sql.append( " from dataelement " )
            .append( translationNamesJoinsOn( "dataelement" ) );

        return sql.toString();
    }

    private String selectAllRowsIgnoringAnyTranslation()
    {
        return new StringBuilder()
            .append( SPACED_SELECT + COMMON_COLUMNS )
            .append( ", dataelement.name as i18n_first_name, cast (null as text) as i18n_second_name" )
            .append(
                ", dataelement.shortname as i18n_first_shortname, cast (null as text) as i18n_second_shortname" )
            .append( " from dataelement " ).toString();
    }
}

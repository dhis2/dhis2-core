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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsPagingAndSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.query.items.AndCondition;
import org.hisp.dhis.analytics.tei.query.items.EventDataValueCondition;
import org.hisp.dhis.analytics.tei.query.items.OrCondition;
import org.hisp.dhis.analytics.tei.query.items.Table;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;

@RequiredArgsConstructor( staticName = "of" )
public class QueryContext
{
    @Getter
    private final TeiQueryParams teiQueryParams;

    @Delegate
    private final ParameterManager parameterManager = new ParameterManager();

    public String getMainTableName()
    {
        return ANALYTICS_TEI + getTeTTableSuffix();
    }

    public String getTeTTableSuffix()
    {
        return teiQueryParams.getTrackedEntityType().getUid();
    }

    public Query getQuery()
    {
        return Query.builder()
            .select( getSelect() )
            .from( getFrom() )
            .where( getWhere() )
            .order( getOrder() )
            .limit( getLimit() )
            .build();
    }

    private LimitOffset getLimit()
    {
        AnalyticsPagingAndSortingParams pagingAndSortingParams = teiQueryParams.getCommonParams()
            .getPagingAndSortingParams();
        return LimitOffset.of(
            pagingAndSortingParams.getPageSize(),
            pagingAndSortingParams.getPageSize() * pagingAndSortingParams.getPage() );
    }

    private Order getOrder()
    {
        // TODO: derive order depending on params
        return Order.ofOrder( StringUtils.EMPTY );
    }

    private Select getSelect()
    {
        // ** TODO: add more static fields if needed
        Stream<Renderable> staticFields = Stream.of( getFieldWithAlias( "trackedentityinstanceid" ),
            getFieldWithAlias( "trackedentityinstanceuid" ),
            getFieldWithAlias( "enrollments" ) );

        // TET and program attribute fields
        Stream<Renderable> attributes = Stream.concat(
            // Tracked entity Type attributes
            teiQueryParams.getTrackedEntityType().getTrackedEntityTypeAttributes().stream(),
            // Program attributes
            teiQueryParams.getCommonParams().getPrograms().stream()
                .map( Program::getProgramAttributes )
                .flatMap( Collection::stream )
                .map( ProgramTrackedEntityAttribute::getAttribute ) )
            .map( BaseIdentifiableObject::getUid )
            // to remove overlapping attributes
            .distinct()
            .map( this::getFieldWithAlias );

        return Select.of(
            Stream.concat( staticFields, attributes )
                .collect( Collectors.toList() ) );
    }

    private Renderable getFieldWithAlias( String field )
    {
        return Field.of( TEI_ALIAS, () -> field, field );
    }

    private From getFrom()
    {
        return From.of( getMainTable(), getJoinTablesAndConditions() );
    }

    public Renderable getMainTable()
    {
        return Table.ofStrings( getMainTableName(), TEI_ALIAS );
    }

    private List<Pair<Renderable, Renderable>> getJoinTablesAndConditions()
    {
        // TODO: implement logic for join when needed, i.e.
        // when sorting on a data element is present
        return Collections.emptyList();
    }

    private Where getWhere()
    {
        // conditions on programs (is enrolled in program)
        Stream<Renderable> programConditions = getTeiQueryParams()
            .getCommonParams()
            .getPrograms()
            .stream()
            .map( program -> () -> TEI_ALIAS + "." + program.getUid() );

        // conditions on filters/dimensions
        Stream<Renderable> dimensionConditions = getTeiQueryParams().getCommonParams().getDimensionIdentifiers()
            .stream()
            .map( this::toConditions )
            .map( OrCondition::of );

        return Where.of(
            AndCondition.of(
                Stream.concat(
                    programConditions,
                    dimensionConditions )
                    .collect( Collectors.toList() ) ) );
    }

    private List<Renderable> toConditions(
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        return dimensionIdentifiers.stream()
            .filter( di -> di.getDimension().hasRestrictions() )
            .collect( Collectors.groupingBy( di -> di.getDimension().getDimensionParamObjectType() ) )
            .entrySet().stream()
            .map( entry -> toCondition( entry.getKey(), entry.getValue() ) )
            .collect( Collectors.toList() );
    }

    private Renderable toCondition( DimensionParamObjectType type,
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        // TODO: depending on the type, we should build a proper condition
        if ( type == DimensionParamObjectType.DATA_ELEMENT )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> EventDataValueCondition.of( dimensionIdentifier, this ) )
                    .collect( Collectors.toList() ) );
        }
        return () -> StringUtils.EMPTY;
    }

    private static class ParameterManager
    {
        private int parameterIndex = 0;

        @Getter
        private final Map<Integer, Object> parametersByPlaceHolder = new HashMap<>();

        public int bindParamAndGetIndex( Object param )
        {
            parameterIndex++;
            parametersByPlaceHolder.put( parameterIndex, param );
            return parameterIndex;
        }
    }
}

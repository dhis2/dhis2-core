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

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ANALYTICS_TEI_TEI;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_TEI_DEFAULT_ALIAS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsPagingAndSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
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

    private final ParameterManager parameterManager = new ParameterManager();

    public String getMainTableName()
    {
        return ANALYTICS_TEI_TEI + getTeTTableSuffix();
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
        return Order.ofOrder( "" );
    }

    private Select getSelect()
    {
        Select.SelectBuilder builder = Select.builder()
            // static fields
            .field( getFieldWithAlias( "trackedentityinstanceid" ) )
            .field( getFieldWithAlias( "trackedentityinstanceuid" ) );

        // TET and program attribute fields
        Stream.concat(
            // Tracked entity Type attributes
            teiQueryParams.getTrackedEntityType().getTrackedEntityTypeAttributes().stream(),
            // Program attributes
            teiQueryParams.getCommonParams().getPrograms().stream()
                .map( Program::getProgramAttributes )
                .flatMap( Collection::stream )
                .map( ProgramTrackedEntityAttribute::getAttribute ) )
            .map( BaseIdentifiableObject::getUid )
            // to normalize overlapping attributes
            .distinct()
            .map( this::getFieldWithAlias )
            .forEach( builder::field );

        // todo: add sort by field if needed
        return builder.build();
    }

    private Renderable getFieldWithAlias( String field )
    {
        return Field.of( TEI_TEI_DEFAULT_ALIAS, () -> field, field );
    }

    private From getFrom()
    {
        From.FromBuilder builder = From.builder();
        builder.tablesOrSubQuery( getMainTable() );

        List<Pair<Renderable, Renderable>> joinTablesAndConditions = getJoinTablesAndConditions();

        joinTablesAndConditions.forEach(
            renderableRenderablePair -> {
                builder.tablesOrSubQuery( renderableRenderablePair.getLeft() );
                builder.joinCondition( renderableRenderablePair.getRight() );
            } );
        return builder.build();
    }

    public Renderable getMainTable()
    {
        return Table.ofStrings( getMainTableName(), TEI_TEI_DEFAULT_ALIAS );
    }

    private List<Pair<Renderable, Renderable>> getJoinTablesAndConditions()
    {
        // TODO: implement logic for join when needed, i.e.
        // when sorting on a data element is present
        return Collections.emptyList();
    }

    private Where getWhere()
    {
        Where.WhereBuilder builder = Where.builder();
        // conditions on programs (is enrolled in program)
        getTeiQueryParams().getCommonParams().getPrograms()
            .forEach( program -> builder.condition( () -> TEI_TEI_DEFAULT_ALIAS + "." + program.getUid() ) );
        // conditions on filters/dimensions
        getTeiQueryParams().getCommonParams().getDimensionIdentifiers()
            .stream()
            .map( this::toConditions )
            .map( OrCondition::of )
            .forEach( builder::condition );

        return builder.build();
    }

    private List<Renderable> toConditions(
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        return dimensionIdentifiers.stream()
            .filter( dimensionIdentifier -> dimensionIdentifier.getDimension().hasRestrictions() )
            .map( this::toCondition )
            .collect( Collectors.toList() );
    }

    private Renderable toCondition( DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier )
    {
        return OrCondition.of(
            dimensionIdentifier.getDimension().getItems().stream()
                .map( item -> EventDataValueCondition.of( dimensionIdentifier, item, this ) )
                .collect( Collectors.toList() ) );
    }

    public int bindParamAndGetIndex( Object param )
    {
        return parameterManager.bindParamAndGetIndex( param );
    }

    private static class ParameterManager
    {

        private int parameterIndex = 1;

        @Getter
        private final Map<Integer, Object> parametersByPlaceHolder = new HashMap<>();

        public int bindParamAndGetIndex( Object param )
        {
            parametersByPlaceHolder.put( parameterIndex, param );
            parameterIndex++;
            return parameterIndex;
        }
    }

}

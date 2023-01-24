/*
 * Copyright (c) 2004-2004, University of Oslo
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.DATA_ELEMENT;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.ORGANISATION_UNIT;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PERIOD;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.analytics.common.query.RenderableUtils.join;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.From;
import org.hisp.dhis.analytics.common.query.JoinsWithConditions;
import org.hisp.dhis.analytics.common.query.LimitOffset;
import org.hisp.dhis.analytics.common.query.OrCondition;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.common.query.Select;
import org.hisp.dhis.analytics.common.query.Where;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.query.context.QueryContext;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

@Getter
@Setter
@AllArgsConstructor( access = PRIVATE )
@Builder( toBuilder = true )
public class TeiFullQuery extends BaseRenderable
{
    private final QueryContext queryContext;

    private final TeiQueryParams teiQueryParams;

    private final Map<String, Object> paramsPlaceHolders;

    @Override
    public String render()
    {
        return join( Stream.of( getSelect(), getFrom(), getWhere(), getOrder(), getLimit() )
            .filter( Objects::nonNull )
            .collect( toList() ),
            SPACE );
    }

    public TeiFullQuery( final QueryContext queryContext )
    {
        this.queryContext = queryContext;
        this.teiQueryParams = queryContext.getTeiQueryParams();
        this.paramsPlaceHolders = queryContext.getParametersByPlaceHolder();
    }

    public SqlQuery statement()
    {
        return new SqlQuery( render(), paramsPlaceHolders );
    }

    private LimitOffset getLimit()
    {
        AnalyticsPagingParams pagingParams = teiQueryParams.getCommonParams().getPagingParams();

        return LimitOffset.of( pagingParams.getPageSize(),
            pagingParams.getPageSize() * (pagingParams.getPage() - 1) );
    }

    private Order getOrder()
    {
        return Order.builder()
            .orders(
                Stream.concat(
                    queryContext.getSortingContext().getOrders().stream(),
                    queryContext.getProgramIndicatorContext().getOrders().stream() )
                    .collect( toList() ) )
            .build();
    }

    private Select getSelect()
    {
        Stream<Field> teiFields = TeiFields.getTeiFields();
        Stream<Field> dimensionsFields = TeiFields.getDimensionFields( teiQueryParams );
        Stream<Field> orderingFields = queryContext.getSortingContext().getFields().stream();
        Stream<Field> programIndicatorFields = queryContext.getProgramIndicatorContext().getFields().stream();

        Stream<Field> fields = Stream.of(
            teiFields,
            dimensionsFields,
            orderingFields,
            programIndicatorFields )
            .flatMap( identity() );

        if ( isNotEmpty( teiQueryParams.getCommonParams().getHeaders() ) )
        {
            fields = fields.filter( f -> teiQueryParams.getCommonParams().getHeaders().contains( f.getFieldAlias() ) );
        }

        return Select.of( fields.collect( toList() ) );
    }

    private From getFrom()
    {
        JoinsWithConditions joinsWithConditions = JoinsWithConditions.of(
            Stream.concat(
                queryContext.getSortingContext().getLeftJoins().stream(),
                queryContext.getProgramIndicatorContext().getLeftJoins().stream() )
                .collect( toList() ) );

        return From.of( queryContext.getMainTable(), joinsWithConditions );
    }

    private Where getWhere()
    {
        // We want all PERIOD dimension to be in the same group
        Stream<DimensionIdentifier<Program, ProgramStage, DimensionParam>> periodDimensions = teiQueryParams
            .getCommonParams().getDimensionIdentifiers()
            .stream()
            .flatMap( Collection::stream )
            .filter( d -> d.getDimension().isPeriodDimension() );

        // Conditions on programs (is enrolled in program)
        Stream<Renderable> programConditions = teiQueryParams
            .getCommonParams()
            .getPrograms()
            .stream()
            .map( BaseIdentifiableObject::getUid )
            .map( EnrolledInProgramCondition::of );

        // Conditions on filters/dimensions
        Stream<Renderable> dimensionConditions = teiQueryParams.getCommonParams().getDimensionIdentifiers()
            .stream()
            .filter( this::isNotPeriodDimension )
            .map( this::toConditions )
            .map( OrCondition::of );

        Renderable periodConditions = OrCondition.of( periodDimensions
            .map( periodDimension -> toCondition( DimensionParamObjectType.PERIOD,
                Collections.singletonList( periodDimension ) ) )
            .collect( toList() ) );

        return Where.of(
            AndCondition.of(
                Stream.of(
                    programConditions,
                    dimensionConditions,
                    queryContext.getProgramIndicatorContext().getConditions().stream(),
                    Stream.of( periodConditions ) )
                    .flatMap( Function.identity() )
                    .collect( toList() ) ) );
    }

    private boolean isNotPeriodDimension(
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        return dimensionIdentifiers.stream()
            .noneMatch( dimId -> dimId.getDimension().isPeriodDimension() );
    }

    private List<Renderable> toConditions(
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        return dimensionIdentifiers.stream()
            .filter( di -> di.getDimension().hasRestrictions() )
            .collect( Collectors.groupingBy( di -> di.getDimension().getDimensionParamObjectType() ) )
            .entrySet().stream()
            .map( entry -> toCondition( entry.getKey(), entry.getValue() ) )
            .collect( toList() );
    }

    private Renderable toCondition( DimensionParamObjectType type,
        List<DimensionIdentifier<Program, ProgramStage, DimensionParam>> dimensionIdentifiers )
    {
        if ( type == DATA_ELEMENT )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> EventDataValueCondition.of( dimensionIdentifier, queryContext ) )
                    .collect( toList() ) );
        }

        if ( type == PROGRAM_ATTRIBUTE )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> ProgramAttributeCondition.of( dimensionIdentifier, queryContext ) )
                    .collect( toList() ) );
        }

        if ( type == ORGANISATION_UNIT )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> OrganisationUnitCondition.of( dimensionIdentifier, queryContext ) )
                    .collect( toList() ) );
        }

        if ( type == PERIOD )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> PeriodCondition.of( dimensionIdentifier, queryContext ) )
                    .collect( toList() ) );
        }

        return () -> EMPTY;
    }
}

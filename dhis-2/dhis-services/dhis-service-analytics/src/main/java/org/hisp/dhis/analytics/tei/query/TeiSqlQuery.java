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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.From;
import org.hisp.dhis.analytics.common.query.GroupRenderable;
import org.hisp.dhis.analytics.common.query.JoinsWithConditions;
import org.hisp.dhis.analytics.common.query.LimitOffset;
import org.hisp.dhis.analytics.common.query.OrCondition;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.common.query.RootConditionRenderer;
import org.hisp.dhis.analytics.common.query.Select;
import org.hisp.dhis.analytics.common.query.Where;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.query.context.QueryContext;
import org.hisp.dhis.common.BaseIdentifiableObject;

/**
 * Class that encapsulates all necessary objects to render a SQL statement. It's
 * focused on the generation of SQL statements for TEIs.
 */
@Getter
@Setter
@AllArgsConstructor( access = PRIVATE )
@Builder( toBuilder = true )
public class TeiSqlQuery extends BaseRenderable
{
    /**
     * The query context.
     */
    private final QueryContext queryContext;

    /**
     * The query parameters.
     */
    private final TeiQueryParams teiQueryParams;

    /**
     * a map for query placeholders and their values.
     */
    private final Map<String, Object> queryPlaceHolders;

    /**
     * Internal flag to allow returning "count" queries.
     */
    boolean countQuery = false;

    @Override
    public String render()
    {
        Select select = getSelect();

        if ( countQuery )
        {
            select = getCountSelect();
        }

        return join( Stream.of( select, getFrom(), getWhere(), getOrder(), getLimit() )
            .filter( Objects::nonNull )
            .collect( toList() ),
            SPACE );
    }

    public TeiSqlQuery( QueryContext queryContext )
    {
        this.queryContext = queryContext;
        this.teiQueryParams = queryContext.getTeiQueryParams();
        this.queryPlaceHolders = queryContext.getParametersPlaceHolder();
    }

    public SqlQuery get()
    {
        countQuery = false;

        return new SqlQuery( render(), queryPlaceHolders );
    }

    public SqlQuery count()
    {
        countQuery = true;

        return new SqlQuery( render(), queryPlaceHolders );
    }

    private LimitOffset getLimit()
    {
        AnalyticsPagingParams pagingParams = teiQueryParams.getCommonParams().getPagingParams();

        return LimitOffset.of( pagingParams.getPageSize(), pagingParams.getOffset() );
    }

    private Order getOrder()
    {
        return Order.builder()
            .orders( Stream.concat(
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
        Stream<Field> fields = Stream.of( teiFields, dimensionsFields, orderingFields, programIndicatorFields )
            .flatMap( identity() );

        if ( isNotEmpty( teiQueryParams.getCommonParams().getHeaders() ) )
        {
            fields = fields.filter( f -> teiQueryParams.getCommonParams().getHeaders().contains( f.getFieldAlias() ) );
        }

        return Select.of( fields.collect( toList() ) );
    }

    private Select getCountSelect()
    {
        return Select.ofUnquoted( "count(1)" );
    }

    /**
     * Returns the "from" clause of the query. It's composed by the following
     * parts:
     * <ul>
     * <li>main table</li>
     * <li>left joins</li>
     * </ul>
     *
     * @return the "from" clause of the query
     */
    private From getFrom()
    {
        JoinsWithConditions joinsWithConditions = JoinsWithConditions.of(
            Stream.concat(
                queryContext.getSortingContext().getLeftJoins().stream(),
                queryContext.getProgramIndicatorContext().getLeftJoins().stream() )
                .collect( toList() ) );

        return From.of( queryContext.getMainTable(), joinsWithConditions );
    }

    /**
     * Returns the "where" clause of the query. It's composed by the following
     * parts:
     * <ul>
     * <li>Static conditions (periods, programs, etc)</li>
     * <li>Dynamic conditions (filters)</li>
     * <li>Program indicator conditions</li>
     * </ul>
     *
     * @return the "where" clause of the query
     */
    private Where getWhere()
    {
        /* "Statical" conditions that are added as AND conditions */

        Stream<DimensionIdentifier<DimensionParam>> periodDimensions = teiQueryParams
            .getCommonParams().getDimensionIdentifiers()
            .stream()
            .filter( d -> d.getDimension().isPeriodDimension() );

        // Periods are OR conditions by default
        Renderable periodConditions = OrCondition.of( periodDimensions
            .map( this::toCondition )
            .collect( toList() ) );

        // Conditions on programs (aka "is enrolled in program")
        Stream<Renderable> programConditions = teiQueryParams.getCommonParams().getPrograms().stream()
            .map( BaseIdentifiableObject::getUid )
            .map( EnrolledInProgramCondition::of );

        Stream<GroupRenderable> staticallyAndConditions = Stream
            .concat( Stream.of( periodConditions ), programConditions )
            // we use an hard-coded value for representing the group name of all conditions that doesn't follow
            // the grouping logic
            .map( renderable -> GroupRenderable.of( "statically_and_conditions", renderable ) );

        /* Dynamic/groupable conditions */

        // Conditions on filters/dimensions
        Stream<GroupRenderable> dimensionConditions = teiQueryParams.getCommonParams().getDimensionIdentifiers()
            .stream()
            .filter( this::isNotPeriodDimension )
            .map( this::toGroupRenderable )
            .filter( Optional::isPresent )
            .map( Optional::get );

        Stream<GroupRenderable> programIndicatorConditions = queryContext.getProgramIndicatorContext().getConditions()
            .stream();

        return Where.of( RootConditionRenderer.of(
            Stream.of( staticallyAndConditions, dimensionConditions, programIndicatorConditions )
                .flatMap( identity() )
                .collect( toList() ) ) );
    }

    /**
     * Given a dimension identifier, returns a {@link GroupRenderable} if the
     * dimension identifier is a filter
     *
     * @param dimensionIdentifier
     * @return an optional {@link GroupRenderable}
     */
    private Optional<GroupRenderable> toGroupRenderable( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        if ( dimensionIdentifier.getDimension().hasRestrictions() )
        {
            return Optional.of( GroupRenderable.of(
                dimensionIdentifier.getGroupId(),
                toCondition( dimensionIdentifier ) ) );
        }
        return Optional.empty();
    }

    /**
     * Returns true if the given dimension identifier is not a period dimension
     *
     * @param dimensionIdentifier the dimension identifier
     * @return true if the given dimension identifier is not a period dimension
     */
    private boolean isNotPeriodDimension(
        DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return !dimensionIdentifier.getDimension().isPeriodDimension();
    }

    /**
     * Given a dimension identifier, returns a {@link Renderable} representing
     * the condition
     *
     * @param dimensionIdentifier the dimension identifier
     * @return a {@link Renderable} representing the condition
     */
    private Renderable toCondition( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        DimensionParamObjectType type = dimensionIdentifier.getDimension().getDimensionParamObjectType();

        if ( type == DATA_ELEMENT )
        {
            return EventDataValueCondition.of( dimensionIdentifier, queryContext );
        }

        if ( type == PROGRAM_ATTRIBUTE )
        {
            return ProgramAttributeCondition.of( dimensionIdentifier, queryContext );
        }

        if ( type == ORGANISATION_UNIT )
        {
            return OrganisationUnitCondition.of( dimensionIdentifier, queryContext );
        }

        if ( type == PERIOD )
        {
            return PeriodCondition.of( dimensionIdentifier, queryContext );
        }

        return () -> EMPTY;
    }
}

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
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.shared.SqlQuery;
import org.hisp.dhis.analytics.shared.query.AndCondition;
import org.hisp.dhis.analytics.shared.query.BaseRenderable;
import org.hisp.dhis.analytics.shared.query.Field;
import org.hisp.dhis.analytics.shared.query.From;
import org.hisp.dhis.analytics.shared.query.JoinsWithConditions;
import org.hisp.dhis.analytics.shared.query.LimitOffset;
import org.hisp.dhis.analytics.shared.query.OrCondition;
import org.hisp.dhis.analytics.shared.query.Order;
import org.hisp.dhis.analytics.shared.query.Renderable;
import org.hisp.dhis.analytics.shared.query.RenderableDimensionIdentifier;
import org.hisp.dhis.analytics.shared.query.RenderableUtils;
import org.hisp.dhis.analytics.shared.query.Select;
import org.hisp.dhis.analytics.shared.query.Where;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;

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
        return RenderableUtils.join(
            Stream.of( getSelect(), getFrom(), getWhere(), getOrder(), getLimit() )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() ),
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
        AnalyticsPagingParams pagingAndSortingParams = teiQueryParams.getCommonParams()
            .getPagingAndSortingParams();

        return LimitOffset.of(
            pagingAndSortingParams.getPageSize(),
            pagingAndSortingParams.getPageSize() * pagingAndSortingParams.getPage() );
    }

    private Order getOrder()
    {
        List<Renderable> collect = teiQueryParams.getCommonParams().getOrderParams().stream()
            .map( p -> (Renderable) () -> "\"" + p.getOrderBy().toString() + "\" " + p.getSortDirection().name() )
            .collect( toList() );

        return Order.builder()
            .orders( collect )
            .build();
    }

    private Select getSelect()
    {
        Stream<Field> staticFields = getStaticFields();

        // TODO remove next line when attributes match those in the table.
        List<String> notGeneratedColumns = List.of( "Jdd8hMStmvF", "EGn5VqU7pHv", "JBJ3AWsrg9P" );

        // TET and program attribute fields
        Stream<Field> attributes = Stream.concat(
            // Tracked entity Type attributes
            teiQueryParams.getTrackedEntityType().getTrackedEntityTypeAttributes().stream(),
            // Program attributes
            teiQueryParams.getCommonParams().getPrograms().stream()
                .map( Program::getProgramAttributes )
                .flatMap( Collection::stream )
                .map( ProgramTrackedEntityAttribute::getAttribute ) )
            .map( BaseIdentifiableObject::getUid )
            // distinct to remove overlapping attributes
            .distinct()
            // TODO remove next line when attributes match those in the table.
            .filter( uid -> !notGeneratedColumns.contains( uid ) )
            .map( attributeUid -> "\"" + attributeUid + "\"" )
            .map( this::getFieldWithAlias );

        Stream<Field> orderFields = getExtractedOrderFieldsForSelect();

        if ( isNotEmpty( teiQueryParams.getCommonParams().getHeaders() ) )
        {
            return Select.of(
                Stream.of( staticFields, attributes, orderFields )
                    .flatMap( identity() )
                    .filter( f -> teiQueryParams.getCommonParams().getHeaders().contains( f.getFieldAlias() ) )
                    .collect( toList() ) );
        }

        return Select.of(
            Stream.of( staticFields, attributes, orderFields )
                .flatMap( identity() )
                .collect( toList() ) );
    }

    private Stream<Field> getStaticFields()
    {
        return Stream.of( getFieldWithAlias( "trackedentityinstanceuid" ), getFieldWithAlias( "trackedentitytypeuid" ),
            getFieldWithAlias( "created" ), getFieldWithAlias( "lastupdated" ), getFieldWithAlias( "geometry" ),
            getFieldWithAlias( "createdbydisplayname" ), getFieldWithAlias( "lastupdatedbydisplayname" ),
            getFieldWithAlias( "longitude" ), getFieldWithAlias( "latitude" ), getFieldWithAlias( "coordinates" ),
            getFieldWithAlias( "ou" ), getFieldWithAlias( "ouname" ), getFieldWithAlias( "oucode" ),
            getFieldWithAlias( "oulevel" ), getFieldWithAlias( "coordinates" ), getFieldWithAlias( "enrollments" ) );
    }

    private Stream<Field> getExtractedOrderFieldsForSelect()
    {
        return teiQueryParams.getCommonParams().getOrderParams()
            .stream()
            .map( AnalyticsSortingParams::getOrderBy )
            .map( RenderableDimensionIdentifier::of )
            .map( RenderableDimensionIdentifier::render )
            .map( s -> Field.of( "", () -> "\"" + s + "\".VALUE", "VALUE" ) );
    }

    private Field getFieldWithAlias( String field )
    {
        return Field.of( TEI_ALIAS, () -> field, field );
    }

    private From getFrom()
    {
        return From.of( queryContext.getMainTable(), getJoinTablesAndConditions() );
    }

    private JoinsWithConditions getJoinTablesAndConditions()
    {
        JoinsWithConditions.JoinsWithConditionsBuilder builder = JoinsWithConditions.builder();

        getOrdersForSubQuery( teiQueryParams ).stream()
            .map( this::toOrderSubQueryWithCondition )
            .forEach( builder::tablesWithJoinCondition );

        return builder.build();
    }

    private Pair<Renderable, Renderable> toOrderSubQueryWithCondition( AnalyticsSortingParams analyticsSortingParams )
    {
        return LeftJoinQueryBuilder.of( analyticsSortingParams, queryContext );
    }

    private List<AnalyticsSortingParams> getOrdersForSubQuery( TeiQueryParams teiQueryParams )
    {
        return teiQueryParams.getCommonParams().getOrderParams().stream()
            .filter( this::needsSubQuery )
            .collect( toList() );
    }

    private boolean needsSubQuery( AnalyticsSortingParams analyticsSortingParams )
    {
        DimensionIdentifier<Program, ProgramStage, DimensionParam> orderBy = analyticsSortingParams.getOrderBy();

        return orderBy.hasProgram();
    }

    private Where getWhere()
    {
        // conditions on programs (is enrolled in program)
        Stream<Renderable> programConditions = teiQueryParams
            .getCommonParams()
            .getPrograms()
            .stream()
            .map( BaseIdentifiableObject::getUid )
            .map( EnrolledInProgramCondition::of );

        // conditions on filters/dimensions
        Stream<Renderable> dimensionConditions = teiQueryParams.getCommonParams().getDimensionIdentifiers()
            .stream()
            .map( this::toConditions )
            .map( OrCondition::of );

        return Where.of(
            AndCondition.of(
                Stream.concat(
                    programConditions,
                    dimensionConditions )
                    .collect( toList() ) ) );
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
        // TODO: depending on the type, we should build a proper condition
        if ( type == DATA_ELEMENT )
        {
            return AndCondition.of(
                dimensionIdentifiers.stream()
                    .map( dimensionIdentifier -> EventDataValueCondition.of( dimensionIdentifier, queryContext ) )
                    .collect( toList() ) );
        }

        return () -> EMPTY;
    }
}

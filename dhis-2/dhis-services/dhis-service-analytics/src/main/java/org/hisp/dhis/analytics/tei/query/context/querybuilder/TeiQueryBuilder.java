/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.DimensionIdentifierType.TEI;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.hasRestrictions;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.TeiAttributeCondition;
import org.hisp.dhis.analytics.tei.query.TeiFields;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders;
import org.springframework.stereotype.Service;

/**
 * This builder is responsible for building the SQL query for the TEI table. It
 * will generate the relevant SQL parts related to
 * dimensions/filters/sortingParameters having one the following structure: -
 * {teiField} - {programUid}.{programAttribute}
 */
@Service
public class TeiQueryBuilder extends SqlQueryBuilderAdaptor
{

    @Override
    public boolean alwaysRun()
    {
        return true;
    }

    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = List.of(
        SqlQueryBuilders::isNotPeriodDimension,
        OrgUnitQueryBuilder::isNotOuDimension,
        TeiQueryBuilder::isTeiRestriction );

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = List.of( TeiQueryBuilder::isTeiOrder );

    @Override
    protected Stream<Field> getSelect( QueryContext queryContext )
    {
        return Stream.concat(
            // Static fields + 'enrollment' dynamic column.
            TeiFields.getStaticAndDynamicFields(),

            // Tei/Program attributes.
            TeiFields.getDimensionFields( queryContext.getTeiQueryParams() ) );
    }

    @Override
    protected Stream<GroupableCondition> getWhereClauses( QueryContext queryContext,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions )
    {
        return acceptedDimensions.stream()
            .map( dimId -> GroupableCondition.of(
                dimId.getGroupId(),
                TeiAttributeCondition.of( dimId, queryContext ) ) );
    }

    /**
     * Returns true if the given dimension identifier has restrictions and is
     * either a TEI dimension or a Program indicator
     *
     * @param dimensionIdentifier the dimension identifier
     * @return true if the given dimension identifier has restrictions and is
     *         either a TEI dimension or a Program indicator
     */
    private static boolean isTeiRestriction( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return hasRestrictions( dimensionIdentifier ) && isTei( dimensionIdentifier );
    }

    /**
     * Returns true if the given dimension identifier is either a TEI dimension
     * or a Program indicator
     *
     * @param dimensionIdentifier the dimension identifier
     * @return true if the given dimension identifier is either a TEI dimension
     *         or a Program indicator
     */
    private static boolean isTei( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return
        // Will match all dimensionIdentifiers like {dimensionUid}.
        dimensionIdentifier.getDimensionIdentifierType() == TEI ||
        // Will match all dimensionIdentifiers whose type is PROGRAM_ATTRIBUTE.
        // e.g. {programUid}.{attributeUid}
            isOfType( dimensionIdentifier, PROGRAM_ATTRIBUTE );
    }

    @Override
    protected Stream<IndexedOrder> getOrderClauses( QueryContext queryContext,
        List<AnalyticsSortingParams> acceptedSortingParams )
    {
        return acceptedSortingParams.stream()
            .map( this::toIndexedOrder );
    }

    private IndexedOrder toIndexedOrder( AnalyticsSortingParams param )
    {
        // Here, we can assume that param is either a static dimension or
        // a TEI/Program attribute in the form asc=pUid.dimension (or desc=pUid.dimension)
        // in both cases the column for the select is the same.
        String column = param.getOrderBy().getDimension().getUid();
        return IndexedOrder.of( param.getIndex(),
            Order.of( Field.of( column ), param.getSortDirection() ) );
    }

    private static boolean isTeiOrder( AnalyticsSortingParams analyticsSortingParams )
    {
        return isTei( analyticsSortingParams.getOrderBy() );
    }
}

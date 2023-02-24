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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.tei.query.PeriodCondition;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilderAdaptor;
import org.springframework.stereotype.Service;

import lombok.Getter;

/**
 * This class is responsible for adding period conditions to the SQL query. By
 * design, "Period" conditions are grouped together in their own single group.
 * This means that the period conditions are not combined with other conditions
 * and are rendered as a single group of "OR" conditions.
 */
@Service
public class PeriodQueryBuilder extends SqlQueryBuilderAdaptor
{
    private static final String PERIOD_CONDITION_GROUP = "PERIOD_CONDITION";

    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = List
        .of( d -> d.getDimension().isPeriodDimension() );

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = Collections.emptyList();

    @Override
    protected Stream<GroupableCondition> getWhereClauses( QueryContext ctx,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions )
    {
        return acceptedDimensions.stream()
            .map( dimensionParamDimensionIdentifier -> PeriodCondition.of( dimensionParamDimensionIdentifier, ctx ) )
            .map( periodCondition -> GroupableCondition.of( PERIOD_CONDITION_GROUP, periodCondition ) );
    }
}

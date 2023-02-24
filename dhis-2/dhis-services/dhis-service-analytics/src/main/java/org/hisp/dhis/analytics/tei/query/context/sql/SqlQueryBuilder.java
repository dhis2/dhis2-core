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
package org.hisp.dhis.analytics.tei.query.context.sql;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;

/**
 * Provides the required methods to build {@link RenderableSqlQuery} objects.
 */
public interface SqlQueryBuilder
{
    @Nonnull
    /**
     * Builds a {@link RenderableSqlQuery} based on the given arguments.
     *
     * @param queryContext the {@link QueryContext}.
     * @param dimensions the list of {@link DimensionIdentifier}.
     * @param sortingParams the list of {@link AnalyticsSortingParams}.
     */
    RenderableSqlQuery buildSqlQuery( @Nonnull
    QueryContext queryContext,
        @Nonnull
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        @Nonnull
        List<AnalyticsSortingParams> acceptedSortingParams );

    /**
     * Provides the list of {@link Predicate} functions for
     * {@link DimensionIdentifier}. They act as filters and are used to build
     * the final {@link RenderableSqlQuery} query.
     * 
     * @return the list of filter dimensions or empty.
     */
    @Nonnull
    List<Predicate<DimensionIdentifier<DimensionParam>>> getDimensionFilters();

    /**
     * Provides the list of {@link Predicate} functions for
     * {@link DimensionIdentifier}. They are used for sorting and are part of
     * the final {@link RenderableSqlQuery} query.
     *
     * @return the list of sorting dimensions or empty.
     */
    @Nonnull
    List<Predicate<AnalyticsSortingParams>> getSortingFilters();
}

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
package org.hisp.dhis.analytics.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.program.Program;

/**
 * This is a reusable and shared representation of queryable items to be used by
 * the service/dao layers.
 *
 * It encapsulates the most common objects that are very likely to be used by
 * the majority of analytics queries.
 */
@Getter
@Setter
@Builder( toBuilder = true )
public class CommonParams
{

    /**
     * The list of Program objects carried on by this object.
     */
    private final Collection<Program> programs;

    /**
     * Data structure containing dimensionParams, which can represent
     * dimensions, filters, queryItems or queryItemFilters.
     */
    @Builder.Default
    private final List<DimensionParam> dimensionParams = new ArrayList<>();

    /**
     * The object that groups the paging and sorting parameters.
     */
    @Builder.Default
    private final AnalyticsPagingAndSortingParams pagingAndSortingParams = AnalyticsPagingAndSortingParams.builder()
        .build();
}

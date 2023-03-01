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
package org.hisp.dhis.analytics.tei;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeiQueryParamPostProcessor
{

    private static final String DEFAULT_PERIOD = "pe:LAST_YEAR:LAST_UPDATED";

    private final CommonQueryRequestMapper commonQueryRequestMapper;

    public TeiQueryParams postProcess( TeiQueryParams params )
    {
        boolean existsPeriodRestriction = params.getCommonParams().getDimensionIdentifiers().stream()
            .filter( d -> d.getDimension().isPeriodDimension() )
            .anyMatch( d -> d.getDimension().hasRestrictions() );

        if ( existsPeriodRestriction )
        {
            return params;
        }

        DimensionIdentifier<DimensionParam> defaultPeriod = commonQueryRequestMapper.toDimensionIdentifier(
            DEFAULT_PERIOD,
            DimensionParamType.DATE_FILTERS,
            params.getCommonParams().getRelativePeriodDate(),
            params.getCommonParams().getDisplayProperty(),
            params.getCommonParams().getPrograms(),
            params.getCommonParams().getUserOrgUnit() );

        List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = Stream
            .concat( params.getCommonParams().getDimensionIdentifiers().stream(), Stream.of( defaultPeriod ) )
            .collect( Collectors.toList() );

        return params.toBuilder()
            .commonParams(
                params.getCommonParams().toBuilder()
                    .dimensionIdentifiers( dimensionIdentifiers )
                    .build() )
            .build();

    }
}

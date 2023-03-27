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
package org.hisp.dhis.analytics.common.params.dimension;

import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.feedback.ErrorCode.E2035;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryOperator;

@Getter
@RequiredArgsConstructor( access = PRIVATE )
public class DimensionParamItem
{
    private final QueryOperator operator;

    private final List<String> values;

    public static List<DimensionParamItem> ofStrings( List<String> items )
    {
        if ( items.isEmpty() )
        {
            return Collections.emptyList();
        }
        // If operator is specified, it's in the first element.
        String firstElement = items.get( 0 );

        if ( firstElement.contains( DIMENSION_NAME_SEP ) )
        { // Has operator.
            String[] parts = firstElement.split( DIMENSION_NAME_SEP );
            QueryOperator queryOperator = getOperator( parts[0] );
            return singletonList(
                new DimensionParamItem(
                    queryOperator,
                    Stream.concat( Stream.of( parts[1] ),
                        items.stream()
                            .skip( 1 ) )
                        .collect( Collectors.toList() ) ) );
        }
        else
        {
            return singletonList( new DimensionParamItem( null, items ) );
        }
    }

    private static QueryOperator getOperator( String operator )
    {
        return Arrays.stream( QueryOperator.values() )
            .filter( queryOperator -> equalsIgnoreCase( queryOperator.name(), operator ) )
            .findFirst()
            .orElseThrow( () -> new IllegalQueryException( E2035, operator ) );
    }
}

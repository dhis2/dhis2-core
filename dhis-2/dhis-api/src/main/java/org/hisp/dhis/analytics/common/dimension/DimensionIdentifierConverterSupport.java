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
package org.hisp.dhis.analytics.common.dimension;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;
import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset.emptyElementWithOffset;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class DimensionIdentifierConverterSupport
{

    private static final Character DIMENSION_SEPARATOR = '.';

    public static DimensionIdentifier<StringUid, StringUid, StringUid> fromFullDimensionId( String fullDimensionId )
    {
        List<DimensionIdentifier.ElementWithOffset<StringUid>> uidWithOffsets = parseFullDimensionId( fullDimensionId );

        if ( uidWithOffsets.size() == 3 ) // ie.: abcde[1].fghi[4].jklm
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 2 ) );
            return DimensionIdentifier.of( uidWithOffsets.get( 0 ), uidWithOffsets.get( 1 ),
                uidWithOffsets.get( 2 ).getElement() );
        }

        if ( uidWithOffsets.size() == 2 ) // ie.: abcde[1].hfjg
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 1 ) );
            return DimensionIdentifier.of( uidWithOffsets.get( 0 ), emptyElementWithOffset(),
                uidWithOffsets.get( 1 ).getElement() );

        }

        if ( uidWithOffsets.size() == 1 ) // ie.: fgrg
        {
            assertDimensionIdHasNoOffset( uidWithOffsets.get( 0 ) );
            return DimensionIdentifier.of( emptyElementWithOffset(), emptyElementWithOffset(),
                uidWithOffsets.get( 0 ).getElement() );
        }

        throw new IllegalArgumentException( "Malformed parameter: " + fullDimensionId );
    }

    private static List<DimensionIdentifier.ElementWithOffset<StringUid>> parseFullDimensionId( String fullDimensionId )
    {
        return stream( split( fullDimensionId, DIMENSION_SEPARATOR ) )
            .map( DimensionIdentifierConverterSupport::elementWithOffsetByString )
            .collect( toList() );
    }

    private static void assertDimensionIdHasNoOffset(
        DimensionIdentifier.ElementWithOffset<StringUid> dimensionIdWithOffset )
    {
        if ( dimensionIdWithOffset.hasOffset() )
        {
            throw new IllegalArgumentException( "Only program and program stage can have offsets" );
        }
    }

    private static DimensionIdentifier.ElementWithOffset<StringUid> elementWithOffsetByString(
        String elementWithOffset )
    {
        String[] split = split( elementWithOffset, "[]" );
        if ( split.length == 2 )
        {
            return DimensionIdentifier.ElementWithOffset.of( StringUid.of( split[0] ), split[1] );
        }
        else
        {
            return DimensionIdentifier.ElementWithOffset.of( StringUid.of( elementWithOffset ), null );
        }
    }
}

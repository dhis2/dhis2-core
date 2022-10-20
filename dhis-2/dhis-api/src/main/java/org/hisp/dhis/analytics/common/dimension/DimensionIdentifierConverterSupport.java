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
        // will parse correctly only if uidWiothOffset is not empty and size is
        // less than or equals to 3
        List<DimensionIdentifier.ElementWithOffset<StringUid>> uidWithOffsets = parseFullDimensionId( fullDimensionId );

        return DimensionIdentifier.of(
            getProgram( uidWithOffsets ),
            getProgramStage( uidWithOffsets ),
            getDimension( uidWithOffsets ) );
    }

    private static DimensionIdentifier.ElementWithOffset<StringUid> getProgram(
        List<DimensionIdentifier.ElementWithOffset<StringUid>> uidWithOffsets )
    {
        if ( uidWithOffsets.size() == 1 )
        {
            return emptyElementWithOffset();
        }
        return uidWithOffsets.get( 0 );
    }

    private static DimensionIdentifier.ElementWithOffset<StringUid> getProgramStage(
        List<DimensionIdentifier.ElementWithOffset<StringUid>> uidWithOffsets )
    {
        if ( uidWithOffsets.size() == 2 || uidWithOffsets.size() == 1 )
        {
            return emptyElementWithOffset();
        }
        else
        {
            return uidWithOffsets.get( 1 );
        }
    }

    private static StringUid getDimension( List<DimensionIdentifier.ElementWithOffset<StringUid>> uidWithOffsets )
    {
        DimensionIdentifier.ElementWithOffset<StringUid> dimension = uidWithOffsets.get( uidWithOffsets.size() - 1 );
        assertDimensionIdHasNoOffset( dimension );
        return dimension.getElement();
    }

    private static List<DimensionIdentifier.ElementWithOffset<StringUid>> parseFullDimensionId( String fullDimensionId )
    {
        List<DimensionIdentifier.ElementWithOffset<StringUid>> elements = stream(
            split( fullDimensionId, DIMENSION_SEPARATOR ) )
                .map( DimensionIdentifierConverterSupport::elementWithOffsetByString )
                .collect( toList() );
        if ( elements.size() > 3 || elements.isEmpty() )
        {
            throw new IllegalArgumentException( "Invalid dimension identifier: " + fullDimensionId );
        }
        return elements;
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

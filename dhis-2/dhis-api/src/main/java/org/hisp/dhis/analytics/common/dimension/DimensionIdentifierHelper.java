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
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.split;
import static org.hisp.dhis.analytics.common.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_IDENTIFIER_SEP;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.NoArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UidObject;

@NoArgsConstructor( access = PRIVATE )
public class DimensionIdentifierHelper
{
    public static final Character DIMENSION_SEPARATOR = '.';

    /**
     * Will parse the given argument into a {@link DimensionIdentifier} object.
     *
     * @param fullDimensionId in the format
     *        "PROGRAM_UID[1].PSTAGE_UID[4].DIM_UID".
     * @throws IllegalArgumentException when the format of the given argument is
     *         not supported.
     * @return the {@link DimensionIdentifier} object.
     */
    public static StringDimensionIdentifier fromFullDimensionId( String fullDimensionId )
    {
        List<ElementWithOffset<StringUid>> uidWithOffsets = parseFullDimensionId( fullDimensionId );
        boolean nonSupportedFormat = uidWithOffsets.size() > 3 || uidWithOffsets.isEmpty();

        if ( nonSupportedFormat )
        {
            throw new IllegalArgumentException( "Invalid dimension identifier: " + fullDimensionId );
        }

        return StringDimensionIdentifier.of(
            getProgram( uidWithOffsets ),
            getProgramStage( uidWithOffsets ),
            getDimension( uidWithOffsets ) );
    }

    private static ElementWithOffset<StringUid> getProgram( List<ElementWithOffset<StringUid>> uidWithOffsets )
    {
        boolean hasOnlySingleDimension = uidWithOffsets.size() == 1;

        if ( hasOnlySingleDimension )
        {
            return emptyElementWithOffset();
        }

        return uidWithOffsets.get( 0 );
    }

    private static ElementWithOffset<StringUid> getProgramStage( List<ElementWithOffset<StringUid>> uidWithOffsets )
    {
        boolean hasOnlyProgramAndDimension = uidWithOffsets.size() == 2 || uidWithOffsets.size() == 1;

        if ( hasOnlyProgramAndDimension )
        {
            return emptyElementWithOffset();
        }
        else
        {
            return uidWithOffsets.get( 1 );
        }
    }

    private static StringUid getDimension( List<ElementWithOffset<StringUid>> uidWithOffsets )
    {
        int dimensionIndex = uidWithOffsets.size() - 1;

        ElementWithOffset<StringUid> dimension = uidWithOffsets.get( dimensionIndex );
        assertDimensionIdHasNoOffset( dimension );

        return dimension.getElement();
    }

    private static List<ElementWithOffset<StringUid>> parseFullDimensionId( String fullDimensionId )
    {
        return stream(
            split( fullDimensionId, DIMENSION_SEPARATOR ) )
                .map( DimensionIdentifierHelper::elementWithOffsetByString )
                .collect( toList() );
    }

    private static void assertDimensionIdHasNoOffset( ElementWithOffset<StringUid> dimensionIdWithOffset )
    {
        if ( dimensionIdWithOffset.hasOffset() )
        {
            throw new IllegalArgumentException( "Only program and program stage can have offset" );
        }
    }

    private static ElementWithOffset<StringUid> elementWithOffsetByString( String elementWithOffset )
    {
        String[] split = split( elementWithOffset, "[]" );
        boolean hasOffset = split.length == 2;

        if ( hasOffset )
        {
            String elementUid = split[0];
            String offset = split[1];

            return ElementWithOffset.of( StringUid.of( elementUid ), offset );
        }
        else
        {
            return ElementWithOffset.of( StringUid.of( elementWithOffset ), null );
        }
    }

    public static String asText( ElementWithOffset<? extends UidObject> program,
        ElementWithOffset<? extends UidObject> programStage, UidObject dimension )
    {
        String string = "";

        if ( Objects.nonNull( program ) && program.isPresent() )
        {
            string += program + DIMENSION_IDENTIFIER_SEP;
        }

        if ( Objects.nonNull( programStage ) && programStage.isPresent() )
        {
            string += programStage + DIMENSION_IDENTIFIER_SEP;
        }

        if ( Objects.nonNull( dimension ) )
        {
            string += dimension.getUid();
        }

        return string;
    }

    public static String getPrefix( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return getPrefix( dimensionIdentifier, true );
    }

    public static String getPrefix( DimensionIdentifier<DimensionParam> dimensionIdentifier, boolean quote )
    {
        return Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getPrefix )
            .filter( StringUtils::isNotBlank )
            .map( s -> quote ? doubleQuote( s ) : s )
            .orElse( TEI_ALIAS );
    }
}

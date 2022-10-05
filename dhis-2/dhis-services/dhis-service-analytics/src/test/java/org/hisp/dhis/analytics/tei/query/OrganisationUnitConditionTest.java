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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.common.*;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class OrganisationUnitConditionTest
{
    @Test
    void testTeiOuMultipleOusProduceCorrectSql()
    {

        // SETUP
        List<String> ous = List.of( "ou1", "ou2" );
        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier = getOUDimensionIdentifier(
            ous );

        // CALL
        QueryContext queryContext = QueryContext.of( null );

        OrganisationUnitCondition organisationUnitCondition = OrganisationUnitCondition.of( dimensionIdentifier,
            queryContext );

        String render = organisationUnitCondition.render();

        assertEquals( "\"ou\" in (:1)", render );
        assertEquals( ous, queryContext.getParametersByPlaceHolder().get( "1" ) );

    }

    @Test
    void testTeiOuSingleOusProduceCorrectSql()
    {

        // SETUP
        List<String> ous = List.of( "ou1" );

        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier = getOUDimensionIdentifier(
            ous );

        // CALL
        QueryContext queryContext = QueryContext.of( null );

        OrganisationUnitCondition organisationUnitCondition = OrganisationUnitCondition.of( dimensionIdentifier,
            queryContext );

        String render = organisationUnitCondition.render();

        assertEquals( "\"ou\" = :1", render );
        assertEquals( ous.get( 0 ), queryContext.getParametersByPlaceHolder().get( "1" ) );

    }

    private DimensionIdentifier<Program, ProgramStage, DimensionParam> getOUDimensionIdentifier( List<String> ous )
    {
        DimensionParam dimensionParam = DimensionParam.ofObject(
            new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT,
                ous.stream()
                    .map( BaseDimensionalItemObject::new )
                    .collect( Collectors.toList() ) ),
            DimensionParamType.DIMENSIONS,
            ous );

        return DimensionIdentifier.of(
            emptyElementWithOffset(),
            emptyElementWithOffset(),
            dimensionParam );
    }

}

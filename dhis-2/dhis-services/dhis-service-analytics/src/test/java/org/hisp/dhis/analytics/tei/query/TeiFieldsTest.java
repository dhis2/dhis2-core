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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.shared.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * TeiFields unit tests.
 *
 * @author Dusan Bernat
 */
class TeiFieldsTest extends DhisConvenienceTest
{
    @Test
    void testGetOrderingFieldsWithOrgUnitName()
    {
        // given
        TeiQueryParams teiQueryParams = TeiQueryParams.builder()
            .commonParams( stubSortingCommonParams( null, "0", "ouname" ) )
            .build();

        // when
        Optional<Field> orderingField = TeiFields.getOrderingFields( teiQueryParams ).findFirst();

        // then
        // ouname is static field present in the select part of statement and
        // will not add into the ordering part of select (duplicates)
        // is still present in order by section
        assertFalse( orderingField.isPresent() );
    }

    @ParameterizedTest
    @ValueSource( strings = { "enddate", "enrollmentdate", "incidentdate" } ) // six
                                                                              // numbers
    void testGetDateOrderingFields( String dateDimensionName )
    {
        // given
        TeiQueryParams teiQueryParams = TeiQueryParams.builder()
            .commonParams( stubSortingCommonParams( null, "0", dateDimensionName ) )
            .build();

        // when
        Optional<Field> orderingField = TeiFields.getOrderingFields( teiQueryParams ).findFirst();

        // then
        assertTrue( orderingField.isPresent() );

        assertEquals( "enr." + dateDimensionName, orderingField.get().render() );
    }

    @Test
    void testGetOrderingFieldsWithCommonDimensionalObject()
    {
        // given
        DimensionalObject dimensionalObject = new BaseDimensionalObject( "a" );

        Program program = createProgram( 'A' );

        TeiQueryParams teiQueryParams = TeiQueryParams.builder()
            .commonParams( stubSortingCommonParams( program, "4", dimensionalObject ) )
            .build();

        // when
        Optional<Field> orderingField = TeiFields.getOrderingFields( teiQueryParams ).findFirst();

        // then
        assertTrue( orderingField.isPresent() );

        assertEquals( "t_1.\"" + program.getUid() + "[4].a\".VALUE as VALUE", orderingField.get().render() );
    }

    private CommonParams stubSortingCommonParams( Program program, String offset, Object dimensionalObject )
    {
        ElementWithOffset<Program> prg = program == null ? ElementWithOffset.emptyElementWithOffset()
            : ElementWithOffset.of( program, offset );

        ElementWithOffset<ProgramStage> programStage = ElementWithOffset.emptyElementWithOffset();

        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier = DimensionIdentifier.of( prg,
            programStage,
            DimensionParam.ofObject( dimensionalObject, DimensionParamType.SORTING, List.of( StringUtils.EMPTY ) ) );

        AnalyticsSortingParams analyticsSortingParams = AnalyticsSortingParams.builder()
            .sortDirection( SortDirection.ASC )
            .orderBy( dimensionIdentifier )
            .build();

        return CommonParams.builder()
            .orderParams( List.of( analyticsSortingParams ) )
            .build();
    }
}

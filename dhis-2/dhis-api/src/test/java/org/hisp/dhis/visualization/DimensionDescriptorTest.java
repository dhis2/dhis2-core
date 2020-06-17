package org.hisp.dhis.visualization;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PRETTY_NAMES;
import static org.hisp.dhis.visualization.DimensionDescriptor.retrieveDescriptiveValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.DimensionType;
import org.junit.Test;

public class DimensionDescriptorTest
{
    @Test
    public void testHasAbbreviationWithSuccess()
    {
        // Given
        final String anyDimensionValue = "dx";
        final DimensionType theDimensionType = DATA_X;
        final DimensionDescriptor aDimensionDescriptor = new DimensionDescriptor( anyDimensionValue, theDimensionType );
        final String dimensionAbbreviation = "dx";

        // When
        final boolean actualResult = aDimensionDescriptor.hasAbbreviation( dimensionAbbreviation );

        // Then
        assertThat( actualResult, is( true ) );
    }

    @Test
    public void testHasAbbreviationFails()
    {
        // Given
        final String anyDimensionValue = "dx";
        final DimensionType theDimensionType = DATA_X;
        final DimensionDescriptor aDimensionDescriptor = new DimensionDescriptor( anyDimensionValue, theDimensionType );
        final String nonExistingDimensionAbbreviation = "ou";

        // When
        final boolean actualResult = aDimensionDescriptor.hasAbbreviation( nonExistingDimensionAbbreviation );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testRetrieveDescriptiveValue()
    {
        // Given
        final String anyDimensionDynamicValue = "ZyxerTyuP";
        final List<DimensionDescriptor> someDimensionDescriptors = mockDimensionDescriptors( anyDimensionDynamicValue );
        final String aValueToRetrieve = "dx";
        final Map<String, String> metaData = mockMetaData();

        // When
        final String actualResult = retrieveDescriptiveValue( someDimensionDescriptors, aValueToRetrieve, metaData );

        // Then
        assertThat( actualResult, is( DATA_X_DIM_ID ) );
    }

    @Test
    public void testRetrieveDescriptiveValueDynamicValue()
    {
        // Given
        final String theDimensionDynamicValueToRetrieve = "ZyxerTyuP";
        final List<DimensionDescriptor> someDimensionDescriptors = mockDimensionDescriptors(
            theDimensionDynamicValueToRetrieve );
        final Map<String, String> metaData = mockMetaData();

        // When
        final String actualResult = retrieveDescriptiveValue( someDimensionDescriptors,
            theDimensionDynamicValueToRetrieve,
            metaData );

        // Then
        assertThat( actualResult, is( ORGUNIT_DIM_ID ) );
    }

    private List<DimensionDescriptor> mockDimensionDescriptors( final String aDynamicValue )
    {
        final DimensionDescriptor dx = new DimensionDescriptor( "dx", DATA_X );
        final DimensionDescriptor dynamicOu = new DimensionDescriptor( aDynamicValue, ORGANISATION_UNIT );

        return Arrays.asList( dx, dynamicOu );
    }

    private Map<String, String> mockMetaData()
    {
        final Map<String, String> metaData = new HashMap<>();
        metaData.putAll( PRETTY_NAMES );

        return metaData;
    }
}

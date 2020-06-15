package org.hisp.dhis.webapi.controller.dataitem.helper;

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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntitiesFromInFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntityFromEqualFilter;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import java.util.Set;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.query.QueryParserException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FilteringHelperTest
{
    @Rule
    public ExpectedException expectedException = none();

    @Test
    public void testExtractEntitiesFromInFilter()
    {
        // Given
        final String anyFilters = "dimensionItemType:in:[INDICATOR,DATA_SET]";
        final Class<? extends BaseDimensionalItemObject>[] expectedClasses = new Class[] { Indicator.class,
            DataSet.class };

        // When
        final Set<Class<? extends BaseDimensionalItemObject>> actualClasses = extractEntitiesFromInFilter( anyFilters );

        // Then
        assertThat( actualClasses, hasSize( 2 ) );
        assertThat( actualClasses, containsInAnyOrder( expectedClasses ) );
    }

    @Test
    public void testExtractEntitiesFromInFilterWhenTypeIsInvalid()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:in:[INVALID_TYPE,DATA_SET]";

        // Expect
        expectedException.expect( QueryParserException.class );
        expectedException.expectMessage( "Unable to parse `" + "INVALID_TYPE" );

        // When
        extractEntitiesFromInFilter( filtersWithInvalidType );
    }

    @Test
    public void testExtractEntitiesFromInFilterWhenFilterContainsOnlyEmpties()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:in:[,]";

        // Expect
        expectedException.expect( QueryParserException.class );
        expectedException.expectMessage( "Unable to parse the filter `" + filtersWithInvalidType + "`" );

        // When
        extractEntitiesFromInFilter( filtersWithInvalidType );
    }

    @Test
    public void testExtractEntitiesFromInFilterWhenFilterIsNotFullyDefined()
    {
        // Given
        final String filtersNotFullyDefined = "dimensionItemType:in:[,DATA_SET]";
        final Class<? extends BaseDimensionalItemObject>[] expectedClasses = new Class[] { DataSet.class };

        // When
        final Set<Class<? extends BaseDimensionalItemObject>> actualClasses = extractEntitiesFromInFilter(
            filtersNotFullyDefined );

        // Then
        assertThat( actualClasses, hasSize( 1 ) );
        assertThat( actualClasses, containsInAnyOrder( expectedClasses ) );
    }

    @Test
    public void testExtractEntityFromEqualFilter()
    {
        // Given
        final String anyFilter = "dimensionItemType:eq:DATA_SET";
        final Class<? extends BaseDimensionalItemObject> expectedClass = DataSet.class;

        // When
        final Class<? extends BaseDimensionalItemObject> actualClass = extractEntityFromEqualFilter( anyFilter );

        // Then
        assertThat( actualClass, is( notNullValue() ) );
        assertThat( actualClass, is( equalTo( expectedClass ) ) );
    }

    @Test
    public void testExtractEntityFromEqualFilterWhenTypeIsInvalid()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:eq:INVALID_TYPE";

        // Expect
        expectedException.expect( QueryParserException.class );
        expectedException.expectMessage( "Unable to parse `" + "INVALID_TYPE" );

        // When
        extractEntityFromEqualFilter( filtersWithInvalidType );
    }

    @Test
    public void testExtractEntityFromEqualFilterWhenFilterIsInvalid()
    {
        // Given
        final String invalidFilter = "dimensionItemType:eq:";

        // Expect
        expectedException.expect( QueryParserException.class );
        expectedException.expectMessage( "Unable to parse the filter `" + invalidFilter + "`" );

        // When
        extractEntityFromEqualFilter( invalidFilter );
    }
}

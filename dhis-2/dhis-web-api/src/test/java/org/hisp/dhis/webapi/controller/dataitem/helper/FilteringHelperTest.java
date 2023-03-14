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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.EnumUtils.getEnumMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntitiesFromInFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractEntityFromEqualFilter;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.extractValueFromFilter;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Set;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataitem.query.QueryableDataItem;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.webapi.controller.dataitem.Filter;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FilteringHelper.
 *
 * @author maikel arabori
 */
class FilteringHelperTest
{
    @Test
    @SuppressWarnings( "unchecked" )
    void testExtractEntitiesFromInFilter()
    {
        // Given
        final String anyFilters = "dimensionItemType:in:[INDICATOR,DATA_SET]";
        final Class<? extends BaseDimensionalItemObject>[] expectedClasses = new Class[] { Indicator.class,
            DataSet.class };
        // When
        final Set<Class<? extends BaseIdentifiableObject>> actualClasses = extractEntitiesFromInFilter( anyFilters );
        // Then
        assertThat( actualClasses, hasSize( 2 ) );
        assertThat( actualClasses, containsInAnyOrder( expectedClasses ) );
    }

    @Test
    void testExtractEntitiesFromInFilterWhenTypeIsInvalid()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:in:[INVALID_TYPE,DATA_SET]";
        // Then
        assertThrows( IllegalQueryException.class, () -> extractEntitiesFromInFilter( filtersWithInvalidType ),
            "Unable to parse element `" + "INVALID_TYPE` on filter `dimensionItemType`. The values available are: "
                + Arrays.toString( getEnumMap( QueryableDataItem.class ).keySet().toArray() ) );
    }

    @Test
    void testExtractEntitiesFromInFilterWhenFilterContainsOnlyEmpties()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:in:[,]";
        // When
        assertThrows( IllegalQueryException.class, () -> extractEntitiesFromInFilter( filtersWithInvalidType ),
            "Unable to parse filter `" + filtersWithInvalidType + "`" );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    void testExtractEntitiesFromInFilterWhenFilterIsNotFullyDefined()
    {
        // Given
        final String filtersNotFullyDefined = "dimensionItemType:in:[,DATA_SET]";
        final Class<? extends BaseIdentifiableObject>[] expectedClasses = new Class[] { DataSet.class };
        // When
        final Set<Class<? extends BaseIdentifiableObject>> actualClasses = extractEntitiesFromInFilter(
            filtersNotFullyDefined );
        // Then
        assertThat( actualClasses, hasSize( 1 ) );
        assertThat( actualClasses, containsInAnyOrder( expectedClasses ) );
    }

    @Test
    void testExtractEntityFromEqualFilter()
    {
        // Given
        final String anyFilter = "dimensionItemType:eq:DATA_SET";
        final Class<? extends BaseDimensionalItemObject> expectedClass = DataSet.class;
        // When
        final Class<? extends BaseIdentifiableObject> actualClass = extractEntityFromEqualFilter( anyFilter );
        // Then
        assertThat( actualClass, is( notNullValue() ) );
        assertThat( actualClass, is( equalTo( expectedClass ) ) );
    }

    @Test
    void testExtractEntityFromEqualFilterWhenTypeIsInvalid()
    {
        // Given
        final String filtersWithInvalidType = "dimensionItemType:eq:INVALID_TYPE";
        // When
        assertThrows( IllegalQueryException.class, () -> extractEntityFromEqualFilter( filtersWithInvalidType ),
            "Unable to parse element `" + "INVALID_TYPE` on filter `dimensionItemType`. The values available are: "
                + Arrays.toString( getEnumMap( QueryableDataItem.class ).keySet().toArray() ) );
    }

    @Test
    void testExtractEntityFromEqualFilterWhenFilterIsInvalid()
    {
        // Given
        final String invalidFilter = "dimensionItemType:eq:";
        // When
        assertThrows( IllegalQueryException.class, () -> extractEntityFromEqualFilter( invalidFilter ),
            "Unable to parse filter `" + invalidFilter + "`" );
    }

    @Test
    void testExtractValueFromFilterNoTrimmed()
    {
        // Given
        final Set<String> filters = newHashSet( "name:ilike:aWord", "programId:eq:anyId " );
        final Filter.Combination theCombination = Filter.Combination.PROGRAM_ID_EQUAL;
        // When
        final String expectedValue = extractValueFromFilter( filters, theCombination );
        // Then
        assertThat( expectedValue, is( "anyId " ) );
    }

    @Test
    void testExtractValueFromFilterTrimmed()
    {
        // Given
        final Set<String> filters = newHashSet( "name:ilike:aWord", "programId:eq:anyId" );
        final Filter.Combination theCombination = Filter.Combination.PROGRAM_ID_EQUAL;
        // When
        final String expectedValue = extractValueFromFilter( filters, theCombination, true );
        // Then
        assertThat( expectedValue, is( "anyId" ) );
    }
}

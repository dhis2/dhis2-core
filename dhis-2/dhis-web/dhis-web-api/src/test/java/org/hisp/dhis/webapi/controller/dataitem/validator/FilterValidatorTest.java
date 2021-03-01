/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataitem.validator;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_IN;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.checkNamesAndOperators;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.containsFilterWithAnyOfPrefixes;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.filterHasPrefix;
import static org.junit.rules.ExpectedException.none;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for FilterValidator.
 *
 * @author maikel arabori
 */
public class FilterValidatorTest
{
    @Rule
    public final ExpectedException exception = none();

    @Test
    public void testCheckNamesAndOperatorsWhenFilterHasInvalidFormat()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:someName:bla:bla" ) );

        // Except exception
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Unable to parse filter `name:ilike:someName:bla:bla`" );

        // When
        checkNamesAndOperators( filters );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenCombinationIsInvalid()
    {
        // Given
        final String invalidCombination = "dimensionItemType:ilike:";
        final Set<String> filters = new HashSet<>( singletonList( invalidCombination ) );

        // Except exception
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Unable to parse filter `dimensionItemType:ilike:`" );

        // When
        checkNamesAndOperators( filters );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenOperationIsInvalid()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:invalidOperation:aWord" ) );

        // Except exception
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Operator not supported: `invalidOperation`" );

        // When
        checkNamesAndOperators( filters );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenAttributeIsInvalid()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "invalidAttribute:ilike:aWord" ) );

        // Except exception
        exception.expect( IllegalQueryException.class );
        exception.expectMessage( "Filter not supported: `invalidAttribute`" );

        // When
        checkNamesAndOperators( filters );
    }

    @Test( expected = Test.None.class ) /* no exception is expected */
    public void testCheckNamesAndOperatorsWithSuccess()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:aWord" ) );
        final boolean noExceptionIsThrown = true;

        // When
        checkNamesAndOperators( filters );

        // Then
        assert (noExceptionIsThrown);
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenPrefixMatches()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "attribute:ilike:aWord" ) );
        final String thePrefix = "attribute:ilike:";

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, thePrefix );

        // Then
        assertThat( actualResult, is( true ) );
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenMultiplePrefixesMatch()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "attribute:ilike:aWord" ) );
        final String[] thePrefixes = { "attribute:ilike:", "attribute" };

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, thePrefixes );

        // Then
        assertThat( actualResult, is( true ) );
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenWeHaveMultiplFiltersAndOneMatch()
    {
        // Given
        final Set<String> filters = newHashSet( "attribute:ilike:aWord", "programId:eq:abcderf" );
        final String thePrefix = "programId:eq:";

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, thePrefix );

        // Then
        assertThat( actualResult, is( true ) );
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenPrefixDoesNotMatch()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "attribute:ilike:aWord" ) );
        final String thePrefix = "attribute:eq:";

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, thePrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenListOfFiltersIsEmpty()
    {
        // Given
        final Set<String> filters = emptySet();
        final String thePrefix = "attribute:eq:";

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, thePrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testContainsFilterWithAnyOfPrefixesWhenPrefixIsNull()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "attribute:ilike:aWord" ) );
        final String nullPrefix = null;

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, nullPrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testFilterHasPrefixMatches()
    {
        final String anyFilter = "displayName:ilike:someBla";
        final String anyPrefix = "displayName:ilike:";

        // When
        final boolean actualResult = filterHasPrefix( anyFilter, anyPrefix );

        // Then
        assertThat( actualResult, is( true ) );
    }

    @Test
    public void testFilterHasPrefixDoesNotMatch()
    {
        final String anyFilter = "displayName:ilike:someBla";
        final String nonExistingPrefix = "nonExistingPrefix";

        // When
        final boolean actualResult = filterHasPrefix( anyFilter, nonExistingPrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testFilterHasPrefixWhenPrefixIsNull()
    {
        final String anyFilter = "displayName:ilike:someBla";
        final String nullPrefix = null;

        // When
        final boolean actualResult = filterHasPrefix( anyFilter, nullPrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testFilterHasPrefixWhenFilterIsNull()
    {
        final String nullFilter = null;
        final String anyPrefix = "bla";

        // When
        final boolean actualResult = filterHasPrefix( nullFilter, anyPrefix );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testContainsDimensionTypeFilterUsingEqualsQuery()
    {
        // Given
        final Set<String> filters = newHashSet( "dimensionItemType:eq:DATA_SET" );

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters,
            DIMENSION_TYPE_EQUAL.getCombination() );

        // Then
        assertThat( actualResult, Matchers.is( true ) );
    }

    @Test
    public void testContainsDimensionTypeFilterUsingInQuery()
    {
        // Given
        final Set<String> filters = newHashSet( "dimensionItemType:in:[DATA_SET,INDICATOR]" );

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, DIMENSION_TYPE_IN.getCombination() );

        // Then
        assertThat( actualResult, Matchers.is( true ) );
    }

    @Test
    public void testContainsDimensionTypeFilterWhenDimensionItemTypeInFilterIsNotSet()
    {
        // Given
        final Set<String> filters = newHashSet( "displayName:ilike:anc" );

        // When
        final boolean actualResult = containsFilterWithAnyOfPrefixes( filters, DIMENSION_TYPE_IN.getCombination() );

        // Then
        assertThat( actualResult, Matchers.is( false ) );
    }
}

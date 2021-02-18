package org.hisp.dhis.webapi.controller.dataitem.validator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.checkNamesAndOperators;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.containsFilterWithAnyOfPrefixes;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.filterHasPrefix;
import static org.junit.Assert.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * Unit tests for FilterValidator.
 *
 * @author maikel arabori
 */
public class FilterValidatorTest
{
    @Test
    public void testCheckNamesAndOperatorsWhenFilterHasInvalidFormat()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:someName:bla:bla" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkNamesAndOperators( filters ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Unable to parse filter `name:ilike:someName:bla:bla`" ) );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenIlikeComparisonValueLengthIsNotEnough()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:a" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkNamesAndOperators( filters ) );

        // Then
        assertThat( thrown.getMessage(),
            containsString( "Minimum of `2` characters required by the filter: `name:ilike:a`" ) );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenCombinationIsInvalid()
    {
        // Given
        final String invalidCombination = "dimensionItemType:ilike:";
        final Set<String> filters = new HashSet<>( singletonList( invalidCombination ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkNamesAndOperators( filters ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Unable to parse filter `dimensionItemType:ilike:`" ) );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenOperationIsInvalid()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "name:invalidOperation:aWord" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkNamesAndOperators( filters ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Operator not supported: `invalidOperation`" ) );
    }

    @Test
    public void testCheckNamesAndOperatorsWhenAttributeIsInvalid()
    {
        // Given
        final Set<String> filters = new HashSet<>( singletonList( "invalidAttribute:ilike:aWord" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkNamesAndOperators( filters ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Filter not supported: `invalidAttribute`" ) );
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
        final Set<String> filters = Sets.newHashSet( "attribute:ilike:aWord", "programId:eq:abcderf" );
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
}

package org.hisp.dhis.webapi.controller.dataitem.validator;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.checkOrderParams;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.checkOrderParamsAndFiltersAllowance;
import static org.junit.Assert.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;

/**
 * Unit tests for OrderValidator.
 *
 * @author maikel arabori
 */
public class OrderValidatorTest
{
    @Test
    public void testCheckOrderParamsWhenOrderAttributeIsNotSupported()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "notSupportedAttribute:asc" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkOrderParams( orderings ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Order not supported: `notSupportedAttribute`" ) );
    }

    @Test
    public void testCheckOrderParamsWhenOrderingValueIsInvalid()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:invalidOrdering" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkOrderParams( orderings ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Order not supported: `invalidOrdering`" ) );
    }

    @Test
    public void testCheckOrderParamsWhenOrderingFormIsInvalid()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:asc:invalid" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkOrderParams( orderings ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Unable to parse order param: `name:asc:invalid`" ) );
    }

    @Test( expected = Test.None.class ) /* no exception is expected */
    public void testCheckOrderParamsWithSuccess()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:desc" ) );
        final boolean noExceptionIsThrown = true;

        // When
        checkOrderParams( orderings );

        // Then
        assert (noExceptionIsThrown);
    }

    @Test( expected = Test.None.class ) /* no exception is expected */
    public void testCheckOrderParamsAndFiltersAllowanceWithSuccess()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:asc" ) );
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:someName" ) );
        final boolean noExceptionIsThrown = true;

        // When
        checkOrderParamsAndFiltersAllowance( orderings, filters );

        // Then
        assert (noExceptionIsThrown);
    }

    @Test
    public void testCheckOrderParamsAndFiltersAllowanceUsingNameOnOrderAndDisplayNameOnFilter()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:asc" ) );
        final Set<String> filters = new HashSet<>( singletonList( "displayName:ilike:someName" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkOrderParamsAndFiltersAllowance( orderings, filters ) );

        // Then
        assertThat( thrown.getMessage(),
            containsString( "Combination not supported: `name:asc + displayName:ilike:someName`" ) );
    }

    @Test
    public void testCheckOrderParamsAndFiltersAllowanceUsingDisplayNameOnOrderAndNameOnFilter()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "displayName:asc" ) );
        final Set<String> filters = new HashSet<>( singletonList( "name:ilike:someName" ) );

        // When throws
        final IllegalQueryException thrown = assertThrows( IllegalQueryException.class,
            () -> checkOrderParamsAndFiltersAllowance( orderings, filters ) );

        // Then
        assertThat( thrown.getMessage(),
            containsString( "Combination not supported: `displayName:asc + name:ilike:someName`" ) );
    }
}

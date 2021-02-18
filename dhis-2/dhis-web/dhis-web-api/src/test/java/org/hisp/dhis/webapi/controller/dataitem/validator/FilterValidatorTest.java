package org.hisp.dhis.webapi.controller.dataitem.validator;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.checkNamesAndOperators;
import static org.junit.Assert.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;

public class FilterValidatorTest
{
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
}

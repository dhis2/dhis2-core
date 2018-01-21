package org.hisp.dhis.reservedvalue.hibernate;

import org.hisp.dhis.reservedvalue.DefaultReservedValueService;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@RunWith( MockitoJUnitRunner.class )
public class DefaultReservedValueServiceTest
{

    @InjectMocks
    private DefaultReservedValueService reservedValueService;

    @Mock
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Before
    public void setup()
    {
        Mockito.when( sequentialNumberCounterStore.getNextValues( "ABC", "ABC-#", 1 ) )
            .thenReturn( getRange( 1, 2 ) );
        Mockito.when( sequentialNumberCounterStore.getNextValues( "ABC", "ABC-#", 3 ) )
            .thenReturn( getRange( 1, 4 ) );
        Mockito.when( sequentialNumberCounterStore.getNextValues( "ABC", "ABC-##", 1 ) )
            .thenReturn( getRange( 1, 2 ) );
        Mockito.when( sequentialNumberCounterStore.getNextValues( "ABC", "ABC-####", 1 ) )
            .thenReturn( getRange( 1, 2 ) );
    }

    @Test
    public void generateAndReserveRandomValues()
    {
    }

    @Test
    public void generateAndReserveSequentialValues()
    {
        assertEquals( 1, reservedValueService.generateAndReserveSequentialValues( "ABC", "ABC-#", "#", 1 ).size() );
        assertEquals( 3, reservedValueService.generateAndReserveSequentialValues( "ABC", "ABC-#", "#", 3 ).size() );
        assertEquals( "1", reservedValueService.generateAndReserveSequentialValues( "ABC", "ABC-#", "#", 1 ).get( 0 ) );
        assertEquals( "01",
            reservedValueService.generateAndReserveSequentialValues( "ABC", "ABC-##", "##", 1 ).get( 0 ) );
        assertEquals( "0001",
            reservedValueService.generateAndReserveSequentialValues( "ABC", "ABC-####", "####", 1 ).get( 0 ) );

    }

    private List<Integer> getRange( int start, int end )
    {
        return IntStream.range( start, end ).boxed().collect( Collectors.toList() );
    }
}
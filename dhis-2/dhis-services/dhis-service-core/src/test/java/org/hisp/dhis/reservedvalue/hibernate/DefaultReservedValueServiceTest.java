package org.hisp.dhis.reservedvalue.hibernate;

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.reservedvalue.DefaultReservedValueService;
import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.dhis.reservedvalue.ReservedValueStore;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternMethod;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class DefaultReservedValueServiceTest
    extends DhisSpringTest
{
    @InjectMocks
    private DefaultReservedValueService reservedValueService;

    @Mock
    private ReservedValueStore reservedValueStore;

    @Mock
    private TextPatternService textPatternService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TextPattern simplePattern;

    private TextPattern randomPattern;

    private ReservedValue reservedValue;

    private Date future;

    private final String TEST_STRING = "Hello world!";

    private final String RANDOM_FORMAT = "RANDOM(#####)";

    @Before
    public void setup()
        throws Exception
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.DATE, 10 );

        future = calendar.getTime();

        simplePattern = new TextPattern(
            Lists.newArrayList( new TextPatternSegment( TextPatternMethod.TEXT, TEST_STRING ) ) );
        simplePattern.setOwnerUID( "UID_A" );
        simplePattern.setClazz( TrackedEntityAttribute.class.getSimpleName() );

        randomPattern = new TextPattern(
            Lists.newArrayList( new TextPatternSegment( TextPatternMethod.RANDOM, RANDOM_FORMAT ) ) );
        randomPattern.setOwnerUID( "UID_B" );
        randomPattern.setClazz( TrackedEntityAttribute.class.getSimpleName() );

        reservedValue = new ReservedValue( simplePattern.getClazz(), simplePattern.getOwnerUID(), TEST_STRING,
            TEST_STRING, future );

        Mockito.when( textPatternService
            .resolvePattern( Mockito.any( TextPattern.class ), Mockito.anyMapOf( String.class, String.class ) ) )
            .thenReturn( TEST_STRING );
        Mockito.when( reservedValueStore.getNumberOfUsedValues( reservedValue ) ).thenReturn( 0 );
        Mockito.when( reservedValueStore.reserveValues( Mockito.any( ReservedValue.class ), Mockito.anyListOf( String.class ) ) ).thenCallRealMethod();
    }

    @Test
    public void testReserveReserveASingleSimpleValue()
        throws Exception
    {
        reservedValueService.reserve( simplePattern, 1, new HashMap<>(), future );

        Mockito.verify( reservedValueStore ).reserveValues( reservedValue, Lists.newArrayList( TEST_STRING ) );
    }

    @Test
    public void testReserveReserveMultipleSingleValuesShouldFail()
        throws Exception
    {
        thrown.expect( Exception.class );
        thrown.expectMessage( "Not enough values left to reserve 2 values." );
        reservedValueService.reserve( simplePattern, 2, new HashMap<>(), future );
    }

    @Test
    public void testReserveReserveASingleRandomValue()
        throws Exception
    {
        reservedValueService.reserve( randomPattern, 1, new HashMap<>(), future );

        Mockito.verify( reservedValueStore )
            .reserveValues( Mockito.any( ReservedValue.class ), Mockito.anyListOf( String.class ) );
    }
}
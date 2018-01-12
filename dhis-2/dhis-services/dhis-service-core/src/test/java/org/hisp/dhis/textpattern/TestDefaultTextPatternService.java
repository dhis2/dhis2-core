package org.hisp.dhis.textpattern;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class TestDefaultTextPatternService
{
    @InjectMocks
    private DefaultTextPatternService textPatternService;

    @Mock
    private ReservedValueService reservedValueService;

    private TextPattern pattern;

    private ImmutableMap<String, String> values;

    @Before
    public void init()
    {
        List<TextPatternSegment> segments = new ArrayList<>();

        segments.add( new TextPatternSegment( TextPatternMethod.TEXT, "\"TEXT\"" ) );
        segments.add( new TextPatternSegment( TextPatternMethod.ORG_UNIT_CODE, "ORG_UNIT_CODE(...)" ) );
        segments.add( new TextPatternSegment( TextPatternMethod.SEQUENTIAL, "SEQUENTIAL(#)" ) );
        segments.add( new TextPatternSegment( TextPatternMethod.CURRENT_DATE, "CURRENT_DATE(YYYY)" ) );

        pattern = new TextPattern( segments );

        values = ImmutableMap.<String, String>builder()
            .put( "ORG_UNIT_CODE(...)", "OSLO" )
            .put( "SEQUENTIAL(#)", "1" )
            .build();

        when(
            reservedValueService.generateAndReserveSequentialValues( anyString(), anyString(), anyString(), anyInt() ) )
            .thenReturn(
                Lists.newArrayList( "1" )
            );
    }

    @Test
    public void testGetRequiredValues()
    {
        List<String> required = textPatternService.getRequiredValues( pattern ).get( "REQUIRED" );

        assertFalse( required.contains( "TEXT" ) );
        assertFalse( required.contains( "CURRENT_DATE" ) );
        assertTrue( required.contains( "ORG_UNIT_CODE(...)" ) );
        assertFalse( required.contains( "SEQUENTIAL(#)" ) );
        assertEquals( 1, required.size() );
    }

    @Test
    public void testGetOptionalValues()
    {
        List<String> optional = textPatternService.getRequiredValues( pattern ).get( "OPTIONAL" );

        assertFalse( optional.contains( "TEXT" ) );
        assertFalse( optional.contains( "CURRENT_DATE" ) );
        assertFalse( optional.contains( "ORG_UNIT_CODE(...)" ) );
        assertTrue( optional.contains( "SEQUENTIAL(#)" ) );
        assertEquals( 1, optional.size() );
    }

    @Test
    public void testResolvePattern()
        throws Exception
    {
        String result = textPatternService.resolvePattern( pattern, values );

        assertEquals( "TEXTOSL1" + (new SimpleDateFormat( "YYYY" ).format( new Date() )), result );
    }
}

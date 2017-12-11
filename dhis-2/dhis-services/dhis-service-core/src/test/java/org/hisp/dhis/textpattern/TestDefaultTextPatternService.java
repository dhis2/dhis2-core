package org.hisp.dhis.textpattern;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.textpattern.MethodType.RequiredStatus.OPTIONAL;
import static org.hisp.dhis.textpattern.MethodType.RequiredStatus.REQUIRED;
import static org.junit.Assert.*;

@RunWith( MockitoJUnitRunner.class )
public class TestDefaultTextPatternService
{
    @InjectMocks
    private DefaultTextPatternService textPatternService;

    private TextPattern pattern;

    private ImmutableMap<String, String> values;

    @Before
    public void init()
    {
        pattern = new TextPattern();

        pattern.addSegment( "\"TEXT\"", TextPatternMethod.TEXT.getType() );
        pattern.addSegment( "ORG_UNIT_CODE(...)", TextPatternMethod.ORG_UNIT_CODE.getType() );
        pattern.addSegment( "SEQUENTIAL(#)", TextPatternMethod.SEQUENTIAL.getType() );
        pattern.addSegment( "CURRENT_DATE(YYYY)", TextPatternMethod.CURRENT_DATE.getType() );

        values = ImmutableMap.<String, String>builder()
            .put( "ORG_UNIT_CODE(...)", "OSLO" )
            .put( "SEQUENTIAL(#)", "1" )
            .build();
    }

    @Test
    public void testGetRequiredValues()
    {
        List<String> required = textPatternService.getRequiredValues( pattern ).get( REQUIRED );

        assertFalse( required.contains( "TEXT" ) );
        assertFalse( required.contains( "CURRENT_DATE" ) );
        assertTrue( required.contains( "ORG_UNIT_CODE(...)" ) );
        assertFalse( required.contains( "SEQUENTIAL(#)" ) );
        assertEquals( 1, required.size() );
    }

    @Test
    public void testGetOptionalValues()
    {
        List<String> optional = textPatternService.getRequiredValues( pattern ).get( OPTIONAL );

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

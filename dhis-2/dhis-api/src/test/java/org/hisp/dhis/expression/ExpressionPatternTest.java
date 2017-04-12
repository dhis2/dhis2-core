package org.hisp.dhis.expression;

import java.util.regex.Matcher;

import org.junit.Test;

import static org.hisp.dhis.expression.ExpressionService.OPERAND_PATTERN;
import static org.hisp.dhis.expression.ExpressionService.GROUP_DATA_ELEMENT;
import static org.hisp.dhis.expression.ExpressionService.GROUP_CATEGORORY_OPTION_COMBO;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.junit.Assert.*;

public class ExpressionPatternTest
{
    @Test
    public void testOperandPattern()
    {
        Matcher matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertNull( matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV}" );
        assertTrue( matcher.find() );        
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.*}" );
        assertTrue( matcher.find() );        
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid.2nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid.kXGiFZ0msNV}" );
        assertFalse( matcher.find() );
        
        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.1nvalidUid}" );
        assertFalse( matcher.find() );
    }
}

package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.regex.Matcher;

import org.junit.Test;

import static org.hisp.dhis.expression.ExpressionService.*;
import static org.hisp.dhis.expression.ExpressionService.GROUP_KEY;
import static org.hisp.dhis.expression.ExpressionService.GROUP_ID;
import static org.hisp.dhis.expression.ExpressionService.GROUP_ID1;
import static org.hisp.dhis.expression.ExpressionService.GROUP_ID2;
import static org.hisp.dhis.expression.ExpressionService.GROUP_DATA_ELEMENT;
import static org.hisp.dhis.expression.ExpressionService.GROUP_CATEGORORY_OPTION_COMBO;
import static org.hisp.dhis.expression.ExpressionService.GROUP_ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ExpressionPatternTest
{
    @Test
    public void testOperandPattern()
    {
        Matcher matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertNull( matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertNull( matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertNull( matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.*}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertNull( matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV.Gb3ZyH0O9M6}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertEquals( "Gb3ZyH0O9M6", matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.*.Gb3ZyH0O9M6}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertEquals( "Gb3ZyH0O9M6", matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV.*}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.*.*}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_ATTRIBUTE_OPTION_COMBO ) );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid.2nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = OPERAND_PATTERN.matcher( "#{1nvalidUid.kXGiFZ0msNV}" );
        assertFalse( matcher.find() );

        matcher = OPERAND_PATTERN.matcher( "#{PuRblkMqsKu.1nvalidUid}" );
        assertFalse( matcher.find() );
    }

    @Test
    public void testVariablePattern()
    {
        Matcher matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu.kXGiFZ0msNV", matcher.group( GROUP_ID ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_ID2 ) );
        assertNull( matcher.group( GROUP_ID3 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu.*}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu.*", matcher.group( GROUP_ID ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_ID2 ) );
        assertNull( matcher.group( GROUP_ID3 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV.Gb3ZyH0O9M6}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu.kXGiFZ0msNV.Gb3ZyH0O9M6", matcher.group( GROUP_ID ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_ID2 ) );
        assertEquals( "Gb3ZyH0O9M6", matcher.group( GROUP_ID3 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu.*.Gb3ZyH0O9M6}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu.*.Gb3ZyH0O9M6", matcher.group( GROUP_ID ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_ID2 ) );
        assertEquals( "Gb3ZyH0O9M6", matcher.group( GROUP_ID3 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV.*}" );
        assertTrue( matcher.find() );
        assertEquals( "#", matcher.group( GROUP_KEY ) );
        assertEquals( "PuRblkMqsKu.kXGiFZ0msNV.*", matcher.group( GROUP_ID ) );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID1 ) );
        assertEquals( "kXGiFZ0msNV", matcher.group( GROUP_ID2 ) );
        assertEquals( SYMBOL_WILDCARD, matcher.group( GROUP_ID3 ) );

        matcher = VARIABLE_PATTERN.matcher( "#{1nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = VARIABLE_PATTERN.matcher( "#{1nvalidUid.2nvalidUid}" );
        assertFalse( matcher.find() );

        matcher = VARIABLE_PATTERN.matcher( "A{sIKbQhhdu8t}" );
        assertTrue( matcher.find() );
        assertEquals( "A", matcher.group( GROUP_KEY ) );
        assertEquals( "sIKbQhhdu8t", matcher.group( GROUP_ID1 ) );

        matcher = VARIABLE_PATTERN.matcher( "I{Fq66TWL10wA}" );
        assertTrue( matcher.find() );
        assertEquals( "I", matcher.group( GROUP_KEY ) );
        assertEquals( "Fq66TWL10wA", matcher.group( GROUP_ID1 ) );

        matcher = VARIABLE_PATTERN.matcher( "D{ZGugB5Dfi9n.Xz9PckXF7Qu}" );
        assertTrue( matcher.find() );
        assertEquals( "D", matcher.group( GROUP_KEY ) );
        assertEquals( "ZGugB5Dfi9n.Xz9PckXF7Qu", matcher.group( GROUP_ID ) );
        assertEquals( "ZGugB5Dfi9n", matcher.group( GROUP_ID1 ) );
        assertEquals( "Xz9PckXF7Qu", matcher.group( GROUP_ID2 ) );
    }

    @Test
    public void testWildcardPattern()
    {
        Matcher matcher = WILDCARD_PATTERN.matcher( "#{PuRblkMqsKu.*}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID ) );

        matcher = WILDCARD_PATTERN.matcher( "#{PuRblkMqsKu.*.*}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID ) );

        matcher = WILDCARD_PATTERN.matcher( "#{PuRblkMqsKu.kXGiFZ0msNV}" );
        assertFalse( matcher.find() );

        matcher = WILDCARD_PATTERN.matcher( "#{PuRblkMqsKu.*.uaIRSFqITG7}" );
        assertFalse( matcher.find() );
        
        matcher = WILDCARD_PATTERN.matcher( "#{PuRblkMqsKu}" );
        assertFalse( matcher.find() );
    }

    @Test
    public void testDataElementTotalPattern()
    {
        Matcher matcher = DATA_ELEMENT_TOTAL_PATTERN.matcher( "#{PuRblkMqsKu}" );
        assertTrue( matcher.find() );
        assertEquals( "PuRblkMqsKu", matcher.group( GROUP_ID ) );
    }

    @Test
    public void testCategoryOptionComboOperandPattern()
    {
        Matcher matcher = CATEGORY_OPTION_COMBO_OPERAND_PATTERN.matcher( "#{ZGugB5Dfi9n.Xz9PckXF7Qu}" );
        assertTrue( matcher.find() );
        assertEquals( "ZGugB5Dfi9n", matcher.group( GROUP_DATA_ELEMENT ) );
        assertEquals( "Xz9PckXF7Qu", matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );
    }

    @Test
    public void testConstantPattern()
    {
        Matcher matcher = CONSTANT_PATTERN.matcher( "C{Jn6Xa61vnic}" );
        assertTrue( matcher.find() );
        assertEquals( "Jn6Xa61vnic", matcher.group( GROUP_ID ) );
    }

    @Test
    public void testOrganisationUnitGroupPattern()
    {
        Matcher matcher = OU_GROUP_PATTERN.matcher( "OUG{a92JkBJm4BY}" );
        assertTrue( matcher.find() );
        assertEquals( "a92JkBJm4BY", matcher.group( GROUP_ID ) );
    }
}

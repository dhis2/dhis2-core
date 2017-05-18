package org.hisp.dhis.commons.util;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.hisp.dhis.commons.util.TextUtils.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.hisp.dhis.commons.util.TextUtils;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class TextUtilsTest
{
    private static final String STRING = "abcdefghij";
    
    @Test
    public void testHtmlLinks()
    {
        assertEquals( "<a href=\"http://dhis2.org\">http://dhis2.org</a>", htmlLinks( "http://dhis2.org" ) );
        assertEquals( "<a href=\"https://dhis2.org\">https://dhis2.org</a>", htmlLinks( "https://dhis2.org" ) );
        assertEquals( "<a href=\"http://www.dhis2.org\">www.dhis2.org</a>", htmlLinks( "www.dhis2.org" ) );        
        assertEquals( "Navigate to <a href=\"http://dhis2.org\">http://dhis2.org</a> or <a href=\"http://www.dhis2.com\">www.dhis2.com</a> to read more.", 
            htmlLinks( "Navigate to http://dhis2.org or www.dhis2.com to read more." ) );
    }
    
    @Test
    public void testSubString()
    {
        assertEquals( "abcdefghij", subString( STRING, 0, 10 ) );        
        assertEquals( "cdef", subString( STRING, 2, 4 ) );
        assertEquals( "ghij", subString( STRING, 6, 4 ) );        
        assertEquals( "ghij", subString( STRING, 6, 6 ) );
        assertEquals( "", subString( STRING, 11, 3 ) );        
        assertEquals( "j", subString( STRING, 9, 1 ) );        
        assertEquals( "", subString( STRING, 4, 0 ) );
    }
    
    @Test
    public void testTrim()
    {
        assertEquals( "abcdefgh", trimEnd( "abcdefghijkl", 4 ) );
    }
    
    @Test
    public void testGetTokens()
    {
        assertEquals( new ArrayList<>( Arrays.asList( "John", "Doe", "Main", "Road", "25" ) ), TextUtils.getTokens( "John Doe Main Road 25" ) );
        assertEquals( new ArrayList<>( Arrays.asList( "Ted,Johnson", "Upper-Road", "45" ) ), TextUtils.getTokens( "Ted,Johnson Upper-Road 45" ) );
    }

    @Test
    public void testRemoveLastOr()
    {
        assertEquals( null, TextUtils.removeLastOr( null ) );
        assertEquals( "", TextUtils.removeLastOr( "" ) );
        assertEquals( "or name='tom' or name='john' ", TextUtils.removeLastOr( "or name='tom' or name='john' or" ) );
        assertEquals( "or name='tom' or name='john' ", TextUtils.removeLastOr( "or name='tom' or name='john' or " ) );
        assertEquals( "or name='tom' or name='john' ", TextUtils.removeLastOr( "or name='tom' or name='john' or  " ) );
    }
    
    @Test
    public void testRemoveLastAnd()
    {
        assertEquals( null, TextUtils.removeLastAnd( null ) );
        assertEquals( "", TextUtils.removeLastAnd( "" ) );
        assertEquals( "and name='tom' and name='john' ", TextUtils.removeLastAnd( "and name='tom' and name='john' and" ) );
        assertEquals( "and name='tom' and name='john' ", TextUtils.removeLastAnd( "and name='tom' and name='john' and " ) );
        assertEquals( "and name='tom' and name='john' ", TextUtils.removeLastAnd( "and name='tom' and name='john' and  " ) );
    }
    
    @Test
    public void testRemoveLastComma()
    {
        assertEquals( null, TextUtils.removeLastComma( null ) );
        assertEquals( "", TextUtils.removeLastComma( "" ) );
        assertEquals( "tom,john", TextUtils.removeLastComma( "tom,john," ) );
        assertEquals( "tom, john", TextUtils.removeLastComma( "tom, john, " ) );
        assertEquals( "tom, john", TextUtils.removeLastComma( "tom, john,  " ) );
    }
    
    @Test
    public void testJoinReplaceNull()
    {
        assertEquals( "green-red-blue", TextUtils.join( Arrays.asList( "green", "red", "blue" ), "-", "[n/a]" ) );
        assertEquals( "green-[n/a]-blue", TextUtils.join( Arrays.asList( "green", null, "blue" ), "-", "[n/a]" ) );
        assertEquals( "green-red-[n/a]", TextUtils.join( Arrays.asList( "green", "red", null ), "-", "[n/a]" ) );
        assertEquals( "greenred[n/a]", TextUtils.join( Arrays.asList( "green", "red", null ), null, "[n/a]" ) );
        assertEquals( "greenred", TextUtils.join( Arrays.asList( "green", "red", null ), null, null ) );
    }
    
    @Test
    public void testSplitSafe()
    {
        assertEquals( "green", TextUtils.splitSafe( "red-green-blue", "-", 1 ) );
        assertEquals( "green", TextUtils.splitSafe( "red.green.blue", "\\.", 1 ) );
        assertEquals( "red", TextUtils.splitSafe( "red-green-blue", "-", 0 ) );
        assertEquals( "blue", TextUtils.splitSafe( "red-green-blue", "-", 2 ) );
        assertNull( TextUtils.splitSafe( "red-green-blue", "-", 3 ) );
        assertNull( TextUtils.splitSafe( "red-green-blue", "-", -2 ) );
        assertNull( TextUtils.splitSafe( "red-green-blue-", "-", 3 ) );        
    }

    @Test
    public void testReplaceFirst()
    {
        assertEquals( "green;red;blue,orange", TextUtils.replaceFirst( "green,red,blue,orange", ",", ";", 2 ) );
        assertEquals( "green.red.blue-orange", TextUtils.replaceFirst( "green-red-blue-orange", "-", ".", 2 ) );
        assertEquals( "llland", TextUtils.replaceFirst( "lalaland", "a", "", 2 ) );
        assertEquals( "mamamand", TextUtils.replaceFirst( "lalaland", "la", "ma", 3 ) );
        assertEquals( "lalaland", TextUtils.replaceFirst( "lalaland", "la", "ma", 0 ) );
    }
    
    @Test
    public void testReplace()
    {        
        String actual = TextUtils.replace( "select * from {table} where {column} = 'Foo'", "{table}", "dataelement", "{column}", "name" );
        
        assertEquals( "select * from dataelement where name = 'Foo'", actual );
        
        actual = TextUtils.replace( "Hi [name] and welcome to [place]", "[name]", "Frank", "[place]", "Oslo" );
        
        assertEquals( "Hi Frank and welcome to Oslo", actual );
    }
}

package org.hisp.dhis.query;

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

import org.hisp.dhis.common.ValueType;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryUtilsTest
{
    @Test
    public void testParseValidEnum()
    {
        assertNotNull( QueryUtils.parseValue( ValueType.class, "INTEGER" ) );
        assertNotNull( QueryUtils.parseValue( ValueType.class, "TEXT" ) );
    }

    @Test
    public void testParseValidInteger()
    {
        Integer value1 = QueryUtils.parseValue( Integer.class, "10" );
        Integer value2 = QueryUtils.parseValue( Integer.class, "100" );

        assertNotNull( value1 );
        assertNotNull( value2 );

        assertSame( 10, value1 );
        assertSame( 100, value2 );
    }

    @Test( expected = QueryParserException.class )
    public void testParseInvalidEnum()
    {
        QueryUtils.parseValue( ValueType.class, "INTEGER" );
        QueryUtils.parseValue( ValueType.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidInteger()
    {
        QueryUtils.parseValue( Integer.class, "1" );
        QueryUtils.parseValue( Integer.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidFloat()
    {
        QueryUtils.parseValue( Float.class, "1.2" );
        QueryUtils.parseValue( Float.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidDouble()
    {
        QueryUtils.parseValue( Double.class, "1.2" );
        QueryUtils.parseValue( Double.class, "ABC" );
    }

    @Test( expected = QueryParserException.class )
    public void testInvalidDate()
    {
        QueryUtils.parseValue( Date.class, "2014" );
        QueryUtils.parseValue( Date.class, "ABC" );
    }
}

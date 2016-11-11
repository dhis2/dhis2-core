package org.hisp.dhis.sqlview;

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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class SqlViewUtilsTest
{
    @Test
    public void testGetVariables()
    {
        String sql = "select * from dataelement where name = '${de_name}' and aggregationType = '${de_aggregation_type}'";

        Set<String> variables = SqlViewUtils.getVariables( sql );

        assertEquals( 2, variables.size() );
        assertTrue( variables.contains( "de_name" ) );
        assertTrue( variables.contains( "de_aggregation_type" ) );
    }

    @Test
    public void testSubsituteSql()
    {
        Map<String, String> variables = new HashMap<>();
        variables.put( "level", "4" );
        variables.put( "id", "abc" );

        String sql = "select * from datavalue where level=${level} and id='${id}'";

        String expected = "select * from datavalue where level=4 and id='abc'";

        String actual = SqlViewUtils.substituteSqlVariables( sql, variables );

        assertEquals( expected, actual );
    }

    @Test
    public void testSubsituteSqlMalicious()
    {
        Map<String, String> variables = new HashMap<>();
        variables.put( "level", "; delete from datavalue;" );

        String sql = "select * from datavalue where level=${level}";

        String expected = "select * from datavalue where level=${level}";

        String actual = SqlViewUtils.substituteSqlVariables( sql, variables );

        assertEquals( expected, actual );
    }
}

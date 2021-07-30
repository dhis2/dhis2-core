/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.commons.jackson.config;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.hisp.dhis.commons.jackson.config.filter.FieldFilterParser;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen
 */
public class FieldFilterParserTest
{
    @Test
    public void testDepth0Filters()
    {
        Set<String> fields = FieldFilterParser.parse( Sets.newHashSet( "id, name", "    abc" ) );

        assertTrue( fields.contains( "id" ) );
        assertTrue( fields.contains( "name" ) );
        assertTrue( fields.contains( "abc" ) );
    }

    @Test
    public void testDepth1Filters()
    {
        Set<String> fields = FieldFilterParser.parse( Sets.newHashSet( "id,name,group[id,name]" ) );

        assertTrue( fields.contains( "id" ) );
        assertTrue( fields.contains( "name" ) );
        assertTrue( fields.contains( "group.id" ) );
        assertTrue( fields.contains( "group.name" ) );
    }

    @Test
    public void testDepthXFilters()
    {
        Set<String> fields = FieldFilterParser
            .parse( Sets.newHashSet( "id,name,group[id,name]", "group[id,name,group[id,name,group[id,name]]]" ) );

        assertTrue( fields.contains( "id" ) );
        assertTrue( fields.contains( "name" ) );
        assertTrue( fields.contains( "group.id" ) );
        assertTrue( fields.contains( "group.name" ) );
        assertTrue( fields.contains( "group.group.id" ) );
        assertTrue( fields.contains( "group.group.name" ) );
        assertTrue( fields.contains( "group.group.group.id" ) );
        assertTrue( fields.contains( "group.group.group.name" ) );
    }

    @Test
    public void testOnlyBlockFilters()
    {
        Set<String> fields = FieldFilterParser.parse( Sets.newHashSet( "group[id,name]" ) );

        assertTrue( fields.contains( "group.id" ) );
        assertTrue( fields.contains( "group.name" ) );
    }

    @Test
    public void testOnlySpringBlockFilters()
    {
        Set<String> fields = FieldFilterParser.parse( Sets.newHashSet( "group[id", "name]" ) );

        assertTrue( fields.contains( "group.id" ) );
        assertTrue( fields.contains( "group.name" ) );
    }

    @Test
    public void testParseWithPrefix1()
    {
        Set<String> fields = FieldFilterParser.parseWithPrefix( Sets.newHashSet( "a", "b" ), "prefix" );

        assertTrue( fields.contains( "prefix.a" ) );
        assertTrue( fields.contains( "prefix.b" ) );
    }

    @Test
    public void testParseWithPrefix2()
    {
        Set<String> fields = FieldFilterParser.parseWithPrefix( Sets.newHashSet( "aaa[a],bbb[b]" ), "prefix" );

        assertTrue( fields.contains( "prefix.aaa.a" ) );
        assertTrue( fields.contains( "prefix.bbb.b" ) );
    }
}

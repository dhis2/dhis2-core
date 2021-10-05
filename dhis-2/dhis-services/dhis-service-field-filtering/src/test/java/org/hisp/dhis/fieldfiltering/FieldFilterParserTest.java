/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "id, name", "    abc" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "abc" );
    }

    @Test
    public void testDepth1Filters()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "id,name,group[id,name]" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "group.id" );
        assertFieldPathContains( fieldPaths, "group.name" );
    }

    @Test
    public void testDepthXFilters()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "id,name,group[id,name]", "group[id,name,group[id,name,group[id,name]]]" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "group.id" );
        assertFieldPathContains( fieldPaths, "group.name" );
        assertFieldPathContains( fieldPaths, "group.group.id" );
        assertFieldPathContains( fieldPaths, "group.group.name" );
        assertFieldPathContains( fieldPaths, "group.group.group.id" );
        assertFieldPathContains( fieldPaths, "group.group.group.name" );
    }

    @Test
    public void testOnlyBlockFilters()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "group[id,name]" ) );

        assertFieldPathContains( fieldPaths, "group.id" );
        assertFieldPathContains( fieldPaths, "group.name" );
    }

    @Test
    public void testOnlySpringBlockFilters()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "group[id", "name]" ) );

        assertFieldPathContains( fieldPaths, "group.id" );
        assertFieldPathContains( fieldPaths, "group.name" );
    }

    @Test
    public void testParseWithPrefix1()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix( Sets.newHashSet( "a", "b" ), "prefix" );

        assertFieldPathContains( fieldPaths, "prefix.a" );
        assertFieldPathContains( fieldPaths, "prefix.b" );
    }

    @Test
    public void testParseWithPrefix2()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix( Sets.newHashSet( "aaa[a],bbb[b]" ), "prefix" );

        assertFieldPathContains( fieldPaths, "prefix.aaa.a" );
        assertFieldPathContains( fieldPaths, "prefix.bbb.b" );
    }

    @Test
    public void testParseWithTransformer1()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "name::x(a;b),id~y(a;b;c),code|z(t)" ) );

        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "code" );
    }

    @Test
    public void testParseWithTransformer2()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "groups[name::x(a;b)]" ) );

        assertFieldPathContains( fieldPaths, "groups" );
        assertFieldPathContains( fieldPaths, "groups.name" );
    }

    @Test
    public void testParseWithTransformer3()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "groups[name::x(a;b), code~y(a)]" ) );

        assertFieldPathContains( fieldPaths, "groups" );
        assertFieldPathContains( fieldPaths, "groups.name" );
        assertFieldPathContains( fieldPaths, "groups.code" );
    }

    @Test
    public void testParseWithTransformer4()
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( Sets.newHashSet( "name::rename(n),groups[name]" ) );

        assertFieldPathContains( fieldPaths, "name", true );
        assertFieldPathContains( fieldPaths, "groups" );
        assertFieldPathContains( fieldPaths, "groups.name", false );
    }

    @Test
    public void testParseWithTransformer5()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "name::rename(n),groups::rename(g)[name::rename(n)]" ) );

        assertFieldPathContains( fieldPaths, "name", true );
        assertFieldPathContains( fieldPaths, "groups", true );
        assertFieldPathContains( fieldPaths, "groups.name", true );
    }

    @Test
    public void testParseWithTransformer6()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "name::rename(n),groups::rename(g)[name]" ) );

        assertFieldPathContains( fieldPaths, "name", true );
        assertFieldPathContains( fieldPaths, "groups", true );
        assertFieldPathContains( fieldPaths, "groups.name", false );
    }

    @Test
    public void testParseWithTransformer7()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "name::size,group::isEmpty" ) );

        assertFieldPathContains( fieldPaths, "name", true );
        assertFieldPathContains( fieldPaths, "group", true );
    }

    @Test
    public void testParseWithTransformer8()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "name::rename(n)" ) );

        assertFieldPathContains( fieldPaths, "name", true );

        FieldPathTransformer fieldPathTransformer = fieldPaths.get( 0 ).getTransformers().get( 0 );
        assertEquals( "rename", fieldPathTransformer.getName() );
    }

    @Test
    public void testParseWithMultipleTransformers()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "name::size::rename(n)" ) );

        assertFieldPathContains( fieldPaths, "name", true );

        FieldPathTransformer fieldPathTransformer = fieldPaths.get( 0 ).getTransformers().get( 0 );
        assertEquals( "size", fieldPathTransformer.getName() );

        fieldPathTransformer = fieldPaths.get( 0 ).getTransformers().get( 1 );
        assertEquals( "rename", fieldPathTransformer.getName() );
    }

    @Test
    public void testParseWithExclusions1()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "id,!code,name" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "!code" );
        assertFieldPathContains( fieldPaths, "name" );
    }

    @Test
    public void testParseWithExclusions2()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "id,!code,name,group[id,!name]" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "!code" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "group.id" );
        assertFieldPathContains( fieldPaths, "group.!name" );
    }

    @Test
    public void testParseWithPreset1()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "id,name,!code,:owner" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "!code" );
        assertFieldPathContains( fieldPaths, ":owner" );
    }

    @Test
    public void testParseWithPreset2()
    {
        List<FieldPath> fieldPaths = FieldFilterParser
            .parse( Sets.newHashSet( "id,name,!code,:owner,group[:owner,:all]" ) );

        assertFieldPathContains( fieldPaths, "id" );
        assertFieldPathContains( fieldPaths, "name" );
        assertFieldPathContains( fieldPaths, "!code" );
        assertFieldPathContains( fieldPaths, ":owner" );
        assertFieldPathContains( fieldPaths, "group.:owner" );
        assertFieldPathContains( fieldPaths, "group.:all" );
    }

    private void assertFieldPathContains( List<FieldPath> fieldPaths, String expected, boolean hasTransformer )
    {
        boolean condition = false;

        for ( FieldPath fieldPath : fieldPaths )
        {
            String path = fieldPath.toFullPath();

            if ( path.startsWith( expected ) )
            {
                condition = !fieldPath.getTransformers().isEmpty() == hasTransformer;
                break;
            }
        }

        assertTrue( condition );
    }

    private void assertFieldPathContains( List<FieldPath> fieldPaths, String expected )
    {
        boolean condition = false;

        for ( FieldPath fieldPath : fieldPaths )
        {
            String path = fieldPath.toFullPath();

            if ( path.startsWith( expected ) )
            {
                condition = true;
                break;
            }
        }

        assertTrue( condition );
    }
}

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
package org.hisp.dhis.schema;

import static org.hamcrest.Matchers.contains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Schema}.
 *
 * @author Volker Schmidt
 */
public class SchemaTest
{
    private List<Authority> authorities;

    @Before
    public void setUp()
    {
        authorities = new ArrayList<>();
        authorities.add( new Authority( AuthorityType.CREATE, Arrays.asList( "x1", "x2" ) ) );
        authorities.add( new Authority( AuthorityType.CREATE, Arrays.asList( "y1", "y2" ) ) );
        authorities.add( new Authority( AuthorityType.DELETE, Arrays.asList( "z1", "z2" ) ) );
    }

    @Test
    public void isSecondaryMetadataObject()
    {
        Assert.assertTrue( new Schema( SecondaryMetadata.class, "singular", "plural" ).isSecondaryMetadata() );
    }

    @Test
    public void isSecondaryMetadataObjectMetadata()
    {
        Assert.assertTrue( new Schema( SecondaryMetadata.class, "singular", "plural" ).isMetadata() );
    }

    @Test
    public void isSecondaryMetadataObjectNot()
    {
        Assert.assertFalse( new Schema( Metadata.class, "singular", "plural" ).isSecondaryMetadata() );
    }

    @Test
    public void testAuthorityByType()
    {
        final Schema schema = new Schema( SecondaryMetadata.class, "singular", "plural" );
        schema.setAuthorities( authorities );

        List<String> list1 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list1, contains( "x1", "x2", "y1", "y2" ) );

        List<String> list2 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list2, contains( "x1", "x2", "y1", "y2" ) );
        Assert.assertSame( list1, list2 );
    }

    @Test
    public void testAuthorityByTypeDifferent()
    {
        final Schema schema = new Schema( SecondaryMetadata.class, "singular", "plural" );
        schema.setAuthorities( authorities );

        List<String> list1 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list1, contains( "x1", "x2", "y1", "y2" ) );

        List<String> list3 = schema.getAuthorityByType( AuthorityType.DELETE );
        Assert.assertThat( list3, contains( "z1", "z2" ) );

        List<String> list2 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list2, contains( "x1", "x2", "y1", "y2" ) );
        Assert.assertSame( list1, list2 );
    }

    @Test
    public void testAuthorityByTypeNotFound()
    {
        final Schema schema = new Schema( SecondaryMetadata.class, "singular", "plural" );
        schema.setAuthorities( authorities );

        List<String> list1 = schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE );
        Assert.assertTrue( list1.isEmpty() );

        List<String> list2 = schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE );
        Assert.assertTrue( list2.isEmpty() );
        Assert.assertSame( list1, list2 );
    }

    @Test
    public void testAuthorityByTypeReset()
    {
        final Schema schema = new Schema( SecondaryMetadata.class, "singular", "plural" );
        schema.setAuthorities( authorities );

        List<String> list1 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list1, contains( "x1", "x2", "y1", "y2" ) );

        authorities.add( new Authority( AuthorityType.CREATE, Arrays.asList( "a1", "a2" ) ) );
        schema.setAuthorities( authorities );

        List<String> list2 = schema.getAuthorityByType( AuthorityType.CREATE );
        Assert.assertThat( list2, contains( "x1", "x2", "y1", "y2", "a1", "a2" ) );
        Assert.assertNotSame( list1, list2 );
    }

    private static class SecondaryMetadata implements SecondaryMetadataObject
    {
    }

    private static class Metadata implements MetadataObject
    {
    }
}
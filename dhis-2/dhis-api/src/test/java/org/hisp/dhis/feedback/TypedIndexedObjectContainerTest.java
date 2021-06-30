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
package org.hisp.dhis.feedback;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link TypedIndexedObjectContainer}.
 *
 * @author Volker Schmidt
 */
public class TypedIndexedObjectContainerTest
{
    private final TypedIndexedObjectContainer container = new TypedIndexedObjectContainer();

    @Test
    public void getContainerNew()
    {
        IndexedObjectContainer container1 = container.getTypedContainer( Attribute.class );
        Assert.assertSame( container1, container.getTypedContainer( Attribute.class ) );

        IndexedObjectContainer container2 = container.getTypedContainer( Category.class );
        Assert.assertSame( container2, container.getTypedContainer( Category.class ) );
        Assert.assertNotSame( container1, container2 );

        Assert.assertSame( container1, container.getTypedContainer( Attribute.class ) );
    }

    @Test
    public void getContainerExisting()
    {
        final Attribute attribute1 = new Attribute();
        final Category category1 = new Category();

        container.mergeObjectIndex( new Attribute() );
        container.mergeObjectIndex( new Category() );
        container.mergeObjectIndex( attribute1 );
        container.mergeObjectIndex( category1 );

        IndexedObjectContainer container1 = container.getTypedContainer( Attribute.class );
        Assert.assertSame( 1, container1.mergeObjectIndex( attribute1 ) );

        IndexedObjectContainer container2 = container.getTypedContainer( Category.class );
        Assert.assertSame( 1, container2.mergeObjectIndex( category1 ) );
    }

    @Test
    public void merge()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();
        final Attribute attribute3 = new Attribute();
        final Category category1 = new Category();
        final Category category2 = new Category();

        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( attribute1 ) );
        Assert.assertEquals( (Integer) 1, container.mergeObjectIndex( attribute2 ) );
        Assert.assertEquals( (Integer) 2, container.mergeObjectIndex( attribute3 ) );
        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( category1 ) );
        Assert.assertEquals( (Integer) 1, container.mergeObjectIndex( category2 ) );
    }

    @Test
    public void add()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();
        final Attribute attribute3 = new Attribute();
        final Category category1 = new Category();
        final Category category2 = new Category();

        container.add( attribute1 );
        container.add( attribute2 );
        container.add( attribute3 );
        container.add( category1 );
        container.add( category2 );

        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( attribute1 ) );
        Assert.assertEquals( (Integer) 1, container.mergeObjectIndex( attribute2 ) );
        Assert.assertEquals( (Integer) 2, container.mergeObjectIndex( attribute3 ) );
        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( category1 ) );
        Assert.assertEquals( (Integer) 1, container.mergeObjectIndex( category2 ) );
    }

    @Test
    public void containsObjectNoType()
    {
        final Category category1 = new Category();
        final Category category2 = new Category();

        container.add( category1 );
        container.add( category2 );

        Assert.assertFalse( container.containsObject( new Attribute() ) );
    }

    @Test
    public void containsObjectNot()
    {
        final Category category1 = new Category();
        final Category category2 = new Category();

        container.add( category1 );
        container.add( category2 );

        Assert.assertFalse( container.containsObject( new Category() ) );
    }

    @Test
    public void containsObject()
    {
        final Category category1 = new Category();
        final Category category2 = new Category();

        container.add( category1 );
        container.add( category2 );

        Assert.assertTrue( container.containsObject( category1 ) );
    }
}
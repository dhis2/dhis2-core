package org.hisp.dhis.feedback;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.attribute.Attribute;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link IndexedObjectContainer}.
 *
 * @author Volker Schmidt
 */
public class IndexedObjectContainerTest
{
    private final IndexedObjectContainer container = new IndexedObjectContainer();

    @Test
    public void merge()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();
        final Attribute attribute3 = new Attribute();

        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( attribute1 ) );
        Assert.assertEquals( (Integer) 1, container.mergeObjectIndex( attribute2 ) );
        Assert.assertEquals( (Integer) 0, container.mergeObjectIndex( attribute1 ) );
        Assert.assertEquals( (Integer) 2, container.mergeObjectIndex( attribute3 ) );
    }

    @Test
    public void add()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();
        final Attribute attribute3 = new Attribute();

        Assert.assertEquals( (Integer) 0, container.add( attribute1 ) );
        Assert.assertEquals( (Integer) 1, container.add( attribute2 ) );
        Assert.assertEquals( (Integer) 0, container.add( attribute1 ) );
        Assert.assertEquals( (Integer) 2, container.add( attribute3 ) );
    }

    @Test
    public void containsObject()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();

        container.add( attribute1 );
        container.add( attribute2 );

        Assert.assertTrue( container.containsObject( attribute2 ) );
    }

    @Test
    public void containsObjectNot()
    {
        final Attribute attribute1 = new Attribute();
        final Attribute attribute2 = new Attribute();

        container.add( attribute1 );
        container.add( attribute2 );

        Assert.assertFalse( container.containsObject( new Attribute() ) );
    }
}
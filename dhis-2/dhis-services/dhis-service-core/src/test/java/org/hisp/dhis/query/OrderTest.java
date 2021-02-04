package org.hisp.dhis.query;

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

import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.schema.Property;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyDescriptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Unit tests for {@link Order}.
 *
 * @author Volker Schmidt
 */
public class OrderTest
{
    private TestObject object1;

    private TestObject object2;

    private Property valueProperty;

    private Order orderAsc;

    private Order orderDesc;

    @Before
    public void setUp() throws Exception
    {
        object1 = new TestObject();
        object2 = new TestObject();
        PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor( object1, "value" );
        valueProperty = new Property( String.class, propertyDescriptor.getReadMethod(), propertyDescriptor.getWriteMethod() );
        valueProperty.setName( "value" );
        orderAsc = new Order( valueProperty, Direction.ASCENDING );
        orderDesc = new Order( valueProperty, Direction.DESCENDING );
    }

    @Test
    public void bothNull()
    {
        Assert.assertEquals( 0, orderAsc.compare( object1, object2 ) );
        Assert.assertEquals( 0, orderAsc.compare( object2, object1 ) );
    }

    @Test
    public void leftNullAsc()
    {
        object2.setValue( "Test" );
        Assert.assertEquals( 1, orderAsc.compare( object1, object2 ) );
    }

    @Test
    public void rightNullAsc()
    {
        object1.setValue( "Test" );
        Assert.assertEquals( -1, orderAsc.compare( object1, object2 ) );
    }

    @Test
    public void leftNullDesc()
    {
        object2.setValue( "Test" );
        Assert.assertEquals( -1, orderDesc.compare( object1, object2 ) );
    }

    @Test
    public void rightNullDesc()
    {
        object1.setValue( "Test" );
        Assert.assertEquals( 1, orderDesc.compare( object1, object2 ) );
    }

    @Test
    public void bothNonNullAsc()
    {
        object1.setValue( "Test1" );
        object2.setValue( "Test2" );
        assertThat( orderAsc.compare( object1, object2 ), lessThan( 0 ) );
    }

    @Test
    public void toOrderStringAsc()
    {
        Assert.assertEquals( "value:asc", Order.from( "asc", valueProperty ).toOrderString() );
    }

    @Test
    public void toOrderStringDesc()
    {
        Assert.assertEquals( "value:desc", Order.from( "desc", valueProperty ).toOrderString() );
    }

    @Test
    public void toOrderStringIAsc()
    {
        Assert.assertEquals( "value:iasc", Order.from( "iasc", valueProperty ).toOrderString() );
    }

    @Test
    public void toOrderStringIDesc()
    {
        Assert.assertEquals( "value:idesc", Order.from( "idesc", valueProperty ).toOrderString() );
    }

    public static class TestObject
    {
        private String value;

        public String getValue()
        {
            return value;
        }

        public void setValue( String value )
        {
            this.value = value;
        }
    }
}
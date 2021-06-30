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
package org.hisp.dhis.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.hisp.dhis.schema.Property;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link JpaQueryUtils}.
 *
 * @author volsch
 */
public class JpaQueryUtilsTest
{
    @Test
    public void createOrderExpressionNull()
    {
        Assert.assertNull( JpaQueryUtils.createOrderExpression( null, null ) );
    }

    @Test
    public void createSelectOrderExpressionNull()
    {
        Assert.assertNull( JpaQueryUtils.createSelectOrderExpression( null, null ) );
    }

    @Test
    public void createOrderExpressionEmpty()
    {
        Assert.assertNull( JpaQueryUtils.createOrderExpression( new ArrayList<>(), null ) );
    }

    @Test
    public void createOrderExpressionNoPersistent()
    {
        final Property property = new Property();
        property.setName( "valueTest" );
        property.setSimple( true );
        property.setPersisted( false );
        Assert.assertNull( JpaQueryUtils
            .createOrderExpression( Collections.singletonList( new Order( property, Direction.ASCENDING ) ), null ) );
    }

    @Test
    public void createSelectOrderExpressionNoPersistent()
    {
        final Property property = new Property();
        property.setName( "valueTest" );
        property.setSimple( true );
        property.setPersisted( false );
        Assert.assertNull( JpaQueryUtils.createSelectOrderExpression(
            Collections.singletonList( new Order( property, Direction.ASCENDING ) ), null ) );
    }

    @Test
    public void createOrderExpression()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setPersisted( true );

        final Property property2 = new Property();
        property2.setName( "value2" );
        property2.setSimple( true );
        property2.setPersisted( false );

        final Property property3 = new Property();
        property3.setName( "value3" );
        property3.setSimple( true );
        property3.setKlass( Integer.class );
        property3.setPersisted( true );

        final Property property4 = new Property();
        property4.setName( "value4" );
        property4.setSimple( true );
        property4.setPersisted( true );
        property4.setKlass( String.class );

        final Property property5 = new Property();
        property5.setName( "value5" );
        property5.setSimple( true );
        property5.setPersisted( true );

        Assert.assertEquals( "value1 asc,value3 asc,lower(value4) asc,value5 desc",
            JpaQueryUtils.createOrderExpression( Arrays.asList(
                new Order( property1, Direction.ASCENDING ),
                new Order( property2, Direction.ASCENDING ),
                new Order( property3, Direction.ASCENDING ).ignoreCase(),
                new Order( property4, Direction.ASCENDING ).ignoreCase(),
                new Order( property5, Direction.DESCENDING ) ), null ) );
    }

    @Test
    public void createSelectOrderExpression()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setPersisted( true );
        property1.setKlass( String.class );

        final Property property2 = new Property();
        property2.setName( "value2" );
        property2.setSimple( true );
        property2.setPersisted( false );

        final Property property3 = new Property();
        property3.setName( "value3" );
        property3.setSimple( true );
        property3.setKlass( Integer.class );
        property3.setPersisted( true );

        final Property property4 = new Property();
        property4.setName( "value4" );
        property4.setSimple( true );
        property4.setPersisted( true );
        property4.setKlass( String.class );

        final Property property5 = new Property();
        property5.setName( "value5" );
        property5.setSimple( true );
        property5.setPersisted( true );

        Assert.assertEquals( "lower(value1),lower(value4)", JpaQueryUtils.createSelectOrderExpression( Arrays.asList(
            new Order( property1, Direction.ASCENDING ).ignoreCase(),
            new Order( property2, Direction.ASCENDING ),
            new Order( property3, Direction.ASCENDING ).ignoreCase(),
            new Order( property4, Direction.ASCENDING ).ignoreCase(),
            new Order( property5, Direction.DESCENDING ) ), null ) );
    }

    @Test
    public void createOrderExpressionSingle()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setPersisted( true );

        Assert.assertEquals( "value1 asc", JpaQueryUtils
            .createOrderExpression( Collections.singletonList( new Order( property1, Direction.ASCENDING ) ), null ) );
    }

    @Test
    public void createSelectOrderExpressionSingle()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setKlass( String.class );
        property1.setPersisted( true );

        Assert.assertEquals( "lower(value1)", JpaQueryUtils.createSelectOrderExpression(
            Collections.singletonList( new Order( property1, Direction.ASCENDING ).ignoreCase() ), null ) );
    }

    @Test
    public void createOrderExpressionAlias()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setPersisted( true );

        Assert.assertEquals( "x.value1 asc", JpaQueryUtils
            .createOrderExpression( Collections.singletonList( new Order( property1, Direction.ASCENDING ) ), "x" ) );
    }

    @Test
    public void createSelectOrderExpressionAlias()
    {
        final Property property1 = new Property();
        property1.setName( "value1" );
        property1.setSimple( true );
        property1.setKlass( String.class );
        property1.setPersisted( true );

        Assert.assertEquals( "lower(x.value1)", JpaQueryUtils.createSelectOrderExpression(
            Collections.singletonList( new Order( property1, Direction.ASCENDING ).ignoreCase() ), "x" ) );
    }
}
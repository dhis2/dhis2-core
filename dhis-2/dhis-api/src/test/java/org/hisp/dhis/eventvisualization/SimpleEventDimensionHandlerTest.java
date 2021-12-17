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
package org.hisp.dhis.eventvisualization;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.SimpleEventDimension.Type.EVENT_DATE;
import static org.hisp.dhis.eventvisualization.SimpleEventDimension.Type.INCIDENT_DATE;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.junit.Test;

/**
 * Unit tests for {@link SimpleEventDimensionHandler}.
 *
 * @author maikel arabori
 */
public class SimpleEventDimensionHandlerTest
{
    @Test
    public void testGetDimensionalObject()
    {
        // Given
        final EventVisualization aEventVisualization = stubEventVisualization();
        final String eventDateDimension = EVENT_DATE.getDimension();
        final Attribute column = COLUMN;
        final SimpleEventDimensionHandler handler = new SimpleEventDimensionHandler( aEventVisualization );

        // When
        final DimensionalObject dimensionalObject = handler.getDimensionalObject( eventDateDimension, column );

        // Then
        assertThat( dimensionalObject.getUid(), is( equalTo( eventDateDimension ) ) );
        assertThat( dimensionalObject.getDimensionType(), is( equalTo( PERIOD ) ) );
        assertThat( dimensionalObject.getItems(), hasSize( 2 ) );
    }

    @Test
    public void testGetDimensionalObjectWhenDimensionIsNotValid()
    {
        // Given
        final EventVisualization aEventVisualization = stubEventVisualization();
        final String invalidDimension = "invalidDimension";
        final Attribute column = COLUMN;
        final SimpleEventDimensionHandler handler = new SimpleEventDimensionHandler( aEventVisualization );

        // When throws
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> handler.getDimensionalObject( invalidDimension, column ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Not a valid dimension" ) );
    }

    @Test
    public void testAssociateDimensions()
    {
        // Given
        final String eventDateDimension = EVENT_DATE.getDimension();
        final EventVisualization aEventVisualization = stubEventVisualization();
        aEventVisualization.getSimpleEventDimensions().clear();
        aEventVisualization.getColumns().addAll( asList( new BaseDimensionalObject( eventDateDimension ) ) );

        final SimpleEventDimensionHandler handler = new SimpleEventDimensionHandler( aEventVisualization );

        // When
        handler.associateDimensions();

        // Then
        assertThat( aEventVisualization.getSimpleEventDimensions(), hasSize( 1 ) );
        assertThat( aEventVisualization.getSimpleEventDimensions().get( 0 ).getParent(), is( equalTo( COLUMN ) ) );
        assertThat( aEventVisualization.getSimpleEventDimensions().get( 0 ).getDimension(),
            is( equalTo( eventDateDimension ) ) );
    }

    private EventVisualization stubEventVisualization()
    {
        final EventVisualization eventVisualization = new EventVisualization();

        eventVisualization.setSimpleEventDimensions( stubSimpleEventDimensions() );

        return eventVisualization;
    }

    private List<SimpleEventDimension> stubSimpleEventDimensions()
    {
        final List<SimpleEventDimension> dimensions = new ArrayList<>();
        dimensions.add( stubSimpleEventDimension( EVENT_DATE.getDimension() ) );
        dimensions.add( stubSimpleEventDimension( INCIDENT_DATE.getDimension() ) );

        return dimensions;
    }

    private SimpleEventDimension stubSimpleEventDimension( final String dimension )
    {
        final SimpleEventDimension simpleEventDimension = new SimpleEventDimension();

        simpleEventDimension.setDimension( dimension );
        simpleEventDimension.setParent( COLUMN );
        simpleEventDimension.setValues( asList( "value1", "value2" ) );

        return simpleEventDimension;
    }
}

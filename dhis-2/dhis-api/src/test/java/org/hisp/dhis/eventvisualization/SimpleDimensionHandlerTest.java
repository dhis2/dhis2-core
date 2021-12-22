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
import static java.util.List.of;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.EVENT_DATE;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.INCIDENT_DATE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimpleDimensionHandler}.
 *
 * @author maikel arabori
 */
class SimpleDimensionHandlerTest
{

    @Test
    void testGetDimensionalObject()
    {
        // Given
        final EventVisualization aEventVisualization = stubEventVisualization();
        final String eventDateDimension = EVENT_DATE.getDimension();
        final SimpleDimensionHandler handler = new SimpleDimensionHandler( aEventVisualization );

        // When
        final DimensionalObject dimensionalObject = handler.getDimensionalObject( eventDateDimension, COLUMN );

        // Then
        assertThat( dimensionalObject.getUid(), is( equalTo( eventDateDimension ) ) );
        assertThat( dimensionalObject.getDimensionType(), is( equalTo( PERIOD ) ) );
        assertThat( dimensionalObject.getItems(), hasSize( 2 ) );
    }

    @Test
    void testGetDimensionalObjectWhenDimensionIsNotValid()
    {
        // Given
        final EventVisualization aEventVisualization = stubEventVisualization();
        final String invalidDimension = "invalidDimension";
        final SimpleDimensionHandler handler = new SimpleDimensionHandler( aEventVisualization );

        // When throws
        final NoSuchElementException thrown = assertThrows( NoSuchElementException.class,
            () -> handler.getDimensionalObject( invalidDimension, COLUMN ) );

        // Then
        assertThat( thrown.getMessage(), containsString( "Invalid dimension: " + invalidDimension ) );
    }

    @Test
    void testAssociateDimensions()
    {
        // Given
        final String eventDateDimension = EVENT_DATE.getDimension();
        final EventVisualization aEventVisualization = stubEventVisualization();
        aEventVisualization.getSimpleDimensions().clear();
        aEventVisualization.getColumns().addAll( of( new BaseDimensionalObject( eventDateDimension ) ) );

        final SimpleDimensionHandler handler = new SimpleDimensionHandler( aEventVisualization );

        // When
        handler.associateDimensions();

        // Then
        assertThat( aEventVisualization.getSimpleDimensions(), hasSize( 1 ) );
        assertThat( aEventVisualization.getSimpleDimensions().get( 0 ).getParent(), is( equalTo( COLUMN ) ) );
        assertThat( aEventVisualization.getSimpleDimensions().get( 0 ).getDimension(),
            is( equalTo( eventDateDimension ) ) );
    }

    private EventVisualization stubEventVisualization()
    {
        final EventVisualization eventVisualization = new EventVisualization();

        eventVisualization.setSimpleDimensions( stubSimpleEventDimensions() );

        return eventVisualization;
    }

    private List<SimpleDimension> stubSimpleEventDimensions()
    {
        final List<SimpleDimension> dimensions = new ArrayList<>();
        dimensions.add( stubSimpleEventDimension( EVENT_DATE.getDimension() ) );
        dimensions.add( stubSimpleEventDimension( INCIDENT_DATE.getDimension() ) );

        return dimensions;
    }

    private SimpleDimension stubSimpleEventDimension( final String dimension )
    {
        final SimpleDimension simpleDimension = new SimpleDimension();

        simpleDimension.setDimension( dimension );
        simpleDimension.setParent( COLUMN );
        simpleDimension.setValues( asList( "value1", "value2" ) );

        return simpleDimension;
    }
}

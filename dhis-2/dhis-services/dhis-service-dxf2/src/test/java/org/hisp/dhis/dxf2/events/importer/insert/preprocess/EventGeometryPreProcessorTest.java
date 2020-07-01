package org.hisp.dhis.dxf2.events.importer.insert.preprocess;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.system.util.GeoUtils;
import org.junit.Before;
import org.junit.Test;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

/**
 * @author Luciano Fiandesio
 */
public class EventGeometryPreProcessorTest
{
    private EventGeometryPreProcessor subject;

    @Before
    public void setUp()
    {

        this.subject = new EventGeometryPreProcessor();
    }

    @Test
    public void verifyEventGeometryGetCorrectSRID()
        throws IOException
    {
        Event event = new Event();
        event.setGeometry( GeoUtils.getGeoJsonPoint( 20.0, 30.0 ) );
        event.getGeometry().setSRID( 0 );
        subject.process( event, WorkContext.builder().build() );

        assertThat( event.getGeometry().getSRID(), is( GeoUtils.SRID ) );
    }

    @Test
    public void verifyEventWithCoordinateHasGeometrySet()
    {
        Event event = new Event();
        event.setCoordinate( new Coordinate( 20.0, 22.0 ) );
        subject.process( event, WorkContext.builder().build() );

        assertThat( event.getGeometry().getSRID(), is( GeoUtils.SRID ) );
        assertThat( event.getGeometry().getCoordinate().x, is( 20.0 ) );
        assertThat( event.getGeometry().getCoordinate().y, is( 22.0 ) );
    }
}
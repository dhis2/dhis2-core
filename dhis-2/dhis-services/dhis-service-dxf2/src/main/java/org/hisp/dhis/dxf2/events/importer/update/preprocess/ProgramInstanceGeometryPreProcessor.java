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
package org.hisp.dhis.dxf2.events.importer.update.preprocess;

import static org.hisp.dhis.organisationunit.FeatureType.NONE;
import static org.hisp.dhis.system.util.GeoUtils.SRID;
import static org.hisp.dhis.system.util.GeoUtils.getGeoJsonPoint;

import java.io.IOException;

import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;

/**
 * @author maikel arabori
 */
public class ProgramInstanceGeometryPreProcessor implements Processor
{
    @Override
    public void process( final Event event, final WorkContext ctx )
    {
        ctx.getProgramStageInstance( event.getUid() ).ifPresent( psi -> {

            if ( event.getGeometry() != null )
            {
                if ( !psi.getProgramStage().getFeatureType().equals( NONE ) || psi
                    .getProgramStage().getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
                {
                    event.getGeometry().setSRID( SRID );
                }
            }
            else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
            {
                final Coordinate coordinate = event.getCoordinate();

                try
                {
                    event.setGeometry( getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
                }
                catch ( IOException e )
                {
                    // Do nothing. The validation phase, before the post process
                    // phase, will catch
                    // it in advance. It should never happen at this stage.
                }
            }
        } );
    }
}

package org.hisp.dhis.mapgeneration;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.awt.image.BufferedImage;
import java.util.Date;

import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * The MapGenerationService interface generates map images from Map objects.
 * 
 * Map objects may be built by adding layers to them, and once passed to
 * generateMapImage it will render an image representing the map according to
 * the properties defined by Map and MapView.
 * 
 * TODO Extend with more configuration options, e.g. width
 * 
 * @author Kenneth Solbø Andersen <kennetsa@ifi.uio.no>
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public interface MapGenerationService
{
    String ID = MapGenerationService.class.getName();
    
    /**
     * Generate an image that represents this map.
     * 
     * @param mapView the map view that will be rendered,
     * @return the rendered map image or null if there is no data for the map view.
     */
    BufferedImage generateMapImage( MapView mapView );
    
    /**
     * Generate an image that represents this map.
     * 
     * @param map the map that will be rendered,
     * @return the rendered map image or null if there is no data for the map view.
     */
    BufferedImage generateMapImage( Map map );

    /**
     * Generate an image that represents this map.
     * 
     * @param map the map that will be rendered.
     * @param date the date for relative periods.
     * @param unit the organisation unit.
     * @param width the maximum width of the map image.
     * @param height the maximum height of the map image.
     * @return the rendered map image or null if there is no data for the map view.
     */
    BufferedImage generateMapImage( Map map, Date date, OrganisationUnit unit, Integer width, Integer height );
}

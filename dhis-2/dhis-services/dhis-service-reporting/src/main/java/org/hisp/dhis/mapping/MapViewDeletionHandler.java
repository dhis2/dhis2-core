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
package org.hisp.dhis.mapping;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.List;

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.common.GenericAnalyticalObjectDeletionHandler;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component( "org.hisp.dhis.mapping.MapViewDeletionHandler" )
public class MapViewDeletionHandler
    extends GenericAnalyticalObjectDeletionHandler<MapView>
{
    private final MappingService mappingService;

    public MapViewDeletionHandler( MappingService mappingService )
    {
        super( new DeletionVeto( MapView.class ) );
        checkNotNull( mappingService );
        this.mappingService = mappingService;
    }

    @Override
    protected AnalyticalObjectService<MapView> getAnalyticalObjectService()
    {
        return mappingService;
    }

    @Override
    protected void register()
    {
        super.register();
        whenDeleting( LegendSet.class, this::deleteLegendSet );
        whenDeleting( OrganisationUnitGroupSet.class, this::deleteOrganisationUnitGroupSet );
        whenVetoing( MapView.class, this::allowDeleteMapView );

    }

    private void deleteLegendSet( LegendSet legendSet )
    {
        List<MapView> mapViews = mappingService.getAnalyticalObjects( legendSet );

        for ( MapView mapView : mapViews )
        {
            mapView.setLegendSet( null );
            mappingService.update( mapView );
        }
    }

    @Override
    protected boolean isDeleteOrganisationUnitGroupSet()
    {
        return false; // special implementation below
    }

    public void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        List<MapView> mapViews = mappingService.getMapViewsByOrganisationUnitGroupSet( groupSet );

        for ( MapView mapView : mapViews )
        {
            mapView.setOrganisationUnitGroupSet( null );
            mappingService.updateMapView( mapView );
        }
    }

    private DeletionVeto allowDeleteMapView( MapView mapView )
    {
        return mappingService.countMapViewMaps( mapView ) == 0 ? ACCEPT : veto;
    }
}

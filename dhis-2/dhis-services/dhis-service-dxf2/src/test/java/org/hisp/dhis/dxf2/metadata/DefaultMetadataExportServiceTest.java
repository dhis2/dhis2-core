/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultMetadataExportService}.
 *
 * @author Volker Schmidt
 */
@ExtendWith( MockitoExtension.class )
class DefaultMetadataExportServiceTest
{
    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private DefaultMetadataExportService service;

    @Test
    void getParamsFromMapIncludedSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "jobConfigurations" ) ) )
            .thenReturn( new Schema( JobConfiguration.class, "jobConfiguration", "jobConfigurations" ) );
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "jobConfigurations", Collections.singletonList( "true" ) );
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertTrue( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }

    @Test
    void getParamsFromMapNotIncludedSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "jobConfigurations" ) ) )
            .thenReturn( new Schema( JobConfiguration.class, "jobConfiguration", "jobConfigurations" ) );
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "jobConfigurations", Arrays.asList( "true", "false" ) );
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertFalse( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }

    @Test
    void getParamsFromMapNoSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertFalse( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }

    @Test
    void testGetMetadataWithDependenciesForDashboardWithMapView()
    {
        MapView mapView = new MapView();
        mapView.setName( "mapViewA" );

        org.hisp.dhis.mapping.Map map = new org.hisp.dhis.mapping.Map();
        map.setName( "mapA" );
        map.getMapViews().add( mapView );

        DashboardItem item = new DashboardItem();
        item.setName( "itemA" );
        item.setMap( map );

        Dashboard dashboard = new Dashboard( "dashboardA" );
        dashboard.getItems().add( item );

        SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result = service
            .getMetadataWithDependencies( dashboard );
        // MapView is embedded object, it must not be included at top level
        assertNull( result.get( MapView.class ) );
        assertNotNull( result.get( Dashboard.class ) );
        assertNotNull( result.get( org.hisp.dhis.mapping.Map.class ) );
        Set<IdentifiableObject> setMap = result.get( org.hisp.dhis.mapping.Map.class );
        assertEquals( 1, setMap.size() );
        org.hisp.dhis.mapping.Map mapResult = (org.hisp.dhis.mapping.Map) setMap.iterator().next();
        assertEquals( 1, mapResult.getMapViews().size() );
        assertEquals( mapView.getName(), mapResult.getMapViews().get( 0 ).getName() );
    }
}
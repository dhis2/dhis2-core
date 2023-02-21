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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.time.ZonedDateTime;
import java.util.Date;

import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventStore;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for maps which have not been viewed in at least a year.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/maps_not_used_1year.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityMapsNotUsedOneYearControllerTest extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    @Autowired
    private MappingService mappingService;

    private DataStatisticsEvent dse1;

    private static final String check = "maps_not_viewed_one_year";

    @Test
    void testUnusedVisualizationsExist()
    {

        Date oneYearAgo = Date.from( ZonedDateTime.now().minusYears( 1 ).minusDays( 1 ).toInstant() );

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.MAP_VIEW, oneYearAgo, "TestUser", BASE_UID );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertNamedMetadataObjectExists( "maps", "MapA" );
        assertHasDataIntegrityIssues( "maps", check, 100, BASE_UID, "MapA", null, true );
    }

    @Test
    void testUsedVisualizationsExist()
    {

        long millis = System.currentTimeMillis();
        Date rightNow = new Date( millis );

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.MAP_VIEW, rightNow, "TestUser", BASE_UID );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertHasNoDataIntegrityIssues( "maps", check, true );
    }

    @Test
    void testUnusedVisualizationsRuns()
    {
        assertHasNoDataIntegrityIssues( "maps", check, false );
    }

    @BeforeEach
    void setUp()
    {
        /* Simplified setup from MappingServiceTest */
        MapView mvA = new MapView( "thematic" );
        Map mpA = new Map( "MapA", null, 0d, 0d, 0 );
        mpA.setUid( BASE_UID );
        mpA.getMapViews().add( mvA );
        mappingService.addMap( mpA );

    }
}

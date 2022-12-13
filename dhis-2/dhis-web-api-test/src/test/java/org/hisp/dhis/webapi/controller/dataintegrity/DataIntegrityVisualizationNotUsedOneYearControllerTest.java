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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventStore;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.visualization.VisualizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataIntegrityVisualizationNotUsedOneYearControllerTest extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    @Autowired
    VisualizationService visualizationService;

    private DataStatisticsEvent dse1;

    private Visualization viz;

    private static final String check = "visualizations_notviewed_1y";

    private static final String viz_uid = "YngaQVeOC44";

    @Test
    void testUnusedVisualizationsExist()
    {

        SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
        String dateInString = "2015-10-01";
        Date date = null;
        try
        {
            date = formatter.parse( dateInString );
        }
        catch ( ParseException e )
        {
            throw new RuntimeException( e );
        }

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.VISUALIZATION_VIEW, date, "TestUser", viz.getUid() );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertHasDataIntegrityIssues( "visualizations", check, 100, viz.getUid(), "myviz", null, true );
    }

    @Test
    void testUsedVisualizationsExist()
    {

        Date date = new Date();

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.VISUALIZATION_VIEW, date, "TestUser", viz.getUid() );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertHasNoDataIntegrityIssues( "visualizations", check, true );
    }

    /* Will create a manual test for the positive and negative cases */
    @Test
    void testUnusedVisualizationsRuns()
    {
        assertHasNoDataIntegrityIssues( "visualizations", "visualizations_notviewed_1y", false );
    }

    @BeforeEach
    void setUp()
    {
        viz = new Visualization( "myviz" );
        viz.setUid( viz_uid );
        viz.setType( VisualizationType.SINGLE_VALUE );
        visualizationService.save( viz );
    }
}

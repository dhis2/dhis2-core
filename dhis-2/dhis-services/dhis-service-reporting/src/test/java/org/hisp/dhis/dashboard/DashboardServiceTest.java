package org.hisp.dhis.dashboard;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventchart.EventChartService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class DashboardServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EventChartService eventChartService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private Dashboard dA;
    private Dashboard dB;

    private DashboardItem diA;
    private DashboardItem diB;
    private DashboardItem diC;
    private DashboardItem diD;

    private Visualization visualizationA;
    private Visualization visualizationB;

    private Document docA;

    @Override
    public void setUpTest()
    {
        visualizationA = createVisualization( "A" );
        visualizationB = createVisualization( "B" );

        visualizationService.save( visualizationA );
        visualizationService.save( visualizationB );

        docA = new Document( "A", "url", false, null );
        Document docB = new Document( "B", "url", false, null );
        Document docC = new Document( "C", "url", false, null );
        Document docD = new Document( "D", "url", false, null );

        documentService.saveDocument( docA );
        documentService.saveDocument( docB );
        documentService.saveDocument( docC );
        documentService.saveDocument( docD );

        diA = new DashboardItem();
        diA.setAutoFields();
        diA.setVisualization( visualizationA );

        diB = new DashboardItem();
        diB.setAutoFields();
        diB.setVisualization( visualizationB );

        diC = new DashboardItem();
        diC.setAutoFields();
        diC.getResources().add( docA );
        diC.getResources().add( docB );

        diD = new DashboardItem();
        diD.setAutoFields();
        diD.getResources().add( docC );
        diD.getResources().add( docD );

        dA = new Dashboard( "A" );
        dA.setAutoFields();
        dA.getItems().add( diA );
        dA.getItems().add( diB );
        dA.getItems().add( diC );

        dB = new Dashboard( "B" );
        dB.setAutoFields();
        dB.getItems().add( diD );
    }

    @Test
    public void testAddGet()
    {
        long dAId = dashboardService.saveDashboard( dA );
        long dBId = dashboardService.saveDashboard( dB );

        assertEquals( dA, dashboardService.getDashboard( dAId ) );
        assertEquals( dB, dashboardService.getDashboard( dBId ) );

        assertEquals( 3, dashboardService.getDashboard( dAId ).getItems().size() );
        assertEquals( 1, dashboardService.getDashboard( dBId ).getItems().size() );

        assertEquals( 1, dashboardService.countVisualizationDashboardItems( visualizationA ) );
        assertEquals( 1, dashboardService.countVisualizationDashboardItems( visualizationB ) );
        assertEquals( 1, dashboardService.countDocumentDashboardItems( docA ) );
    }

    @Test
    public void testUpdate()
    {
        long dAId = dashboardService.saveDashboard( dA );

        assertEquals( "A", dashboardService.getDashboard( dAId ).getName() );

        dA.setName( "B" );

        dashboardService.updateDashboard( dA );

        assertEquals( "B", dashboardService.getDashboard( dAId ).getName() );
    }

    @Test
    public void testDelete()
    {
        // ## Ensuring the preparation for deletion
        // When saved
        final long dAId = dashboardService.saveDashboard( dA );
        final long dBId = dashboardService.saveDashboard( dB );

        // Then confirm that they were saved
        assertThatDashboardAndItemsArePersisted( dAId );
        assertThatDashboardAndItemsArePersisted( dBId );

        // ## Testing deletion
        // Given
        final List<DashboardItem> itemsOfDashA = dashboardService.getDashboard( dAId ).getItems();
        final List<DashboardItem> itemsOfDashB = dashboardService.getDashboard( dBId ).getItems();

        // When deleted
        dashboardService.deleteDashboard( dA );
        dashboardService.deleteDashboard( dB );

        // Then confirm that they were deleted
        assertDashboardAndItemsAreDeleted( dAId, itemsOfDashA );
        assertDashboardAndItemsAreDeleted( dBId, itemsOfDashB );
    }

    private void assertThatDashboardAndItemsArePersisted( final long dashboardId )
    {
        final Dashboard dashboard = dashboardService.getDashboard( dashboardId );
        assertNotNull( dashboard );

        final List<DashboardItem> itemsA = dashboard.getItems();

        for ( final DashboardItem dAItem : itemsA )
        {
            assertNotNull( "DashboardItem should exist", dashboardService.getDashboardItem( dAItem.getUid() ) );
        }
    }

    private void assertDashboardAndItemsAreDeleted( final long dashboardId, final List<DashboardItem> dashboardItems )
    {
        assertNull( dashboardService.getDashboard( dashboardId ) );

        // Assert that there are not items related to the given Dashboard
        for ( final DashboardItem item : dashboardItems )
        {
            assertNull( "DashboardItem should not exist", dashboardService.getDashboardItem( item.getUid() ) );
        }
    }

    @Test
    public void testAddItemContent()
    {
        dashboardService.saveDashboard( dA );
        dashboardService.saveDashboard( dB );

        DashboardItem itemA = dashboardService.addItemContent( dA.getUid(), DashboardItemType.VISUALIZATION, visualizationA.getUid() );

        assertNotNull( itemA );
        assertNotNull( itemA.getUid() );
    }

    @Test( expected = DeleteNotAllowedException.class)
    public void testDeleteWithDashboardItem()
    {
        Program prA = createProgram( 'A', null, null );
        objectManager.save( prA );

        EventChart eventChart = new EventChart( "ecA" );
        eventChart.setProgram( prA );
        eventChart.setType( ChartType.COLUMN );

        long idA = eventChartService.saveEventChart( eventChart );

        assertNotNull( eventChartService.getEventChart( idA ) );

        Dashboard dashboard = new Dashboard( "A" );
        dashboard.setAutoFields();

        dashboardService.saveDashboard( dashboard );

        DashboardItem itemA = dashboardService.addItemContent( dashboard.getUid(), DashboardItemType.EVENT_CHART, eventChart.getUid() );

        assertNotNull( itemA );

        eventChartService.deleteEventChart( eventChart );
    }

    @Test
    public void testSearchDashboard()
    {
        dashboardService.saveDashboard( dA );
        dashboardService.saveDashboard( dB );

        DashboardSearchResult result = dashboardService.search( "A" );
        assertEquals(1, result.getVisualizationCount() );
        assertEquals(1, result.getResourceCount() );

        result = dashboardService.search( "B" );
        assertEquals(1, result.getVisualizationCount() );
        assertEquals(1, result.getResourceCount() );

        result = dashboardService.search( "Z" );
        assertEquals(0, result.getVisualizationCount() );
        assertEquals(0, result.getResourceCount() );
    }

    @Test
    public void testSearchDashboardWithMaxCount()
    {
        Program prA = createProgram( 'A', null, null );
        objectManager.save( prA );

        IntStream.range(1, 30).forEach( i -> {
            Visualization visualization = createVisualization( "A" );
            visualization.setName( RandomStringUtils.randomAlphabetic( 5 ) );
            visualizationService.save( visualization );

        });

        IntStream.range(1, 30 ).forEach( i -> eventChartService.saveEventChart( createEventChart( prA ) ) );

        DashboardSearchResult result = dashboardService.search( Sets.newHashSet( DashboardItemType.VISUALIZATION ) );

        assertThat( result.getVisualizationCount(), is( 25 ) );
        assertThat( result.getEventChartCount(), is( 6 ) );

        result = dashboardService.search( Sets.newHashSet( DashboardItemType.VISUALIZATION ), 3, null );

        assertThat( result.getVisualizationCount(), is( 25 ) );
        assertThat( result.getEventChartCount(), is( 3 ) );

        result = dashboardService.search( Sets.newHashSet( DashboardItemType.VISUALIZATION ), 3, 29 );

        assertThat( result.getVisualizationCount(), is( 29 ) );
        assertThat( result.getEventChartCount(), is( 3 ) );

    }

    private EventChart createEventChart( Program program )
    {
        EventChart eventChart = new EventChart( RandomStringUtils.randomAlphabetic( 5 ) );
        eventChart.setProgram( program );
        eventChart.setType( ChartType.COLUMN );
        return eventChart;
    }
}

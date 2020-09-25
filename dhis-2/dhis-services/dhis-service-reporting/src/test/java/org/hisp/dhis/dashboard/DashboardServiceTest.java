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

    private Dashboard dbA;
    private Dashboard dbB;

    private DashboardItem diA;
    private DashboardItem diB;
    private DashboardItem diC;
    private DashboardItem diD;

    private Visualization vzA;
    private Visualization vzB;

    private Document dcA;

    @Override
    public void setUpTest()
    {
        vzA = createVisualization( "A" );
        vzB = createVisualization( "B" );

        visualizationService.save( vzA );
        visualizationService.save( vzB );

        dcA = new Document( "A", "url", false, null );
        Document dcB = new Document( "B", "url", false, null );
        Document dcC = new Document( "C", "url", false, null );
        Document dcD = new Document( "D", "url", false, null );

        documentService.saveDocument( dcA );
        documentService.saveDocument( dcB );
        documentService.saveDocument( dcC );
        documentService.saveDocument( dcD );

        diA = new DashboardItem();
        diA.setAutoFields();
        diA.setVisualization( vzA );

        diB = new DashboardItem();
        diB.setAutoFields();
        diB.setVisualization( vzB );

        diC = new DashboardItem();
        diC.setAutoFields();
        diC.getResources().add( dcA );
        diC.getResources().add( dcB );

        diD = new DashboardItem();
        diD.setAutoFields();
        diD.getResources().add( dcC );
        diD.getResources().add( dcD );

        dbA = new Dashboard( "A" );
        dbA.setAutoFields();
        dbA.getItems().add( diA );
        dbA.getItems().add( diB );
        dbA.getItems().add( diC );

        dbB = new Dashboard( "B" );
        dbB.setAutoFields();
        dbB.getItems().add( diD );
    }

    @Test
    public void testAddGet()
    {
        long dAId = dashboardService.saveDashboard( dbA );
        long dBId = dashboardService.saveDashboard( dbB );

        assertEquals( dbA, dashboardService.getDashboard( dAId ) );
        assertEquals( dbB, dashboardService.getDashboard( dBId ) );

        assertEquals( 3, dashboardService.getDashboard( dAId ).getItems().size() );
        assertEquals( 1, dashboardService.getDashboard( dBId ).getItems().size() );

        assertEquals( 1, dashboardService.countVisualizationDashboardItems( vzA ) );
        assertEquals( 1, dashboardService.countVisualizationDashboardItems( vzB ) );
        assertEquals( 1, dashboardService.countDocumentDashboardItems( dcA ) );
    }

    @Test
    public void testUpdate()
    {
        long dAId = dashboardService.saveDashboard( dbA );

        assertEquals( "A", dashboardService.getDashboard( dAId ).getName() );

        dbA.setName( "B" );

        dashboardService.updateDashboard( dbA );

        assertEquals( "B", dashboardService.getDashboard( dAId ).getName() );
    }

    @Test
    public void testDelete()
    {
        // ## Ensuring the preparation for deletion
        // When saved
        final long dAId = dashboardService.saveDashboard( dbA );
        final long dBId = dashboardService.saveDashboard( dbB );

        // Then confirm that they were saved
        assertThatDashboardAndItemsArePersisted( dAId );
        assertThatDashboardAndItemsArePersisted( dBId );

        // ## Testing deletion
        // Given
        final List<DashboardItem> itemsOfDashA = dashboardService.getDashboard( dAId ).getItems();
        final List<DashboardItem> itemsOfDashB = dashboardService.getDashboard( dBId ).getItems();

        // When deleted
        dashboardService.deleteDashboard( dbA );
        dashboardService.deleteDashboard( dbB );

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
        dashboardService.saveDashboard( dbA );
        dashboardService.saveDashboard( dbB );

        DashboardItem itemA = dashboardService.addItemContent( dbA.getUid(), DashboardItemType.VISUALIZATION, vzA.getUid() );

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
        dashboardService.saveDashboard( dbA );
        dashboardService.saveDashboard( dbB );

        DashboardSearchResult result = dashboardService.search( "A" );
        assertEquals( 1, result.getVisualizationCount() );
        assertEquals( 1, result.getResourceCount() );

        result = dashboardService.search( "B" );
        assertEquals( 1, result.getVisualizationCount() );
        assertEquals( 1, result.getResourceCount() );

        result = dashboardService.search( "Z" );
        assertEquals( 0, result.getVisualizationCount() );
        assertEquals( 0, result.getResourceCount() );
    }

    @Test
    public void testSearchDashboardWithMaxCount()
    {
        Program prA = createProgram( 'A', null, null );
        objectManager.save( prA );

        IntStream.range( 1, 30 ).forEach( i -> {
            Visualization visualization = createVisualization( 'A' );
            visualization.setName( RandomStringUtils.randomAlphabetic( 5 ) );
            visualizationService.save( visualization );

        } );

        IntStream.range( 1, 30 ).forEach( i -> eventChartService.saveEventChart( createEventChart( prA ) ) );

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

    private Visualization createVisualization( String name )
    {
        Visualization visualization = createVisualization( 'X' );
        visualization.setName( name );
        return visualization;
    }

    private EventChart createEventChart( Program program )
    {
        EventChart eventChart = new EventChart( RandomStringUtils.randomAlphabetic( 5 ) );
        eventChart.setProgram( program );
        eventChart.setType( ChartType.COLUMN );
        return eventChart;
    }
}

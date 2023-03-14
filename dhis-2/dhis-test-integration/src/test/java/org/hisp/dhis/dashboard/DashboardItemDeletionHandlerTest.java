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
package org.hisp.dhis.dashboard;

import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventchart.EventChartService;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventreport.EventReportService;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class DashboardItemDeletionHandlerTest extends TransactionalIntegrationTest
{

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private EventVisualizationService eventVisualizationService;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private EventReportService eventReportService;

    @Autowired
    private EventChartService eventChartService;

    @Autowired
    private UserService _userService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DocumentService documentService;

    private Dashboard dashboard;

    private DashboardItem dashboardItem;

    private Program program;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        dashboardItem = new DashboardItem();
        dashboard = new Dashboard( "A" );
        dashboard.getItems().add( dashboardItem );
        program = createProgram( 'A' );
    }

    @Test
    void testDeleteVisualization()
    {
        Visualization visualization = createVisualization( 'A' );
        visualizationService.save( visualization );
        dashboardItem.setVisualization( visualization );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getVisualizationDashboardItems( visualization ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        visualizationService.delete( visualization );
        assertEquals( 0, dashboardService.getVisualizationDashboardItems( visualization ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteEventVisualization()
    {
        programService.addProgram( program );
        EventVisualization eventVisualization = createEventVisualization( 'A', program );
        eventVisualizationService.save( eventVisualization );
        dashboardItem.setEventVisualization( eventVisualization );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getEventVisualizationDashboardItems( eventVisualization ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        eventVisualizationService.delete( eventVisualization );
        assertEquals( 0, dashboardService.getEventVisualizationDashboardItems( eventVisualization ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteEventChart()
    {
        programService.addProgram( program );
        EventChart eventChart = new EventChart( "A" );
        eventChart.setProgram( program );
        eventChart.setType( EventVisualizationType.COLUMN );
        eventChartService.saveEventChart( eventChart );
        dashboardItem.setEventChart( eventChart );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getEventChartDashboardItems( eventChart ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        eventChartService.deleteEventChart( eventChart );
        assertEquals( 0, dashboardService.getEventChartDashboardItems( eventChart ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteMap()
    {
        Map map = new Map();
        map.setName( "A" );
        mappingService.addMap( map );
        dashboardItem.setMap( map );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getMapDashboardItems( map ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        mappingService.deleteMap( map );
        assertEquals( 0, dashboardService.getMapDashboardItems( map ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteEventReport()
    {
        programService.addProgram( program );
        EventReport eventReport = new EventReport( "A" );
        eventReport.setProgram( program );
        eventReport.setType( LINE_LIST );
        eventReportService.saveEventReport( eventReport );
        dashboardItem.setEventReport( eventReport );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getEventReportDashboardItems( eventReport ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        eventReportService.deleteEventReport( eventReport );
        assertEquals( 0, dashboardService.getEventReportDashboardItems( eventReport ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteUser()
    {
        User userA = makeUser( "X" );
        User userB = makeUser( "Y" );
        userService.addUser( userA );
        userService.addUser( userB );
        dashboardItem.getUsers().add( userA );
        // Test removing duplicates
        dashboardItem.getUsers().add( userA );
        dashboardItem.getUsers().add( userB );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getUserDashboardItems( userA ).size() );
        assertEquals( 1, dashboardService.getUserDashboardItems( userB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        userService.deleteUser( userA );
        assertEquals( 0, dashboardService.getUserDashboardItems( userA ).size() );
        assertEquals( 1, dashboardService.getUserDashboardItems( userB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        userService.deleteUser( userB );
        assertEquals( 0, dashboardService.getUserDashboardItems( userA ).size() );
        assertEquals( 0, dashboardService.getUserDashboardItems( userB ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteReport()
    {
        Report reportA = new Report();
        Report reportB = new Report();
        reportA.setName( "A" );
        reportB.setName( "B" );
        reportService.saveReport( reportA );
        reportService.saveReport( reportB );
        dashboardItem.getReports().add( reportA );
        // Test removing duplicates
        dashboardItem.getReports().add( reportA );
        dashboardItem.getReports().add( reportB );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getReportDashboardItems( reportA ).size() );
        assertEquals( 1, dashboardService.getReportDashboardItems( reportB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        reportService.deleteReport( reportA );
        assertEquals( 0, dashboardService.getReportDashboardItems( reportA ).size() );
        assertEquals( 1, dashboardService.getReportDashboardItems( reportB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        reportService.deleteReport( reportB );
        assertEquals( 0, dashboardService.getReportDashboardItems( reportA ).size() );
        assertEquals( 0, dashboardService.getReportDashboardItems( reportB ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }

    @Test
    void testDeleteDocument()
    {
        Document documentA = new Document();
        Document documentB = new Document();
        documentA.setName( "A" );
        documentB.setName( "B" );
        documentA.setUrl( "UrlA.com" );
        documentB.setUrl( "UrlB.com" );
        documentService.saveDocument( documentA );
        documentService.saveDocument( documentB );
        dashboardItem.getResources().add( documentA );
        // Test removing
        dashboardItem.getResources().add( documentA );
        // duplicates
        dashboardItem.getResources().add( documentB );
        dashboardService.saveDashboard( dashboard );
        assertEquals( 1, dashboardService.getDocumentDashboardItems( documentA ).size() );
        assertEquals( 1, dashboardService.getDocumentDashboardItems( documentB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        documentService.deleteDocument( documentA );
        assertEquals( 0, dashboardService.getDocumentDashboardItems( documentA ).size() );
        assertEquals( 1, dashboardService.getDocumentDashboardItems( documentB ).size() );
        assertEquals( 1, dashboard.getItemCount() );
        documentService.deleteDocument( documentB );
        assertEquals( 0, dashboardService.getDocumentDashboardItems( documentA ).size() );
        assertEquals( 0, dashboardService.getDocumentDashboardItems( documentB ).size() );
        assertEquals( 0, dashboard.getItemCount() );
    }
}

package org.hisp.dhis.dashboard;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Set;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface DashboardService
{
    String ID = DashboardService.class.getName();

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    DashboardSearchResult search( String query );

    DashboardSearchResult search( String query, Set<DashboardItemType> maxTypes );

    DashboardItem addItemContent( String dashboardUid, DashboardItemType type, String contentUid );

    void mergeDashboard( Dashboard dashboard );

    void mergeDashboardItem( DashboardItem item );

    int saveDashboard( Dashboard dashboard );

    void updateDashboard( Dashboard dashboard );

    void deleteDashboard( Dashboard dashboard );

    Dashboard getDashboard( int id );

    Dashboard getDashboard( String uid );

    // -------------------------------------------------------------------------
    // DashboardItem
    // -------------------------------------------------------------------------

    void updateDashboardItem( DashboardItem item );
    
    DashboardItem getDashboardItem( String uid );

    Dashboard getDashboardFromDashboardItem( DashboardItem dashboardItem );

    void deleteDashboardItem( DashboardItem item );
    
    int countMapDashboardItems( Map map );

    int countChartDashboardItems( Chart chart );

    int countReportTableDashboardItems( ReportTable reportTable );

    int countReportDashboardItems( Report report );

    int countDocumentDashboardItems( Document document );
    
    int countUserDashboardItems( User user );
}

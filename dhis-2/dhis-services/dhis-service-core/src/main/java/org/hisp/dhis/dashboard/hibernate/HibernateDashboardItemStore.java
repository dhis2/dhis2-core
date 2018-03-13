package org.hisp.dhis.dashboard.hibernate;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemStore;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;

import javax.persistence.Query;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateDashboardItemStore extends HibernateIdentifiableObjectStore<DashboardItem>
    implements DashboardItemStore
{
    @Override
    public int countMapDashboardItems( Map map )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where c.map=:map" );
        query.setParameter( "map", map );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countChartDashboardItems( Chart chart )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where c.chart=:chart" );
        query.setParameter( "chart", chart );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countEventChartDashboardItems( EventChart eventChart )
    {
        Query query = getJpaQuery("select count(distinct c) from DashboardItem c where c.eventChart=:eventChart" );

        query.setParameter( "eventChart", eventChart );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countReportTableDashboardItems( ReportTable reportTable )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where c.reportTable=:reportTable" );
        query.setParameter( "reportTable", reportTable );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countReportDashboardItems( Report report )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where :report in elements(c.reports)" );
        query.setParameter( "report", report );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countDocumentDashboardItems( Document document )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where :document in elements(c.resources)" );
        query.setParameter( "document", document );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public int countUserDashboardItems( User user )
    {
        Query query = getJpaQuery( "select count(distinct c) from DashboardItem c where :user in elements(c.users)" );
        query.setParameter( "user", user );

        return ((Long) query.getSingleResult()).intValue();
    }

    @Override
    public Dashboard getDashboardFromDashboardItem( DashboardItem dashboardItem )
    {
        Query query = getJpaQuery( "from Dashboard d where :item in elements(d.items)" );
        query.setParameter( "item", dashboardItem );

        return (Dashboard) query.getSingleResult();
    }
}

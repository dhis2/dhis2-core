package org.hisp.dhis.pushanalysis;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * @author Stian Sandvold
 */
public class DefaultPushAnalysisService
    implements PushAnalysisService
{

    private ReportTableService reportTableService;

    private OrganisationUnitService organisationUnitService;

    private MessageSender messageSender;

    private ChartService chartService;

    private I18nManager i18nManager;

    public void setReportTableService( ReportTableService reportTableService )
    {
        this.reportTableService = reportTableService;
    }

    public ReportTableService getReportTableService()
    {
        return reportTableService;
    }

    public OrganisationUnitService getOrganisationUnitService()
    {
        return organisationUnitService;
    }

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    public MessageSender getMessageSender()
    {
        return messageSender;
    }

    public void setMessageSender( MessageSender messageSender )
    {
        this.messageSender = messageSender;
    }

    public ChartService getChartService()
    {
        return chartService;
    }

    public void setChartService( ChartService chartService )
    {
        this.chartService = chartService;
    }

    public I18nManager getI18nManager()
    {
        return i18nManager;
    }

    public void setI18nManager( I18nManager i18nManager )
    {
        this.i18nManager = i18nManager;
    }

    private List<DashboardItemType> supportedItemTypes = Lists
        .newArrayList( DashboardItemType.MAP, DashboardItemType.CHART, DashboardItemType.REPORT_TABLE,
            DashboardItemType.EVENT_CHART, DashboardItemType.EVENT_REPORT );

    private GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore;

    public void setPushAnalysisStore( GenericIdentifiableObjectStore<PushAnalysis> pushAnalysisStore )
    {
        this.pushAnalysisStore = pushAnalysisStore;
    }

    @Override
    public PushAnalysis getByUid( String uid )
    {
        return pushAnalysisStore.getByUid( uid );
    }

    @Override
    public boolean stopPushAnalysis( PushAnalysis pushAnalysis )
    {
        return false;
    }

    @Override
    public boolean startPushAnalysis( PushAnalysis pushAnalysis )
    {
        return false;
    }

    @Override
    public void runPushAnalysis( PushAnalysis pushAnalysis )
    {
        Set<User> receivingUsers = new HashSet<>();

        pushAnalysis.getReceivingUserGroups().forEach( userGroup -> receivingUsers.addAll( userGroup.getMembers() ) );

        receivingUsers.forEach( user -> {
            if ( user.getEmail().length() > 0 )
            {
                try
                {
                    messageSender
                        .sendMessage( pushAnalysis.getName(), generatePushAnalysisForUser( user, pushAnalysis ), "",
                            null, Sets.newHashSet( user ), true );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        } );
    }

    @Override
    public String generatePushAnalysisForUser( User user, PushAnalysis pushAnalysis )
        throws Exception
    {
        return generatePushAnalysisHTML( user, pushAnalysis );
    }

    private String generatePushAnalysisHTML( User user, PushAnalysis pushAnalysis )
        throws Exception
    {
        /**
         * Note:
         *
         * Gmail strips head and doctype info, and ignores embedded css
         * Inlining all css, and keeping head in case other clients support it.
         *
         **/
        String result = "" +
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
            "<html lang=\"en-GB\" xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>" +
            pushAnalysis.getName() +
            "</title><meta name=\"viewport\" content=\"width=device-width\" />" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
            "</head><body><table><tr><td></td><td style=\"max-width:600px!important;display:block!important;margin:0 auto!important;clear:both!important\"><div style=\"max-width:600px!important;padding:0!important;margin:0 auto\">" +
            "<div style=\"overflow-x:scroll;width:300px;float:left;margin-bottom:20px;max-width:600px;min-width:300px;width:100%\"><table><tr><td>" +
            "<h1>" + pushAnalysis.getName() + "</h1><p>" + pushAnalysis.getMessage() + "</p>" +
            "</td></tr></table></div>";

        for ( DashboardItem dashboardItem : pushAnalysis.getDashboard().getItems() )
        {
            result += generateHTMLForItem( user, dashboardItem );
        }

        result += "</div></td><td></td></tr></table></body></html>";

        return result;
    }

    private String generateHTMLForItem( User user, DashboardItem item )
        throws Exception
    {

        String res = "";

        // Skip unsupported types (IE: messages, apps, etc)
        if ( supportedItemTypes.contains( item.getType() ) )
        {
            res +=
                "<div style=\"overflow-x:auto;width:300px;float:left;margin-bottom:20px;max-width:600px;min-width:300px;width:100%\"><table><tr><td>" +
                    generateHMTLForItemContent( user, item ) +
                    "</td></tr></table></div>";
        }

        return res;
    }

    // TODO: Get absolute url for images
    private String generateHMTLForItemContent( User user, DashboardItem item )
        throws Exception
    {
        if ( item.getType() == DashboardItemType.MAP )
        {
            return "" +
                "<h3>" + item.getMap().getDisplayName() + "</h3>" +
                "<img src='/api/maps/" + item.getMap().getUid() + "/data.png?width=600' width='100%' alt='" +
                item.getMap().getName() + "' />";
        }
        else if ( item.getType() == DashboardItemType.CHART )
        {
            return "" +
                "<h3>" + item.getChart().getDisplayName() + "</h3>" +
                "<img src='/api/charts/" + item.getChart().getUid() + "/data.png?width=600' width='100%' alt='" +
                item.getChart().getName() + "' />";
        }
        else if ( item.getType() == DashboardItemType.EVENT_CHART )
        {
            chartService.getJFreeChart( item.getEventChart(), null, null, i18nManager.getI18nFormat() );

            return "" +
                "<h3>" + item.getEventChart().getDisplayName() + "</h3>" +
                "<img src='/api/eventCharts/" + item.getEventChart().getUid() +
                "/data.png?width=600' width='100%' alt='" +
                item.getEventChart().getName() + "' />";
        }
        else if ( item.getType() == DashboardItemType.REPORT_TABLE )
        {
            ReportTable reportTable = reportTableService.getReportTable( item.getReportTable().getUid() );

            Date date = new Date();
            String organisationUnitUid = null;
            StringWriter stringWriter = new StringWriter();

            if ( reportTable.hasReportParams() && reportTable.getReportParams().isOrganisationUnitSet() )
            {
                organisationUnitUid = organisationUnitService.getRootOrganisationUnits().iterator().next().getUid();
            }

            GridUtils.toHtmlInlineCss(
                reportTableService
                    .getReportTableGridByUser( item.getReportTable().getUid(), date, organisationUnitUid, user ),
                stringWriter
            );

            // Remove all newlines, R introduced in java 8 and covers all line break characters
            return stringWriter.toString().replaceAll( "\\R", "" );
        }
        else if ( item.getType() == DashboardItemType.EVENT_REPORT )
        {

            // TODO
            return "TODO: EventReports";
        }
        else
        {
            return "";
        }
    }
}
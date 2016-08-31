package org.hisp.dhis.dashboard;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an item in the dashboard. An item can represent an embedded object
 * or represent links to other objects.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dashboardItem", namespace = DxfNamespaces.DXF_2_0 )
public class DashboardItem
    extends BaseIdentifiableObject
{
    public static final int MAX_CONTENT = 8;

    private Chart chart;

    private EventChart eventChart;

    private Map map;

    private ReportTable reportTable;

    private EventReport eventReport;

    @Scanned
    private List<User> users = new ArrayList<>();

    @Scanned
    private List<Report> reports = new ArrayList<>();

    @Scanned
    private List<Document> resources = new ArrayList<>();

    private Boolean messages;

    private DashboardItemShape shape;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DashboardItem()
    {
        setAutoFields();
    }

    public DashboardItem( String uid )
    {
        this.uid = uid;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DashboardItemType getType()
    {
        if ( chart != null )
        {
            return DashboardItemType.CHART;
        }
        else if ( eventChart != null )
        {
            return DashboardItemType.EVENT_CHART;
        }
        else if ( map != null )
        {
            return DashboardItemType.MAP;
        }
        else if ( reportTable != null )
        {
            return DashboardItemType.REPORT_TABLE;
        }
        else if ( eventReport != null )
        {
            return DashboardItemType.EVENT_REPORT;
        }
        else if ( !users.isEmpty() )
        {
            return DashboardItemType.USERS;
        }
        else if ( !reports.isEmpty() )
        {
            return DashboardItemType.REPORTS;
        }
        else if ( !resources.isEmpty() )
        {
            return DashboardItemType.RESOURCES;
        }
        else if ( messages != null )
        {
            return DashboardItemType.MESSAGES;
        }

        return null;
    }

    /**
     * Returns the actual item object if this dashboard item represents an
     * embedded item and not links to items.
     */
    public IdentifiableObject getEmbeddedItem()
    {
        if ( chart != null )
        {
            return chart;
        }
        else if ( eventChart != null )
        {
            return eventChart;
        }
        else if ( map != null )
        {
            return map;
        }
        else if ( reportTable != null )
        {
            return reportTable;
        }
        else if ( eventReport != null )
        {
            return eventReport;
        }

        return null;
    }

    /**
     * Returns a list of the actual item objects if this dashboard item
     * represents a list of objects and not an embedded item.
     */
    public List<? extends IdentifiableObject> getLinkItems()
    {
        if ( !users.isEmpty() )
        {
            return users;
        }
        else if ( !reports.isEmpty() )
        {
            return reports;
        }
        else if ( !resources.isEmpty() )
        {
            return resources;
        }

        return null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getContentCount()
    {
        int count = 0;
        count += chart != null ? 1 : 0;
        count += eventChart != null ? 1 : 0;
        count += map != null ? 1 : 0;
        count += reportTable != null ? 1 : 0;
        count += eventReport != null ? 1 : 0;
        count += users.size();
        count += reports.size();
        count += resources.size();
        count += messages != null ? 1 : 0;
        return count;
    }

    /**
     * Removes the content with the given uid. Returns true if a content with
     * the given uid existed and was removed.
     *
     * @param uid the identifier of the content.
     * @return true if a content was removed.
     */
    public boolean removeItemContent( String uid )
    {
        if ( !users.isEmpty() )
        {
            return removeContent( uid, users );
        }
        else if ( !reports.isEmpty() )
        {
            return removeContent( uid, reports );
        }
        else
        {
            return removeContent( uid, resources );
        }
    }

    private boolean removeContent( String uid, List<? extends IdentifiableObject> content )
    {
        Iterator<? extends IdentifiableObject> iterator = content.iterator();

        while ( iterator.hasNext() )
        {
            if ( uid.equals( iterator.next().getUid() ) )
            {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Chart getChart()
    {
        return chart;
    }

    public void setChart( Chart chart )
    {
        this.chart = chart;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventChart getEventChart()
    {
        return eventChart;
    }

    public void setEventChart( EventChart eventChart )
    {
        this.eventChart = eventChart;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Map getMap()
    {
        return map;
    }

    public void setMap( Map map )
    {
        this.map = map;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ReportTable getReportTable()
    {
        return reportTable;
    }

    public void setReportTable( ReportTable reportTable )
    {
        this.reportTable = reportTable;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventReport getEventReport()
    {
        return eventReport;
    }

    public void setEventReport( EventReport eventReport )
    {
        this.eventReport = eventReport;
    }

    @JsonProperty( "users" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "users", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "user", namespace = DxfNamespaces.DXF_2_0 )
    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers( List<User> users )
    {
        this.users = users;
    }

    @JsonProperty( "reports" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "reports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "report", namespace = DxfNamespaces.DXF_2_0 )
    public List<Report> getReports()
    {
        return reports;
    }

    public void setReports( List<Report> reports )
    {
        this.reports = reports;
    }

    @JsonProperty( "resources" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "resources", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "resource", namespace = DxfNamespaces.DXF_2_0 )
    public List<Document> getResources()
    {
        return resources;
    }

    public void setResources( List<Document> resources )
    {
        this.resources = resources;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getMessages()
    {
        return messages;
    }

    public void setMessages( Boolean messages )
    {
        this.messages = messages;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DashboardItemShape getShape()
    {
        return shape;
    }

    public void setShape( DashboardItemShape shape )
    {
        this.shape = shape;
    }

    // -------------------------------------------------------------------------
    // Merge with
    // -------------------------------------------------------------------------

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            DashboardItem dashboardItem = (DashboardItem) other;

            if ( strategy.isReplace() )
            {
                chart = dashboardItem.getChart();
                map = dashboardItem.getMap();
                reportTable = dashboardItem.getReportTable();
                users = dashboardItem.getUsers();
                reports = dashboardItem.getReports();
                resources = dashboardItem.getResources();
                messages = dashboardItem.getMessages();
                shape = dashboardItem.getShape();
            }
            else if ( strategy.isMerge() )
            {
                chart = dashboardItem.getChart() == null ? chart : dashboardItem.getChart();
                map = dashboardItem.getMap() == null ? map : dashboardItem.getMap();
                reportTable = dashboardItem.getReportTable() == null ? reportTable : dashboardItem.getReportTable();
                users = dashboardItem.getUsers() == null ? users : dashboardItem.getUsers();
                reports = dashboardItem.getReports() == null ? reports : dashboardItem.getReports();
                resources = dashboardItem.getResources() == null ? resources : dashboardItem.getResources();
                messages = dashboardItem.getMessages() == null ? messages : dashboardItem.getMessages();
                shape = dashboardItem.getShape() == null ? shape : dashboardItem.getShape();
            }
        }
    }
}

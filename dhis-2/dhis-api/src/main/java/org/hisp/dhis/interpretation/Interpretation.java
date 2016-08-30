package org.hisp.dhis.interpretation;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.analytics.AnalyticsFavoriteType;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "interpretation", namespace = DxfNamespaces.DXF_2_0 )
public class Interpretation
    extends BaseIdentifiableObject
{
    private ReportTable reportTable;

    private Chart chart;

    private Map map;

    private EventReport eventReport;
    
    private EventChart eventChart;
    
    private DataSet dataSet;

    private Period period; // Applicable to report table and data set report

    private OrganisationUnit organisationUnit; // Applicable to chart, report table and data set report

    private String text;

    private List<InterpretationComment> comments = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Interpretation()
    {
    }

    public Interpretation( Chart chart, OrganisationUnit organisationUnit, String text )
    {
        this.chart = chart;
        this.organisationUnit = organisationUnit;
        this.text = text;
    }

    public Interpretation( Map map, String text )
    {
        this.map = map;
        this.text = text;
    }

    public Interpretation( ReportTable reportTable, Period period, OrganisationUnit organisationUnit, String text )
    {
        this.reportTable = reportTable;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.text = text;
    }
    
    public Interpretation( EventReport eventReport, String text )
    {
        this.eventReport = eventReport;
        this.text = text;
    }

    public Interpretation( EventChart eventChart, String text )
    {
        this.eventChart = eventChart;
        this.text = text;
    }
    
    public Interpretation( DataSet dataSet, Period period, OrganisationUnit organisationUnit, String text )
    {
        this.dataSet = dataSet;
        this.period = period;
        this.organisationUnit = organisationUnit;
        this.text = text;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Overriding getUser in order to expose user in web api. Sharing is not enabled
     * for interpretations but "user" is used for representing the creator. Must
     * be removed when sharing is enabled for this class.
     */
    @Override
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getUser()
    {
        return user;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AnalyticsFavoriteType getType()
    {
        if ( reportTable != null )
        {
            return AnalyticsFavoriteType.REPORT_TABLE;
        }
        else if ( chart != null )
        {
            return AnalyticsFavoriteType.CHART;
        }
        else if ( map != null )
        {
            return AnalyticsFavoriteType.MAP;
        }
        else if ( eventReport != null )
        {
            return AnalyticsFavoriteType.EVENT_REPORT;
        }
        else if ( eventChart != null )
        {
            return AnalyticsFavoriteType.EVENT_CHART;
        }
        else if ( dataSet != null )
        {
            return AnalyticsFavoriteType.DATASET_REPORT;
        }

        return null;
    }

    public IdentifiableObject getObject()
    {
        if ( reportTable != null )
        {
            return reportTable;
        }
        else if ( chart != null )
        {
            return chart;
        }
        else if ( map != null )
        {
            return map;
        }
        else if ( eventReport != null )
        {
            return eventReport;
        }
        else if ( eventChart != null )
        {
            return eventChart;
        }
        else if ( dataSet != null )
        {
            return dataSet;
        }

        return null;
    }

    public void addComment( InterpretationComment comment )
    {
        this.comments.add( comment );
    }

    public boolean isChartInterpretation()
    {
        return chart != null;
    }

    public boolean isMapInterpretation()
    {
        return map != null;
    }

    public boolean isReportTableInterpretation()
    {
        return reportTable != null;
    }
    
    public boolean isEventReportInterpretation()
    {
        return eventReport != null;
    }
    
    public boolean isEventChartInterpretation()
    {
        return eventChart != null;
    }

    public boolean isDataSetReportInterpretation()
    {
        return dataSet != null;
    }

    public PeriodType getPeriodType()
    {
        return period != null ? period.getPeriodType() : null;
    }

    public void updateSharing()
    {
        setPublicAccess( AccessStringHelper.newInstance().enable( AccessStringHelper.Permission.READ ).build() );
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return uid;
    }

    @JsonProperty
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

    @JsonProperty
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( OrganisationUnit organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "comments", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "comment", namespace = DxfNamespaces.DXF_2_0 )
    public List<InterpretationComment> getComments()
    {
        return comments;
    }

    public void setComments( List<InterpretationComment> comments )
    {
        this.comments = comments;
    }
}

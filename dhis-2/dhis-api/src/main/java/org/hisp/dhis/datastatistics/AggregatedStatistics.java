package org.hisp.dhis.datastatistics;

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

/**
 * Aggregated DataStatistics object
 * 
 * @author Julie Hill Roa
 * @author Yrjan Fraschetti
 */
public class AggregatedStatistics
{
    private Integer year;
    private Integer month;
    private Integer week;
    private Integer day;
    private Integer mapViews;
    private Integer chartViews;
    private Integer pivotTableViews;
    private Integer eventReportViews;
    private Integer eventChartViews;
    private Integer dashboardViews;
    private Integer dataSetReportViews;
    private Integer totalViews;
    private Integer averageViews;
    private Integer averageMapViews;
    private Integer averageChartViews;
    private Integer averagePivotTableViews;
    private Integer averageEventReportViews;
    private Integer averageEventChartViews;
    private Integer averageDashboardViews;
    private Integer savedMaps;
    private Integer savedCharts;
    private Integer savedPivotTables;
    private Integer savedEventReports;
    private Integer savedEventCharts;
    private Integer savedDashboards;
    private Integer savedIndicators;
    private Integer savedDataValues;
    private Integer activeUsers;
    private Integer users;

    public AggregatedStatistics()
    {
    }

    @JsonProperty
    public Integer getYear()
    {
        return year;
    }

    public void setYear( Integer year )
    {
        this.year = year;
    }

    @JsonProperty
    public Integer getMonth()
    {
        return month;
    }

    public void setMonth( Integer month )
    {
        this.month = month;
    }

    @JsonProperty
    public Integer getWeek()
    {
        return week;
    }

    public void setWeek( Integer week )
    {
        this.week = week;
    }

    @JsonProperty
    public Integer getDay()
    {
        return day;
    }

    public void setDay( Integer day )
    {
        this.day = day;
    }

    @JsonProperty
    public Integer getActiveUsers()
    {
        return activeUsers;
    }

    public void setActiveUsers( Integer activeUsers )
    {
        this.activeUsers = activeUsers;
    }

    @JsonProperty
    public Integer getMapViews()
    {
        return mapViews;
    }

    public void setMapViews( Integer mapViews )
    {
        this.mapViews = mapViews;
    }

    @JsonProperty
    public Integer getChartViews()
    {
        return chartViews;
    }

    public void setChartViews( Integer chartViews )
    {
        this.chartViews = chartViews;
    }

    @JsonProperty
    public Integer getPivotTableViews()
    {
        return pivotTableViews;
    }

    public void setPivotTableViews( Integer pivotTableViews )
    {
        this.pivotTableViews = pivotTableViews;
    }

    @JsonProperty
    public Integer getEventReportViews()
    {
        return eventReportViews;
    }

    public void setEventReportViews( Integer eventReportViews )
    {
        this.eventReportViews = eventReportViews;
    }

    @JsonProperty
    public Integer getEventChartViews()
    {
        return eventChartViews;
    }

    public void setEventChartViews( Integer eventChartViews )
    {
        this.eventChartViews = eventChartViews;
    }

    @JsonProperty
    public Integer getDashboardViews()
    {
        return dashboardViews;
    }

    public void setDashboardViews( Integer dashboardViews )
    {
        this.dashboardViews = dashboardViews;
    }

    @JsonProperty
    public Integer getDataSetReportViews()
    {
        return dataSetReportViews;
    }

    public void setDataSetReportViews( Integer dataSetReportViews )
    {
        this.dataSetReportViews = dataSetReportViews;
    }

    @JsonProperty
    public Integer getTotalViews()
    {
        return totalViews;
    }

    public void setTotalViews( Integer totalViews )
    {
        this.totalViews = totalViews;
    }

    @JsonProperty
    public Integer getAverageViews()
    {
        return averageViews;
    }

    public void setAverageViews( Integer averageViews )
    {
        this.averageViews = averageViews;
    }

    @JsonProperty
    public Integer getAverageDashboardViews()
    {
        return averageDashboardViews;
    }

    public void setAverageDashboardViews( Integer averageDashboardViews )
    {
        this.averageDashboardViews = averageDashboardViews;
    }

    @JsonProperty
    public Integer getAverageMapViews()
    {
        return averageMapViews;
    }

    public void setAverageMapViews( Integer averageMapViews )
    {
        this.averageMapViews = averageMapViews;
    }

    @JsonProperty
    public Integer getAverageChartViews()
    {
        return averageChartViews;
    }

    public void setAverageChartViews( Integer averageChartViews )
    {
        this.averageChartViews = averageChartViews;
    }

    @JsonProperty
    public Integer getAveragePivotTableViews()
    {
        return averagePivotTableViews;
    }

    public void setAveragePivotTableViews( Integer averagePivotTableViews )
    {
        this.averagePivotTableViews = averagePivotTableViews;
    }

    @JsonProperty
    public Integer getAverageEventReportViews()
    {
        return averageEventReportViews;
    }

    public void setAverageEventReportViews( Integer averageEventReportViews )
    {
        this.averageEventReportViews = averageEventReportViews;
    }

    @JsonProperty
    public Integer getAverageEventChartViews()
    {
        return averageEventChartViews;
    }

    public void setAverageEventChartViews( Integer averageEventChartViews )
    {
        this.averageEventChartViews = averageEventChartViews;
    }

    @JsonProperty
    public Integer getSavedMaps()
    {
        return savedMaps;
    }

    public void setSavedMaps( Integer savedMaps )
    {
        this.savedMaps = savedMaps;
    }

    @JsonProperty
    public Integer getSavedCharts()
    {
        return savedCharts;
    }

    public void setSavedCharts( Integer savedCharts )
    {
        this.savedCharts = savedCharts;
    }

    @JsonProperty
    public Integer getSavedPivotTables()
    {
        return savedPivotTables;
    }

    public void setSavedPivotTables( Integer savedPivotTables )
    {
        this.savedPivotTables = savedPivotTables;
    }

    @JsonProperty
    public Integer getSavedEventReports()
    {
        return savedEventReports;
    }

    public void setSavedEventReports( Integer savedEventReports )
    {
        this.savedEventReports = savedEventReports;
    }

    @JsonProperty
    public Integer getSavedEventCharts()
    {
        return savedEventCharts;
    }

    public void setSavedEventCharts( Integer savedEventCharts )
    {
        this.savedEventCharts = savedEventCharts;
    }

    @JsonProperty
    public Integer getSavedDashboards()
    {
        return savedDashboards;
    }

    public void setSavedDashboards( Integer savedDashboards )
    {
        this.savedDashboards = savedDashboards;
    }

    @JsonProperty
    public Integer getSavedIndicators()
    {
        return savedIndicators;
    }

    public void setSavedIndicators( Integer savedIndicators )
    {
        this.savedIndicators = savedIndicators;
    }

    @JsonProperty
    public Integer getSavedDataValues()
    {
        return savedDataValues;
    }

    public void setSavedDataValues( Integer savedDataValues )
    {
        this.savedDataValues = savedDataValues;
    }

    @JsonProperty
    public Integer getUsers()
    {
        return users;
    }

    public void setUsers( Integer users )
    {
        this.users = users;
    }

    @Override public String toString()
    {
        return "AggregatedStatistics{" +
            "year=" + year +
            ", month=" + month +
            ", week=" + week +
            ", day=" + day +
            ", mapViews=" + mapViews +
            ", chartViews=" + chartViews +
            ", pivotTableViews=" + pivotTableViews +
            ", eventReportViews=" + eventReportViews +
            ", eventChartViews=" + eventChartViews +
            ", dashboardViews=" + dashboardViews +
            ", dataSetReportViews=" + dataSetReportViews +
            ", totalViews=" + totalViews +
            ", averageViews=" + averageViews +
            ", averageMapViews=" + averageMapViews +
            ", averageChartViews=" + averageChartViews +
            ", averagePivotTableViews=" + averagePivotTableViews +
            ", averageEventReportViews=" + averageEventReportViews +
            ", averageEventChartViews=" + averageEventChartViews +
            ", averageDashboardViews=" + averageDashboardViews +
            ", savedMaps=" + savedMaps +
            ", savedCharts=" + savedCharts +
            ", savedPivotTables=" + savedPivotTables +
            ", savedEventReports=" + savedEventReports +
            ", savedEventCharts=" + savedEventCharts +
            ", savedDashboards=" + savedDashboards +
            ", savedIndicators=" + savedIndicators +
            ", savedDataValues=" + savedDataValues +
            ", activeUsers=" + activeUsers +
            ", users=" + users +
            '}';
    }
}
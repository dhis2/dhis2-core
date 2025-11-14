/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.datastatistics;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Julie Hill Roa
 * @author Yrjan Fraschetti
 * @author Jason P. Pickering A record representing aggregated statistics for data usage and
 *     activity.
 * @param year The year of the statistics.
 * @param month The month of the statistics.
 * @param week The week of the statistics.
 * @param day The day of the statistics.
 * @param mapViews The total number of map views.
 * @param visualizationViews The total number of visualization views.
 * @param eventReportViews The total number of event report views.
 * @param eventChartViews The total number of event chart views.
 * @param eventVisualizationViews The total number of event visualization views.
 * @param dashboardViews The total number of dashboard views.
 * @param passiveDashboardViews The total number of passive dashboard views.
 * @param dataSetReportViews The total number of dataset report views.
 * @param totalViews The total number of all views combined.
 * @param averageViews The average number of views per user.
 * @param averageMapViews The average number of map views per user.
 * @param averageVisualizationViews The average number of visualization views per user.
 * @param averageEventReportViews The average number of event report views per user.
 * @param averageEventChartViews The average number of event chart views per user.
 * @param averageEventVisualizationViews The average number of event visualization views per user.
 * @param averageDashboardViews The average number of dashboard views per user.
 * @param averagePassiveDashboardViews The average number of passive dashboard views per user.
 * @param savedMaps The total number of saved maps.
 * @param savedVisualizations The total number of saved visualizations.
 * @param savedEventReports The total number of saved event reports.
 * @param savedEventCharts The total number of saved event charts.
 * @param savedEventVisualizations The total number of saved event visualizations.
 * @param savedDashboards The total number of saved dashboards.
 * @param savedIndicators The total number of saved indicators.
 * @param savedDataValues The total number of saved data values.
 * @param activeUsers The total number of active users.
 * @param users The total number of users.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AggregatedStatistics(
    Integer year,
    Integer month,
    Integer week,
    Integer day,
    long mapViews,
    long visualizationViews,
    long eventReportViews,
    long eventChartViews,
    long eventVisualizationViews,
    long dashboardViews,
    long passiveDashboardViews,
    long dataSetReportViews,
    long totalViews,
    double averageViews,
    double averageMapViews,
    double averageVisualizationViews,
    double averageEventReportViews,
    double averageEventChartViews,
    double averageEventVisualizationViews,
    double averageDashboardViews,
    double averagePassiveDashboardViews,
    long savedMaps,
    long savedVisualizations,
    long savedEventReports,
    long savedEventCharts,
    long savedEventVisualizations,
    long savedDashboards,
    long savedIndicators,
    long savedDataValues,
    long activeUsers,
    long users) {}

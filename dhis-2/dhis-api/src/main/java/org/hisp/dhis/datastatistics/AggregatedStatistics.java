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
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AggregatedStatistics(
    @JsonProperty Integer year,
    @JsonProperty Integer month,
    @JsonProperty Integer week,
    @JsonProperty Integer day,
    @JsonProperty Long mapViews,
    @JsonProperty Long visualizationViews,
    @JsonProperty Long eventReportViews,
    @JsonProperty Long eventChartViews,
    @JsonProperty Long eventVisualizationViews,
    @JsonProperty Long dashboardViews,
    @JsonProperty Long passiveDashboardViews,
    @JsonProperty Long dataSetReportViews,
    @JsonProperty Long totalViews,
    @JsonProperty Double averageViews,
    @JsonProperty Double averageMapViews,
    @JsonProperty Double averageVisualizationViews,
    @JsonProperty Double averageEventReportViews,
    @JsonProperty Double averageEventChartViews,
    @JsonProperty Double averageEventVisualizationViews,
    @JsonProperty Double averageDashboardViews,
    @JsonProperty Double averagePassiveDashboardViews,
    @JsonProperty Long savedMaps,
    @JsonProperty Long savedVisualizations,
    @JsonProperty Long savedEventReports,
    @JsonProperty Long savedEventCharts,
    @JsonProperty Long savedEventVisualizations,
    @JsonProperty Long savedDashboards,
    @JsonProperty Long savedIndicators,
    @JsonProperty Long savedDataValues,
    @JsonProperty Long activeUsers,
    @JsonProperty Long users) {}

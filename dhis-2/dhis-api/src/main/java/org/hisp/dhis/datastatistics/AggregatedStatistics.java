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

/**
 * Aggregated DataStatistics object
 *
 * @author Julie Hill Roa
 * @author Yrjan Fraschetti
 */

/** Aggregated DataStatistics snapshot over a period (year/month/week/day). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AggregatedStatistics {

  // period parts
  @JsonProperty private Integer year;
  @JsonProperty private Integer month;
  @JsonProperty private Integer week;
  @JsonProperty private Integer day;

  // counts (BIGINT -> long)
  @JsonProperty private long mapViews;
  @JsonProperty private long visualizationViews;
  @JsonProperty private long eventReportViews;
  @JsonProperty private long eventChartViews;
  @JsonProperty private long eventVisualizationViews;
  @JsonProperty private long dashboardViews;
  @JsonProperty private long passiveDashboardViews;
  @JsonProperty private long dataSetReportViews;
  @JsonProperty private long totalViews;

  @JsonProperty private long savedMaps;
  @JsonProperty private long savedVisualizations;
  @JsonProperty private long savedEventReports;
  @JsonProperty private long savedEventCharts;
  @JsonProperty private long savedEventVisualizations;
  @JsonProperty private long savedDashboards;
  @JsonProperty private long savedIndicators;
  @JsonProperty private long savedDataValues;

  // averages (double precision)
  @JsonProperty private double averageViews;
  @JsonProperty private double averageMapViews;
  @JsonProperty private double averageVisualizationViews;
  @JsonProperty private double averageEventReportViews;
  @JsonProperty private double averageEventChartViews;
  @JsonProperty private double averageEventVisualizationViews;
  @JsonProperty private double averageDashboardViews;
  @JsonProperty private double averagePassiveDashboardViews;

  // users
  @JsonProperty private Long activeUsers;
  @JsonProperty private Long users;

  public AggregatedStatistics() {}

  // getters/setters (only showing a few; generate the rest with your IDE)

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
  }

  public Integer getWeek() {
    return week;
  }

  public void setWeek(Integer week) {
    this.week = week;
  }

  public Integer getDay() {
    return day;
  }

  public void setDay(Integer day) {
    this.day = day;
  }

  public long getMapViews() {
    return mapViews;
  }

  public void setMapViews(long mapViews) {
    this.mapViews = mapViews;
  }

  public long getVisualizationViews() {
    return visualizationViews;
  }

  public void setVisualizationViews(long visualizationViews) {
    this.visualizationViews = visualizationViews;
  }

  public long getEventReportViews() {
    return eventReportViews;
  }

  public void setEventReportViews(long eventReportViews) {
    this.eventReportViews = eventReportViews;
  }

  public long getEventChartViews() {
    return eventChartViews;
  }

  public void setEventChartViews(long eventChartViews) {
    this.eventChartViews = eventChartViews;
  }

  public long getEventVisualizationViews() {
    return eventVisualizationViews;
  }

  public void setEventVisualizationViews(long eventVisualizationViews) {
    this.eventVisualizationViews = eventVisualizationViews;
  }

  public long getDashboardViews() {
    return dashboardViews;
  }

  public void setDashboardViews(long dashboardViews) {
    this.dashboardViews = dashboardViews;
  }

  public long getPassiveDashboardViews() {
    return passiveDashboardViews;
  }

  public void setPassiveDashboardViews(long passiveDashboardViews) {
    this.passiveDashboardViews = passiveDashboardViews;
  }

  public long getDataSetReportViews() {
    return dataSetReportViews;
  }

  public void setDataSetReportViews(long dataSetReportViews) {
    this.dataSetReportViews = dataSetReportViews;
  }

  public long getTotalViews() {
    return totalViews;
  }

  public void setTotalViews(long totalViews) {
    this.totalViews = totalViews;
  }

  public double getAverageViews() {
    return averageViews;
  }

  public void setAverageViews(double averageViews) {
    this.averageViews = averageViews;
  }

  public double getAverageMapViews() {
    return averageMapViews;
  }

  public void setAverageMapViews(double averageMapViews) {
    this.averageMapViews = averageMapViews;
  }

  public double getAverageVisualizationViews() {
    return averageVisualizationViews;
  }

  public void setAverageVisualizationViews(double averageVisualizationViews) {
    this.averageVisualizationViews = averageVisualizationViews;
  }

  public double getAverageEventReportViews() {
    return averageEventReportViews;
  }

  public void setAverageEventReportViews(double averageEventReportViews) {
    this.averageEventReportViews = averageEventReportViews;
  }

  public double getAverageEventChartViews() {
    return averageEventChartViews;
  }

  public void setAverageEventChartViews(double averageEventChartViews) {
    this.averageEventChartViews = averageEventChartViews;
  }

  public double getAverageEventVisualizationViews() {
    return averageEventVisualizationViews;
  }

  public void setAverageEventVisualizationViews(double averageEventVisualizationViews) {
    this.averageEventVisualizationViews = averageEventVisualizationViews;
  }

  public double getAverageDashboardViews() {
    return averageDashboardViews;
  }

  public void setAverageDashboardViews(double averageDashboardViews) {
    this.averageDashboardViews = averageDashboardViews;
  }

  public double getAveragePassiveDashboardViews() {
    return averagePassiveDashboardViews;
  }

  public void setAveragePassiveDashboardViews(double averagePassiveDashboardViews) {
    this.averagePassiveDashboardViews = averagePassiveDashboardViews;
  }

  public long getSavedMaps() {
    return savedMaps;
  }

  public void setSavedMaps(long savedMaps) {
    this.savedMaps = savedMaps;
  }

  public long getSavedVisualizations() {
    return savedVisualizations;
  }

  public void setSavedVisualizations(long savedVisualizations) {
    this.savedVisualizations = savedVisualizations;
  }

  public long getSavedEventReports() {
    return savedEventReports;
  }

  public void setSavedEventReports(long savedEventReports) {
    this.savedEventReports = savedEventReports;
  }

  public long getSavedEventCharts() {
    return savedEventCharts;
  }

  public void setSavedEventCharts(long savedEventCharts) {
    this.savedEventCharts = savedEventCharts;
  }

  public long getSavedEventVisualizations() {
    return savedEventVisualizations;
  }

  public void setSavedEventVisualizations(long savedEventVisualizations) {
    this.savedEventVisualizations = savedEventVisualizations;
  }

  public long getSavedDashboards() {
    return savedDashboards;
  }

  public void setSavedDashboards(long savedDashboards) {
    this.savedDashboards = savedDashboards;
  }

  public long getSavedIndicators() {
    return savedIndicators;
  }

  public void setSavedIndicators(long savedIndicators) {
    this.savedIndicators = savedIndicators;
  }

  public long getSavedDataValues() {
    return savedDataValues;
  }

  public void setSavedDataValues(long savedDataValues) {
    this.savedDataValues = savedDataValues;
  }

  public Long getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(Long activeUsers) {
    this.activeUsers = activeUsers;
  }

  public Long getUsers() {
    return users;
  }

  public void setUsers(Long users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return "AggregatedStatistics{"
        + "year="
        + year
        + ", month="
        + month
        + ", week="
        + week
        + ", day="
        + day
        + ", mapViews="
        + mapViews
        + ", visualizationViews="
        + visualizationViews
        + ", eventReportViews="
        + eventReportViews
        + ", eventChartViews="
        + eventChartViews
        + ", eventVisualizationViews="
        + eventVisualizationViews
        + ", dashboardViews="
        + dashboardViews
        + ", passiveDashboardViews="
        + passiveDashboardViews
        + ", dataSetReportViews="
        + dataSetReportViews
        + ", totalViews="
        + totalViews
        + ", averageViews="
        + averageViews
        + ", averageMapViews="
        + averageMapViews
        + ", averageVisualizationViews="
        + averageVisualizationViews
        + ", averageEventReportViews="
        + averageEventReportViews
        + ", averageEventChartViews="
        + averageEventChartViews
        + ", averageEventVisualizationViews="
        + averageEventVisualizationViews
        + ", averageDashboardViews="
        + averageDashboardViews
        + ", averagePassiveDashboardViews="
        + averagePassiveDashboardViews
        + ", savedMaps="
        + savedMaps
        + ", savedVisualizations="
        + savedVisualizations
        + ", savedEventReports="
        + savedEventReports
        + ", savedEventCharts="
        + savedEventCharts
        + ", savedEventVisualizations="
        + savedEventVisualizations
        + ", savedDashboards="
        + savedDashboards
        + ", savedIndicators="
        + savedIndicators
        + ", savedDataValues="
        + savedDataValues
        + ", activeUsers="
        + activeUsers
        + ", users="
        + users
        + '}';
  }
}

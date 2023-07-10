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
package org.hisp.dhis.datastatistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.BaseIdentifiableObject;

/**
 * DataStatistics object to be saved as snapshot.
 *
 * @author Julie Hill Roa
 * @author Yrjan A. F. Fraschetti
 */
public class DataStatistics extends BaseIdentifiableObject {
  private Double mapViews;

  private Double visualizationViews;

  private Double eventReportViews;

  private Double eventChartViews;

  private Double eventVisualizationViews;

  private Double dashboardViews;

  private Double passiveDashboardViews;

  private Double dataSetReportViews;

  private Double totalViews;

  private Double savedMaps;

  private Double savedVisualizations;

  private Double savedEventReports;

  private Double savedEventCharts;

  private Double savedEventVisualizations;

  private Double savedDashboards;

  private Double savedIndicators;

  private Double savedDataValues;

  private Integer activeUsers;

  private Integer users;

  public DataStatistics() {}

  public DataStatistics(
      Double mapViews,
      Double visualizationViews,
      Double eventReportViews,
      Double eventChartViews,
      Double eventVisualizationViews,
      Double dashboardViews,
      Double passiveDashboardViews,
      Double dataSetReportViews,
      Double totalViews,
      Double savedMaps,
      Double savedVisualizations,
      Double savedEventReports,
      Double savedEventCharts,
      Double savedEventVisualizations,
      Double savedDashboards,
      Double savedIndicators,
      Double savedDataValues,
      Integer activeUsers,
      Integer users) {
    this.mapViews = mapViews;
    this.visualizationViews = visualizationViews;
    this.eventReportViews = eventReportViews;
    this.eventChartViews = eventChartViews;
    this.eventVisualizationViews = eventVisualizationViews;
    this.dashboardViews = dashboardViews;
    this.passiveDashboardViews = passiveDashboardViews;
    this.dataSetReportViews = dataSetReportViews;
    this.totalViews = totalViews;
    this.savedMaps = savedMaps;
    this.savedVisualizations = savedVisualizations;
    this.savedEventReports = savedEventReports;
    this.savedEventCharts = savedEventCharts;
    this.savedEventVisualizations = savedEventVisualizations;
    this.savedDashboards = savedDashboards;
    this.savedIndicators = savedIndicators;
    this.savedDataValues = savedDataValues;
    this.activeUsers = activeUsers;
    this.users = users;
  }

  @JsonProperty
  public Integer getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(Integer activeUsers) {
    this.activeUsers = activeUsers;
  }

  @JsonProperty
  public Double getMapViews() {
    return mapViews;
  }

  public void setMapViews(Double mapViews) {
    this.mapViews = mapViews;
  }

  @JsonProperty
  public Double getVisualizationViews() {
    return visualizationViews;
  }

  public void setVisualizationViews(Double visualizationViews) {
    this.visualizationViews = visualizationViews;
  }

  @JsonProperty
  public Double getEventReportViews() {
    return eventReportViews;
  }

  public void setEventReportViews(Double eventReportViews) {
    this.eventReportViews = eventReportViews;
  }

  @JsonProperty
  public Double getEventChartViews() {
    return eventChartViews;
  }

  public void setEventChartViews(Double eventChartViews) {
    this.eventChartViews = eventChartViews;
  }

  @JsonProperty
  public Double getEventVisualizationViews() {
    return eventVisualizationViews;
  }

  public void setEventVisualizationViews(Double eventVisualizationViews) {
    this.eventVisualizationViews = eventVisualizationViews;
  }

  @JsonProperty
  public Double getDashboardViews() {
    return dashboardViews;
  }

  public void setDashboardViews(Double dashboardViews) {
    this.dashboardViews = dashboardViews;
  }

  @JsonProperty
  public Double getPassiveDashboardViews() {
    return passiveDashboardViews;
  }

  public void setPassiveDashboardViews(Double passiveDashboardViews) {
    this.passiveDashboardViews = passiveDashboardViews;
  }

  @JsonProperty
  public Double getDataSetReportViews() {
    return dataSetReportViews;
  }

  public void setDataSetReportViews(Double dataSetReportViews) {
    this.dataSetReportViews = dataSetReportViews;
  }

  @JsonProperty
  public Double getTotalViews() {
    return totalViews;
  }

  public void setTotalViews(Double totalViews) {
    this.totalViews = totalViews;
  }

  @JsonProperty
  public Double getSavedMaps() {
    return savedMaps;
  }

  public void setSavedMaps(Double savedMaps) {
    this.savedMaps = savedMaps;
  }

  @JsonProperty
  public Double getSavedVisualizations() {
    return savedVisualizations;
  }

  public void setSavedVisualizations(Double savedVisualizations) {
    this.savedVisualizations = savedVisualizations;
  }

  @JsonProperty
  public Double getSavedEventReports() {
    return savedEventReports;
  }

  public void setSavedEventReports(Double savedEventReports) {
    this.savedEventReports = savedEventReports;
  }

  @JsonProperty
  public Double getSavedEventCharts() {
    return savedEventCharts;
  }

  public void setSavedEventCharts(Double savedEventCharts) {
    this.savedEventCharts = savedEventCharts;
  }

  @JsonProperty
  public Double getSavedEventVisualizations() {
    return savedEventVisualizations;
  }

  public void setSavedEventVisualizations(Double savedEventVisualizations) {
    this.savedEventVisualizations = savedEventVisualizations;
  }

  @JsonProperty
  public Double getSavedDashboards() {
    return savedDashboards;
  }

  public void setSavedDashboards(Double savedDashboards) {
    this.savedDashboards = savedDashboards;
  }

  @JsonProperty
  public Double getSavedIndicators() {
    return savedIndicators;
  }

  public void setSavedIndicators(Double savedIndicators) {
    this.savedIndicators = savedIndicators;
  }

  @JsonProperty
  public Double getSavedDataValues() {
    return savedDataValues;
  }

  public void setSavedDataValues(Double savedDataValues) {
    this.savedDataValues = savedDataValues;
  }

  @JsonProperty
  public Integer getUsers() {
    return users;
  }

  @JsonProperty
  public void setUsers(Integer users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return super.toString()
        + "DataStatistics{"
        + "mapViews="
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
        + ", totalViews="
        + totalViews
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

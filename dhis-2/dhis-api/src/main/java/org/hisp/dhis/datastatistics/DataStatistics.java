/*
 * Copyright (c) 2004-2025, University of Oslo
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;

/**
 * * @author Julie Hill Roa * @author Yrjan Fraschetti * @author Jason P. Pickering
 *
 * <p>Represents data statistics for various views, saved items, and user activity. Used to capture
 * and report on usage metrics within the system over a specified period.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataStatistics extends BaseIdentifiableObject {

  /** The total number of map views. */
  @JsonProperty private Long mapViews;

  /** The total number of visualization views. */
  @JsonProperty private Long visualizationViews;

  /** The total number of event report views. */
  @JsonProperty private Long eventReportViews;

  /** The total number of event chart views. */
  @JsonProperty private Long eventChartViews;

  /** The total number of event visualization views. */
  @JsonProperty private Long eventVisualizationViews;

  /** The total number of dashboard views. */
  @JsonProperty private Long dashboardViews;

  /** The total number of passive dashboard views. */
  @JsonProperty private Long passiveDashboardViews;

  /** The total number of dataset report views. */
  @JsonProperty private Long dataSetReportViews;

  /** The total number of all views combined. */
  @JsonProperty private Long totalViews;

  /** The total number of saved maps. */
  @JsonProperty private Long savedMaps;

  /** The total number of saved visualizations. */
  @JsonProperty private Long savedVisualizations;

  /** The total number of saved event reports. */
  @JsonProperty private Long savedEventReports;

  /** The total number of saved event charts. */
  @JsonProperty private Long savedEventCharts;

  /** The total number of saved event visualizations. */
  @JsonProperty private Long savedEventVisualizations;

  /** The total number of saved dashboards. */
  @JsonProperty private Long savedDashboards;

  /** The total number of saved indicators. */
  @JsonProperty private Long savedIndicators;

  /** The total number of saved data values. */
  @JsonProperty private Long savedDataValues;

  /** The total number of active users. */
  @JsonProperty private Long activeUsers;

  /** The total number of users. */
  @JsonProperty private Long users;

  /**
   * Constructs a new DataStatistics object with the specified values. Numbers are used here for
   * flexibility, but are converted to Long internally.
   *
   * @param mapViews The total number of map views.
   * @param visualizationViews The total number of visualization views.
   * @param eventReportViews The total number of event report views.
   * @param eventChartViews The total number of event chart views.
   * @param eventVisualizationViews The total number of event visualization views.
   * @param dashboardViews The total number of dashboard views.
   * @param passiveDashboardViews The total number of passive dashboard views.
   * @param dataSetReportViews The total number of dataset report views.
   * @param totalViews The total number of all views combined.
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
  public DataStatistics(
      Number mapViews,
      Number visualizationViews,
      Number eventReportViews,
      Number eventChartViews,
      Number eventVisualizationViews,
      Number dashboardViews,
      Number passiveDashboardViews,
      Number dataSetReportViews,
      Number totalViews,
      Number savedMaps,
      Number savedVisualizations,
      Number savedEventReports,
      Number savedEventCharts,
      Number savedEventVisualizations,
      Number savedDashboards,
      Number savedIndicators,
      Number savedDataValues,
      Number activeUsers,
      Number users) {

    this.mapViews = toLong(mapViews);
    this.visualizationViews = toLong(visualizationViews);
    this.eventReportViews = toLong(eventReportViews);
    this.eventChartViews = toLong(eventChartViews);
    this.eventVisualizationViews = toLong(eventVisualizationViews);
    this.dashboardViews = toLong(dashboardViews);
    this.passiveDashboardViews = toLong(passiveDashboardViews);
    this.dataSetReportViews = toLong(dataSetReportViews);
    this.totalViews = toLong(totalViews);

    this.savedMaps = toLong(savedMaps);
    this.savedVisualizations = toLong(savedVisualizations);
    this.savedEventReports = toLong(savedEventReports);
    this.savedEventCharts = toLong(savedEventCharts);
    this.savedEventVisualizations = toLong(savedEventVisualizations);
    this.savedDashboards = toLong(savedDashboards);
    this.savedIndicators = toLong(savedIndicators);
    this.savedDataValues = toLong(savedDataValues);

    this.activeUsers = toLong(activeUsers);
    this.users = toLong(users);
  }

  /**
   * Converts a Number to a Long.
   *
   * @param n The number to convert.
   * @return The converted Long value, or null if the input is null.
   */
  private static Long toLong(Number n) {
    return n == null ? null : n.longValue();
  }

  /**
   * Returns a string representation of the DataStatistics object.
   *
   * @return A string containing the field values of the object.
   */
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
        + ", dataSetReportViews="
        + dataSetReportViews
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

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

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataStatistics extends BaseIdentifiableObject {

  @JsonProperty private Long mapViews;
  @JsonProperty private Long visualizationViews;
  @JsonProperty private Long eventReportViews;
  @JsonProperty private Long eventChartViews;
  @JsonProperty private Long eventVisualizationViews;
  @JsonProperty private Long dashboardViews;
  @JsonProperty private Long passiveDashboardViews;
  @JsonProperty private Long dataSetReportViews;
  @JsonProperty private Long totalViews;

  @JsonProperty private Long savedMaps;
  @JsonProperty private Long savedVisualizations;
  @JsonProperty private Long savedEventReports;
  @JsonProperty private Long savedEventCharts;
  @JsonProperty private Long savedEventVisualizations;
  @JsonProperty private Long savedDashboards;
  @JsonProperty private Long savedIndicators;
  @JsonProperty private Long savedDataValues;

  @JsonProperty private Long activeUsers;
  @JsonProperty private Long users;

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

  private static Long toLong(Number n) {
    return n == null ? null : n.longValue();
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

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
package org.hisp.dhis.dashboard;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.SimpleEventVisualizationView;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.SimpleVisualizationView;

@JacksonXmlRootElement(localName = "dashboardSearchResult", namespace = DXF_2_0)
public class DashboardSearchResult {
  private List<User> users = new ArrayList<>();

  private List<SimpleVisualizationView> visualizations = new ArrayList<>();

  private List<SimpleEventVisualizationView> eventVisualizations = new ArrayList<>();

  private List<EventVisualization> eventCharts = new ArrayList<>();

  private List<Map> maps = new ArrayList<>();

  private List<EventVisualization> eventReports = new ArrayList<>();

  private List<Report> reports = new ArrayList<>();

  private List<Document> resources = new ArrayList<>();

  private List<App> apps = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  public DashboardSearchResult() {}

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonProperty
  public int getSearchCount() {
    int results = 0;
    results += users.size();
    results += eventVisualizations.size();
    results += visualizations.size();
    results += eventCharts.size();
    results += maps.size();
    results += eventReports.size();
    results += reports.size();
    results += resources.size();
    results += apps.size();
    return results;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getUserCount() {
    return users.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getVisualizationCount() {
    return visualizations.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getEventVisualizationCount() {
    return eventVisualizations.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getEventChartCount() {
    return eventCharts.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getMapCount() {
    return maps.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getEventReportCount() {
    return eventReports.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getReportCount() {
    return reports.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getResourceCount() {
    return resources.size();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getAppCount() {
    return apps.size();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty(value = "users")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "users", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "user", namespace = DXF_2_0)
  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  @JsonProperty(value = "visualizations")
  @JsonSerialize(contentAs = SimpleVisualizationView.class)
  @JacksonXmlElementWrapper(localName = "visualizations", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "visualization", namespace = DXF_2_0)
  public List<SimpleVisualizationView> getVisualizations() {
    return visualizations;
  }

  public void setVisualizations(final List<SimpleVisualizationView> visualizations) {
    this.visualizations = visualizations;
  }

  @JsonProperty(value = "eventVisualizations")
  @JsonSerialize(contentAs = SimpleEventVisualizationView.class)
  @JacksonXmlElementWrapper(localName = "eventVisualizations", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "eventVisualization", namespace = DXF_2_0)
  public List<SimpleEventVisualizationView> getEventVisualizations() {
    return eventVisualizations;
  }

  public void setEventVisualizations(final List<SimpleEventVisualizationView> eventVisualizations) {
    this.eventVisualizations = eventVisualizations;
  }

  @JsonProperty(value = "eventCharts")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "eventCharts", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "eventChart", namespace = DXF_2_0)
  public List<EventVisualization> getEventCharts() {
    return eventCharts;
  }

  public void setEventCharts(List<EventVisualization> eventCharts) {
    this.eventCharts = eventCharts;
  }

  @JsonProperty(value = "maps")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "maps", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "map", namespace = DXF_2_0)
  public List<Map> getMaps() {
    return maps;
  }

  public void setMaps(List<Map> maps) {
    this.maps = maps;
  }

  @JsonProperty(value = "eventReports")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "eventReports", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "eventReport", namespace = DXF_2_0)
  public List<EventVisualization> getEventReports() {
    return eventReports;
  }

  public void setEventReports(List<EventVisualization> eventReports) {
    this.eventReports = eventReports;
  }

  @JsonProperty(value = "reports")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "reports", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "report", namespace = DXF_2_0)
  public List<Report> getReports() {
    return reports;
  }

  public void setReports(List<Report> reports) {
    this.reports = reports;
  }

  @JsonProperty(value = "resources")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "resources", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "resource", namespace = DXF_2_0)
  public List<Document> getResources() {
    return resources;
  }

  public void setResources(List<Document> resources) {
    this.resources = resources;
  }

  @JsonProperty(value = "apps")
  @JsonSerialize(contentAs = App.class)
  @JacksonXmlElementWrapper(localName = "apps", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "apps", namespace = DXF_2_0)
  public List<App> getApps() {
    return apps;
  }

  public void setApps(List<App> apps) {
    this.apps = apps;
  }
}

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
package org.hisp.dhis.dashboard;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.InterpretableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;

/**
 * Represents an item in the dashboard. An item can represent an embedded object or represent links
 * to other objects.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "dashboardItem", namespace = DXF_2_0)
public class DashboardItem extends BaseIdentifiableObject implements EmbeddedObject {
  public static final int MAX_CONTENT = 8;

  private Visualization visualization;

  private EventVisualization eventVisualization;

  private EventChart eventChart;

  private Map map;

  private EventReport eventReport;

  private String text;

  private List<User> users = new ArrayList<>();

  private List<Report> reports = new ArrayList<>();

  private List<Document> resources = new ArrayList<>();

  private Boolean messages;

  private String appKey;

  private DashboardItemShape shape;

  private Integer x;

  private Integer y;

  private Integer height;

  private Integer width;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DashboardItem() {
    setAutoFields();
  }

  public DashboardItem(String uid) {
    this.uid = uid;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DashboardItemType getType() {
    if (visualization != null) {
      return DashboardItemType.VISUALIZATION;
    } else if (eventChart != null) {
      return DashboardItemType.EVENT_CHART;
    } else if (eventReport != null) {
      return DashboardItemType.EVENT_REPORT;
    }
    if (eventVisualization != null) {
      return DashboardItemType.EVENT_VISUALIZATION;
    } else if (map != null) {
      return DashboardItemType.MAP;
    } else if (text != null) {
      return DashboardItemType.TEXT;
    } else if (!users.isEmpty()) {
      return DashboardItemType.USERS;
    } else if (!reports.isEmpty()) {
      return DashboardItemType.REPORTS;
    } else if (!resources.isEmpty()) {
      return DashboardItemType.RESOURCES;
    } else if (messages != null) {
      return DashboardItemType.MESSAGES;
    } else if (appKey != null) {
      return DashboardItemType.APP;
    }

    return null;
  }

  /**
   * Returns the actual item object if this dashboard item represents an embedded item and not links
   * to items.
   */
  public InterpretableObject getEmbeddedItem() {
    if (visualization != null) {
      return visualization;
    } else if (eventChart != null) {
      return eventChart;
    } else if (eventReport != null) {
      return eventReport;
    }
    if (eventVisualization != null) {
      return eventVisualization;
    } else if (map != null) {
      return map;
    }

    return null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getInterpretationCount() {
    InterpretableObject object = getEmbeddedItem();

    return object != null ? object.getInterpretations().size() : 0;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getInterpretationLikeCount() {
    InterpretableObject object = getEmbeddedItem();

    return object != null
        ? object.getInterpretations().stream().mapToInt(Interpretation::getLikes).sum()
        : 0;
  }

  /**
   * Returns a list of the actual item objects if this dashboard item represents a list of objects
   * and not an embedded item.
   */
  public List<? extends IdentifiableObject> getLinkItems() {
    if (!users.isEmpty()) {
      return users;
    } else if (!reports.isEmpty()) {
      return reports;
    } else if (!resources.isEmpty()) {
      return resources;
    }

    return null;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public int getContentCount() {
    int count = 0;
    count += visualization != null ? 1 : 0;
    count += eventVisualization != null ? 1 : 0;
    count += eventChart != null ? 1 : 0;
    count += map != null ? 1 : 0;
    count += eventReport != null ? 1 : 0;
    count += text != null ? 1 : 0;
    count += users.size();
    count += reports.size();
    count += resources.size();
    count += messages != null ? 1 : 0;
    count += appKey != null ? 1 : 0;
    return count;
  }

  /**
   * Removes the content with the given uid. Returns true if a content with the given uid existed
   * and was removed.
   *
   * @param uid the identifier of the content.
   * @return true if a content was removed.
   */
  public boolean removeItemContent(String uid) {
    if (!users.isEmpty()) {
      return removeContent(uid, users);
    } else if (!reports.isEmpty()) {
      return removeContent(uid, reports);
    } else {
      return removeContent(uid, resources);
    }
  }

  private boolean removeContent(String uid, List<? extends IdentifiableObject> content) {
    Iterator<? extends IdentifiableObject> iterator = content.iterator();

    while (iterator.hasNext()) {
      if (uid.equals(iterator.next().getUid())) {
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
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Visualization getVisualization() {
    return visualization;
  }

  public void setVisualization(Visualization visualization) {
    this.visualization = visualization;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventVisualization getEventVisualization() {
    return eventVisualization;
  }

  public void setEventVisualization(EventVisualization eventVisualization) {
    this.eventVisualization = eventVisualization;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventChart getEventChart() {
    return eventChart;
  }

  public void setEventChart(EventChart eventChart) {
    this.eventChart = eventChart;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Map getMap() {
    return map;
  }

  public void setMap(Map map) {
    this.map = map;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventReport getEventReport() {
    return eventReport;
  }

  public void setEventReport(EventReport eventReport) {
    this.eventReport = eventReport;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getText() {
    return text;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "text")
  public String getDisplayText() {
    return getTranslation("TEXT", getText());
  }

  public void setText(String text) {
    this.text = text;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto[].class)
  @JsonProperty
  @JsonSerialize(contentUsing = UserPropertyTransformer.JacksonSerialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlElementWrapper(localName = "users", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "user", namespace = DxfNamespaces.DXF_2_0)
  public List<User> getUsers() {
    return users;
  }

  @JsonDeserialize(contentUsing = UserPropertyTransformer.JacksonDeserialize.class)
  public void setUsers(List<User> users) {
    this.users = users;
  }

  @JsonProperty("reports")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "reports", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "report", namespace = DXF_2_0)
  public List<Report> getReports() {
    return reports;
  }

  public void setReports(List<Report> reports) {
    this.reports = reports;
  }

  @JsonProperty("resources")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "resources", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "resource", namespace = DXF_2_0)
  public List<Document> getResources() {
    return resources;
  }

  public void setResources(List<Document> resources) {
    this.resources = resources;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Boolean getMessages() {
    return messages;
  }

  public void setMessages(Boolean messages) {
    this.messages = messages;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getAppKey() {
    return appKey;
  }

  public void setAppKey(String appKey) {
    this.appKey = appKey;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DashboardItemShape getShape() {
    return shape;
  }

  public void setShape(DashboardItemShape shape) {
    this.shape = shape;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getX() {
    return x;
  }

  public void setX(Integer x) {
    this.x = x;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getY() {
    return y;
  }

  public void setY(Integer y) {
    this.y = y;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }
}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.InterpretableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.visualization.Visualization;

/**
 * Represents an item in the dashboard. An item can represent an embedded object or represent links
 * to other objects.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "dashboardItem", namespace = DXF_2_0)
@Entity
@Setter
@Table(name = "dashboarditem")
public class DashboardItem implements IdentifiableObject, EmbeddedObject {
  public static final int MAX_CONTENT = 8;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "dashboarditemid")
  private long id;

  @Column(name = "code")
  private String code;

  @ManyToOne
  @JoinColumn(name = "visualizationid")
  private Visualization visualization;

  @ManyToOne
  @JoinColumn(name = "eventvisualizationid")
  private EventVisualization eventVisualization;

  @ManyToOne
  @JoinColumn(name = "eventchartid")
  private EventChart eventChart;

  @ManyToOne
  @JoinColumn(name = "mapid")
  private Map map;

  @ManyToOne
  @JoinColumn(name = "eventreport")
  private EventReport eventReport;

  @Column(name = "textcontent")
  private String text;

  @ManyToMany
  @JoinTable(
      name = "dashboarditem_users",
      joinColumns = @JoinColumn(name = "dashboarditemid"),
      inverseJoinColumns = @JoinColumn(name = "userid"))
  @OrderColumn(name = "sort_order")
  private List<User> users = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "dashboarditem_reports",
      joinColumns = @JoinColumn(name = "dashboarditemid"),
      inverseJoinColumns = @JoinColumn(name = "reportid"))
  @OrderColumn(name = "sort_order")
  private List<Report> reports = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "dashboarditem_resources",
      joinColumns = @JoinColumn(name = "dashboarditemid"),
      inverseJoinColumns = @JoinColumn(name = "resourceid"))
  @OrderColumn(name = "sort_order")
  private List<Document> resources = new ArrayList<>();

  private Boolean messages;

  private String appKey;

  @Column(name = "shape", length = 50)
  @Enumerated(EnumType.STRING)
  private DashboardItemShape shape;

  private Integer x;

  private Integer y;

  private Integer height;

  private Integer width;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  protected String uid;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastUpdated;

  @ManyToOne
  @JoinColumn(name = "lastupdatedby")
  protected User lastUpdatedBy;

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
  // Transient fields
  // -------------------------------------------------------------------------
  private transient Access access;

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

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventChart getEventChart() {
    return eventChart;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Map getMap() {
    return map;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EventReport getEventReport() {
    return eventReport;
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
    return translations.getTranslation("TEXT", getText());
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

  @JsonProperty("resources")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "resources", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "resource", namespace = DXF_2_0)
  public List<Document> getResources() {
    return resources;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Boolean getMessages() {
    return messages;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public String getAppKey() {
    return appKey;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public DashboardItemShape getShape() {
    return shape;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getX() {
    return x;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getY() {
    return y;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getHeight() {
    return height;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Integer getWidth() {
    return width;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  public String getUid() {
    return uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getHref() {
    return DashboardItem.class.isAssignableFrom(getClass())
        ? "/dashboardItems/" + getUid()
        : "/" + getClass().getSimpleName().toLowerCase() + "s/" + getUid();
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getCode() {
    return code;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translations.getTranslation("NAME", getName());
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Date getCreated() {
    return created;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations.setTranslations(translations);
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
  public void setAccess(Access access) {
    this.access = access;
  }

  @Override
  @JsonIgnore
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  @JsonIgnore
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------------------------------
  // Not supported by DashboardItem
  // -------------------------------------------------------------------------

  @Override
  public Set<String> getFavorites() {
    return Set.of();
  }

  @Override
  @Deprecated
  public boolean isFavorite() {
    return false;
  }

  @Override
  public boolean setAsFavorite(UserDetails user) {
    return false;
  }

  @Override
  public boolean removeAsFavorite(UserDetails user) {
    return false;
  }

  @Override
  @Deprecated
  @JsonIgnore
  /** DashboardItem does not support sharing. */
  public Sharing getSharing() {
    return null;
  }

  @Override
  public void setSharing(Sharing sharing) {}

  @Override
  public void setOwner(String owner) {}

  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {}

  @Override
  public void addAttributeValue(String attributeId, String value) {}

  @Override
  public void removeAttributeValue(String attributeId) {}

  @Override
  public void setHref(String link) {}

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void setName(String name) {}

  @Override
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @JsonIgnore
  public User getCreatedBy() {
    return null;
  }

  @Override
  @Deprecated
  public User getUser() {
    return null;
  }

  @Override
  public void setCreatedBy(User createdBy) {}

  @Override
  @Deprecated
  public void setUser(User user) {}
}

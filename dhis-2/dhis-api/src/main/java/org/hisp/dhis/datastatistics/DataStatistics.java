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

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * * @author Julie Hill Roa * @author Yrjan Fraschetti * @author Jason P. Pickering
 *
 * <p>Represents data statistics for various views, saved items, and user activity. Used to capture
 * and report on usage metrics within the system over a specified period.
 *
 * <p>The table has no name, creator, sharing, translations or attribute values columns, so this
 * entity implements {@link IdentifiableObject} directly and those properties are no-op stubs at the
 * end of the class.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "datastatistics")
@JacksonXmlRootElement(localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0)
public class DataStatistics implements IdentifiableObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "statisticsid")
  private long id;

  @AuditAttribute
  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @AuditAttribute
  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastupdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lastupdatedby")
  private User lastUpdatedBy;

  /** The total number of map views. */
  @JsonProperty
  @Column(name = "mapviews")
  private Long mapViews;

  /** The total number of visualization views. */
  @JsonProperty
  @Column(name = "visualizationviews")
  private Long visualizationViews;

  /** The total number of event report views. */
  @JsonProperty
  @Column(name = "eventreportviews")
  private Long eventReportViews;

  /** The total number of event chart views. */
  @JsonProperty
  @Column(name = "eventchartviews")
  private Long eventChartViews;

  /** The total number of event visualization views. */
  @JsonProperty
  @Column(name = "eventvisualizationviews")
  private Long eventVisualizationViews;

  /** The total number of dashboard views. */
  @JsonProperty
  @Column(name = "dashboardviews")
  private Long dashboardViews;

  /** The total number of passive dashboard views. */
  @JsonProperty
  @Column(name = "passivedashboardviews")
  private Long passiveDashboardViews;

  /** The total number of dataset report views. */
  @JsonProperty
  @Column(name = "datasetreportviews")
  private Long dataSetReportViews;

  /** The total number of all views combined. */
  @JsonProperty
  @Column(name = "totalviews")
  private Long totalViews;

  /** The total number of saved maps. */
  @JsonProperty
  @Column(name = "maps")
  private Long savedMaps;

  /** The total number of saved visualizations. */
  @JsonProperty
  @Column(name = "visualizations")
  private Long savedVisualizations;

  /** The total number of saved event reports. */
  @JsonProperty
  @Column(name = "eventreports")
  private Long savedEventReports;

  /** The total number of saved event charts. */
  @JsonProperty
  @Column(name = "eventcharts")
  private Long savedEventCharts;

  /** The total number of saved event visualizations. */
  @JsonProperty
  @Column(name = "eventvisualizations")
  private Long savedEventVisualizations;

  /** The total number of saved dashboards. */
  @JsonProperty
  @Column(name = "dashboards")
  private Long savedDashboards;

  /** The total number of saved indicators. */
  @JsonProperty
  @Column(name = "indicators")
  private Long savedIndicators;

  /** The total number of saved data values. */
  @JsonProperty
  @Column(name = "datavalues")
  private Long savedDataValues;

  /** The total number of active users. */
  @JsonProperty
  @Column(name = "active_users")
  private Long activeUsers;

  /** The total number of users. */
  @JsonProperty
  @Column(name = "users")
  private Long users;

  /** Link to this object in the API. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

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
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
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
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);

    return result;
  }

  /**
   * Same real class (Hibernate proxies resolved) and the same {@code uid} and {@code code}, read
   * through the getters. The name is not compared, as it is always null.
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof DataStatistics other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode());
  }

  /**
   * Returns a string representation of the DataStatistics object.
   *
   * @return A string containing the field values of the object.
   */
  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + getId()
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"code\":\""
        + getCode()
        + "\", "
        + "\"name\":\""
        + getName()
        + "\", "
        + "\"created\":\""
        + getCreated()
        + "\", "
        + "\"lastUpdated\":\""
        + getLastUpdated()
        + "\" "
        + "}"
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

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** DataStatistics does not support a name. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return null;
  }

  /** DataStatistics does not support a name. */
  @Override
  @Deprecated(since = "2.44")
  public void setName(String name) {
    // Not supported
  }

  /** DataStatistics does not support a name. */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return null;
  }

  /** DataStatistics does not support translations. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return new HashSet<>();
  }

  /** DataStatistics does not support translations. */
  @Override
  @Deprecated(since = "2.44")
  public void setTranslations(Set<Translation> translations) {
    // Not supported
  }

  /** DataStatistics does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return null;
  }

  /** DataStatistics does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  public void setCreatedBy(User createdBy) {
    // Not supported
  }

  /** DataStatistics does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return null;
  }

  /** DataStatistics does not support a creator. */
  @Override
  @Deprecated(since = "2.44")
  public void setUser(User user) {
    // Not supported
  }

  /** DataStatistics does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setOwner(String owner) {
    // Not supported
  }

  /**
   * DataStatistics does not support sharing. Returns a new {@link Sharing} rather than {@link
   * Sharing#empty()} to keep the shape of the former in-memory default.
   */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return new Sharing();
  }

  /** DataStatistics does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** DataStatistics does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** DataStatistics does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** DataStatistics does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** DataStatistics does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}

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
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dashboard.design.ItemConfig;
import org.hisp.dhis.dashboard.design.Layout;
import org.hisp.dhis.dashboard.embedded.EmbeddedDashboard;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Encapsulates information about an embedded dashboard. An embedded dashboard is typically loaded
 * from an external provider.
 *
 * @author Lars Helge Overland
 */
@Entity
@Table(name = "dashboard")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NoArgsConstructor
@JacksonXmlRootElement(localName = "dashboard", namespace = DxfNamespaces.DXF_2_0)
public class Dashboard extends BaseMetadataObject implements IdentifiableObject {
  public static final int MAX_ITEMS = 40;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "dashboardid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 230)
  private String name;
  
  private String description;

  @JoinTable(
      name = "dashboard_items",
      joinColumns = @JoinColumn(
          name = "dashboardid", 
          foreignKey = @ForeignKey(name = "fk_dashboard_items_dashboardid")),
      inverseJoinColumns = @JoinColumn(
          name = "dashboarditemid", 
          foreignKey = @ForeignKey(name = "fk_dashboard_items_dashboarditemid")))
  @OrderColumn(name = "sort_order")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<DashboardItem> items = new ArrayList<>();

  @Type(type = "jblDashboardLayout")
  private Layout layout;

  @Type(type = "jbDashboardItemConfig")
  private ItemConfig itemConfig;

  /**
   * Whether filter dimensions are restricted for the dashboard. The allowed filter dimensions are
   * specified by {@link Dashboard#allowedFilters}.
   */
  @Column(name = "restrictfilters")
  private boolean restrictFilters;

  /** Allowed filter dimensions (if any) which may be used for the dashboard. */
  @Type(type = "jbList")
  @Column(name = "allowedfilters")
  private List<String> allowedFilters = new ArrayList<>();

  /** Optional, only set if this dashboard is embedded and loaded from an external provider. */
  @Type(type = "jbEmbeddedDashboard")
  @Column(name = "embedded")
  private EmbeddedDashboard embedded;

  @Column(name = "sharing")
  @Type(type = "jsbObjectSharing")
  private Sharing sharing = new Sharing();

  // ----------------------------------------------------------------
  // Transient properties
  // ----------------------------------------------------------------
  private transient String href;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Dashboard(String name) {
    this.name = name;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Indicates whether this dashboard has at least one item. */
  public boolean hasItems() {
    return items != null && !items.isEmpty();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getItemCount() {
    return items == null ? 0 : items.size();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty("dashboardItems")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "dashboardItems", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "dashboardItem", namespace = DxfNamespaces.DXF_2_0)
  public List<DashboardItem> getItems() {
    return items;
  }

  public void setItems(List<DashboardItem> items) {
    this.items = items;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public Layout getLayout() {
    return layout;
  }

  public void setLayout(Layout layout) {
    this.layout = layout;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public ItemConfig getItemConfig() {
    return itemConfig;
  }

  public void setItemConfig(ItemConfig itemConfig) {
    this.itemConfig = itemConfig;
  }

  @JsonProperty
  @JacksonXmlProperty
  public boolean isRestrictFilters() {
    return restrictFilters;
  }

  public void setRestrictFilters(boolean restrictFilters) {
    this.restrictFilters = restrictFilters;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "allowedFilters", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "allowedFilter", namespace = DxfNamespaces.DXF_2_0)
  public List<String> getAllowedFilters() {
    return allowedFilters;
  }

  public void setAllowedFilters(List<String> allowedFilters) {
    this.allowedFilters = allowedFilters;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public EmbeddedDashboard getEmbedded() {
    return embedded;
  }

  public void setEmbedded(EmbeddedDashboard embedded) {
    this.embedded = embedded;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDisplayName() {
    return name;
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
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return createdBy;
  }

  @Override
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return createdBy;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // Do nothing, not supported
  }

  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // Do nothing, not supported
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    // Do nothing, not supported
  }

  @Override
  public Set<String> getFavorites() {
    return Set.of();
  }

  @Override
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
  public Access getAccess() {
    return null;
  }

  @Override
  public void setAccess(Access access) {
    // Do nothing, not supported
  }

  @Override
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }
    return sharing;
  }

  @Override
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  @Override
  public void setOwner(String owner) {
    getSharing().setOwner(owner);
  }

  @Override
  public Set<Translation> getTranslations() {
    return Set.of();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    // Do nothing, not supported
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
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
  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  public void setLastUpdatedBy(User user) {
    this.lastUpdatedBy = user;
  }

  @Override
  public String getHref() {
    return href;
  }

  @Override
  public void setHref(String href) {
    this.href = href;
  }

  public void setPublicAccess(String access) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.setPublicAccess(access);
  }
}

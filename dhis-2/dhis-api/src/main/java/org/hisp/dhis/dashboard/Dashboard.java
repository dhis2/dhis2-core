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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dashboard.design.ItemConfig;
import org.hisp.dhis.dashboard.design.Layout;
import org.hisp.dhis.dashboard.embedded.EmbeddedDashboard;

/**
 * Encapsulates information about an embedded dashboard. An embedded dashboard is typically loaded
 * from an external provider.
 *
 * @author Lars Helge Overland
 */
@NoArgsConstructor
@JacksonXmlRootElement(localName = "dashboard", namespace = DxfNamespaces.DXF_2_0)
public class Dashboard extends BaseNameableObject implements MetadataObject {
  public static final int MAX_ITEMS = 40;

  private List<DashboardItem> items = new ArrayList<>();

  private Layout layout;

  private ItemConfig itemConfig;

  /**
   * Whether filter dimensions are restricted for the dashboard. The allowed filter dimensions are
   * specified by {@link Dashboard#allowedFilters}.
   */
  private boolean restrictFilters;

  /** Allowed filter dimensions (if any) which may be used for the dashboard. */
  private List<String> allowedFilters = new ArrayList<>();

  /** Optional, only set if this dashboard is embedded and loaded from an external provider. */
  private EmbeddedDashboard embedded;

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
}

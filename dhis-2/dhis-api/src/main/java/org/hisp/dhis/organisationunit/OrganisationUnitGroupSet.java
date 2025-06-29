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
package org.hisp.dhis.organisationunit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;

/**
 * @author Kristian Nordal
 */
@JacksonXmlRootElement(localName = "organisationUnitGroupSet", namespace = DxfNamespaces.DXF_2_0)
public class OrganisationUnitGroupSet extends BaseDimensionalObject implements MetadataObject {
  private boolean compulsory;

  private boolean includeSubhierarchyInAnalytics;

  private Set<OrganisationUnitGroup> organisationUnitGroups = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public OrganisationUnitGroupSet() {}

  public OrganisationUnitGroupSet(String name, String description, boolean compulsory) {
    this.name = name;
    this.description = description;
    this.compulsory = compulsory;
    this.includeSubhierarchyInAnalytics = false;
  }

  public OrganisationUnitGroupSet(
      String name, String description, boolean compulsory, boolean dataDimension) {
    this(name, description, compulsory);
    this.dataDimension = dataDimension;
    this.includeSubhierarchyInAnalytics = false;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void addOrganisationUnitGroup(OrganisationUnitGroup organisationUnitGroup) {
    organisationUnitGroups.add(organisationUnitGroup);
    organisationUnitGroup.getGroupSets().add(this);
  }

  public void removeOrganisationUnitGroup(OrganisationUnitGroup organisationUnitGroup) {
    organisationUnitGroups.remove(organisationUnitGroup);
    organisationUnitGroup.getGroupSets().remove(this);
  }

  public void removeAllOrganisationUnitGroups() {
    for (OrganisationUnitGroup group : organisationUnitGroups) {
      group.getGroupSets().remove(this);
    }

    organisationUnitGroups.clear();
  }

  public Collection<OrganisationUnit> getOrganisationUnits() {
    List<OrganisationUnit> units = new ArrayList<>();

    for (OrganisationUnitGroup group : organisationUnitGroups) {
      units.addAll(group.getMembers());
    }

    return units;
  }

  public boolean isMemberOfOrganisationUnitGroups(OrganisationUnit organisationUnit) {
    for (OrganisationUnitGroup group : organisationUnitGroups) {
      if (group.getMembers().contains(organisationUnit)) {
        return true;
      }
    }

    return false;
  }

  public boolean hasOrganisationUnitGroups() {
    return organisationUnitGroups != null && organisationUnitGroups.size() > 0;
  }

  public OrganisationUnitGroup getGroup(OrganisationUnit unit) {
    for (OrganisationUnitGroup group : organisationUnitGroups) {
      if (group.getMembers().contains(unit)) {
        return group;
      }
    }

    return null;
  }

  public List<OrganisationUnitGroup> getSortedGroups() {
    List<OrganisationUnitGroup> sortedGroups = new ArrayList<>(organisationUnitGroups);

    Collections.sort(sortedGroups);

    return sortedGroups;
  }

  // -------------------------------------------------------------------------
  // Dimensional object
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JsonSerialize(contentAs = DimensionalItemObject.class)
  @JacksonXmlElementWrapper(localName = "items", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "item", namespace = DxfNamespaces.DXF_2_0)
  public List<DimensionalItemObject> getItems() {
    return new ArrayList<>(organisationUnitGroups);
  }

  @Override
  public DimensionType getDimensionType() {
    return DimensionType.ORGANISATION_UNIT_GROUP_SET;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCompulsory() {
    return compulsory;
  }

  public void setCompulsory(boolean compulsory) {
    this.compulsory = compulsory;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isIncludeSubhierarchyInAnalytics() {
    return includeSubhierarchyInAnalytics;
  }

  public void setIncludeSubhierarchyInAnalytics(boolean includeSubhierarchyInAnalytics) {
    this.includeSubhierarchyInAnalytics = includeSubhierarchyInAnalytics;
  }

  @JsonProperty("organisationUnitGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "organisationUnitGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnitGroup", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnitGroup> getOrganisationUnitGroups() {
    return organisationUnitGroups;
  }

  public void setOrganisationUnitGroups(Set<OrganisationUnitGroup> organisationUnitGroups) {
    this.organisationUnitGroups = organisationUnitGroups;
  }
}

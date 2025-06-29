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
package org.hisp.dhis.option;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@JacksonXmlRootElement(localName = "optionGroupSet", namespace = DxfNamespaces.DXF_2_0)
public class OptionGroupSet extends BaseDimensionalObject implements MetadataObject {
  private List<OptionGroup> members = new ArrayList<>();

  private OptionSet optionSet;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public OptionGroupSet() {}

  public OptionGroupSet(String name) {
    this.name = name;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty("optionGroups")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "optionGroups", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "optionGroup", namespace = DxfNamespaces.DXF_2_0)
  public List<OptionGroup> getMembers() {
    return members;
  }

  public void setMembers(List<OptionGroup> members) {
    this.members = members;
  }

  @JsonProperty("optionSet")
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(localName = "optionSet", namespace = DxfNamespaces.DXF_2_0)
  public OptionSet getOptionSet() {
    return optionSet;
  }

  public void setOptionSet(OptionSet optionSet) {
    this.optionSet = optionSet;
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
    return Lists.newArrayList(members);
  }

  @Override
  public DimensionType getDimensionType() {
    return DimensionType.OPTION_GROUP_SET;
  }

  public void addOptionGroup(OptionGroup optionGroup) {
    members.add(optionGroup);
  }

  public Collection<Option> getOptions() {
    List<Option> options = new ArrayList<>();

    for (OptionGroup group : members) {
      options.addAll(group.getMembers());
    }

    return options;
  }
}

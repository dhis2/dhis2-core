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
package org.hisp.dhis.analytics.orgunit;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;

/**
 * @author Lars Helge Overland
 */
public class OrgUnitQueryParams {
  /** Organisation units to query. */
  private List<OrganisationUnit> orgUnits = new ArrayList<>();

  /** Organisation unit group sets to query. */
  private List<OrganisationUnitGroupSet> orgUnitGroupSets = new ArrayList<>();

  /** Organisation unit group sets to use as columns in a table layout. */
  private List<DimensionalObject> columns = new ArrayList<>();

  /** Organisation unit level to query, set internally. */
  private transient int orgUnitLevel;

  private OrgUnitQueryParams() {}

  public List<OrganisationUnit> getOrgUnits() {
    return orgUnits;
  }

  public List<OrganisationUnitGroupSet> getOrgUnitGroupSets() {
    return orgUnitGroupSets;
  }

  public List<DimensionalObject> getColumns() {
    return columns;
  }

  public List<DimensionalObject> getRows() {
    List<DimensionalObject> rows = new ArrayList<>();
    rows.add(
        new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, orgUnits));
    rows.addAll(orgUnitGroupSets);
    rows.removeAll(columns);
    return rows;
  }

  public int getOrgUnitLevel() {
    return orgUnitLevel;
  }

  public boolean isTableLayout() {
    return !columns.isEmpty();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("orgUnits", orgUnits)
        .append("orgUnitGroupSets", orgUnitGroupSets)
        .append("columns", columns)
        .append("isTableLayout", isTableLayout())
        .build();
  }

  public OrgUnitQueryParams getInstance() {
    OrgUnitQueryParams params = new OrgUnitQueryParams();
    params.orgUnits = Lists.newArrayList(this.orgUnits);
    params.orgUnitGroupSets = Lists.newArrayList(this.orgUnitGroupSets);
    return params;
  }

  public static class Builder {
    private OrgUnitQueryParams params;

    public Builder() {
      this.params = new OrgUnitQueryParams();
    }

    public Builder(OrgUnitQueryParams params) {
      this.params = params.getInstance();
    }

    public Builder withOrgUnits(List<OrganisationUnit> orgUnits) {
      this.params.orgUnits = orgUnits;
      return this;
    }

    public Builder withOrgUnitGroupSets(List<OrganisationUnitGroupSet> orgUnitGroupSets) {
      this.params.orgUnitGroupSets = orgUnitGroupSets;
      return this;
    }

    public Builder withColumns(List<DimensionalObject> columns) {
      this.params.columns = columns;
      return this;
    }

    public Builder withOrgUnitLevel(int orgUnitLevel) {
      this.params.orgUnitLevel = orgUnitLevel;
      return this;
    }

    public OrgUnitQueryParams build() {
      return this.params;
    }
  }
}

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
package org.hisp.dhis.merge.orgunit;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Encapsulation of an org unit merge request.
 *
 * @author Lars Helge Overland
 */
@Getter
public class OrgUnitMergeRequest {
  private Set<OrganisationUnit> sources = new HashSet<>();

  private OrganisationUnit target;

  private DataMergeStrategy dataValueMergeStrategy;

  private DataMergeStrategy dataApprovalMergeStrategy;

  private boolean deleteSources;

  public Set<OrganisationUnit> getSources() {
    return ImmutableSet.copyOf(sources);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sources", IdentifiableObjectUtils.getUids(sources))
        .add("target", target != null ? target.getUid() : null)
        .add("deleteSources", deleteSources)
        .toString();
  }

  public static class Builder {
    private OrgUnitMergeRequest request;

    public Builder() {
      this.request = new OrgUnitMergeRequest();

      this.request.dataValueMergeStrategy = DataMergeStrategy.LAST_UPDATED;
      this.request.dataApprovalMergeStrategy = DataMergeStrategy.LAST_UPDATED;
      this.request.deleteSources = true;
    }

    public Builder addSource(OrganisationUnit source) {
      this.request.sources.add(source);
      return this;
    }

    public Builder addSources(Set<OrganisationUnit> sources) {
      this.request.sources.addAll(sources);
      return this;
    }

    public Builder withTarget(OrganisationUnit target) {
      this.request.target = target;
      return this;
    }

    public Builder withDataValueMergeStrategy(DataMergeStrategy dataValueMergeStrategy) {
      this.request.dataValueMergeStrategy =
          firstNonNull(dataValueMergeStrategy, this.request.dataValueMergeStrategy);
      return this;
    }

    public Builder withDataApprovalMergeStrategy(DataMergeStrategy dataApprovalMergeStrategy) {
      this.request.dataApprovalMergeStrategy =
          firstNonNull(dataApprovalMergeStrategy, this.request.dataApprovalMergeStrategy);
      return this;
    }

    public Builder withDeleteSources(Boolean deleteSources) {
      this.request.deleteSources = firstNonNull(deleteSources, this.request.deleteSources);
      return this;
    }

    public OrgUnitMergeRequest build() {
      return this.request;
    }
  }
}

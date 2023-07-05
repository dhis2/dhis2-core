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
package org.hisp.dhis.split.orgunit;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Encapsulation of an org unit split request.
 *
 * @author Lars Helge Overland
 */
@Getter
public class OrgUnitSplitRequest {
  private OrganisationUnit source;

  private Set<OrganisationUnit> targets = new HashSet<>();

  private OrganisationUnit primaryTarget;

  private boolean deleteSource;

  public Set<OrganisationUnit> getTargets() {
    return ImmutableSet.copyOf(targets);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("source", source != null ? source.getUid() : null)
        .add("targets", IdentifiableObjectUtils.getUids(targets))
        .add("primaryTarget", primaryTarget != null ? primaryTarget.getUid() : null)
        .add("deleteSource", deleteSource)
        .toString();
  }

  public static class Builder {
    private OrgUnitSplitRequest request;

    public Builder() {
      this.request = new OrgUnitSplitRequest();

      this.request.deleteSource = true;
    }

    public Builder withSource(OrganisationUnit source) {
      this.request.source = source;
      return this;
    }

    public Builder addTarget(OrganisationUnit target) {
      this.request.targets.add(target);
      return this;
    }

    public Builder addTargets(Set<OrganisationUnit> targets) {
      this.request.targets.addAll(targets);
      return this;
    }

    public Builder withPrimaryTarget(OrganisationUnit primaryTarget) {
      this.request.primaryTarget = primaryTarget;
      return this;
    }

    public Builder withDeleteSource(Boolean deleteSource) {
      this.request.deleteSource = firstNonNull(deleteSource, this.request.deleteSource);
      return this;
    }

    public OrgUnitSplitRequest build() {
      return request;
    }
  }
}

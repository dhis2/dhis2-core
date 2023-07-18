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
package org.hisp.dhis.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorReport;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CascadeSharingReport {
  @JsonProperty private List<ErrorReport> errorReports = new ArrayList<>();

  /** Number of DashboardItem updated for cascade sharing */
  @JsonProperty private int countUpdatedDashboardItems = 0;

  /**
   * Map contains objects that will be updated for cascade sharing.
   *
   * <p>Key: Object Class name
   *
   * <p>Value: Set of UIDs of updated objects
   */
  @JsonProperty private Map<String, Set<IdObject>> updateObjects = new HashMap<>();

  public void addUpdatedObject(String key, IdentifiableObject object) {
    Set<IdObject> typeReport = getUpdateObjects().get(key);

    if (typeReport == null) {
      typeReport = new HashSet<>();
    }

    typeReport.add(new IdObject(object.getUid(), object.getDisplayName()));
    getUpdateObjects().put(key, typeReport);
  }

  public void incUpdatedDashboardItem() {
    countUpdatedDashboardItems++;
  }

  public boolean hasErrors() {
    return !CollectionUtils.isEmpty(errorReports);
  }

  public class IdObject {
    @JsonProperty private String id;

    @JsonProperty private String name;

    public IdObject(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }
}

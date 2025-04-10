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
package org.hisp.dhis.analytics.orgunit.data;

import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.data.QueryPlannerUtils;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryParams;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryPlanner;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.orgunit.OrgUnitQueryPlanner")
@NoArgsConstructor
public class DefaultOrgUnitQueryPlanner implements OrgUnitQueryPlanner {
  @Override
  public List<OrgUnitQueryParams> planQuery(OrgUnitQueryParams params) {
    return groupByOrgUnitLevel(params);
  }

  /**
   * Groups the given query by organisation unit level.
   *
   * @param params the {@link OrgUnitQueryParams}.
   * @return a list of {@link OrgUnitQueryParams}.
   */
  private List<OrgUnitQueryParams> groupByOrgUnitLevel(OrgUnitQueryParams params) {
    List<OrgUnitQueryParams> queries = new ArrayList<>();

    if (!params.getOrgUnits().isEmpty()) {
      ListMap<Integer, OrganisationUnit> levelOrgUnitMap =
          QueryPlannerUtils.getLevelOrgUnitTypedMap(params.getOrgUnits());

      for (Integer level : levelOrgUnitMap.keySet()) {
        OrgUnitQueryParams query =
            new OrgUnitQueryParams.Builder(params)
                .withOrgUnits(levelOrgUnitMap.get(level))
                .withOrgUnitLevel(level)
                .build();

        queries.add(query);
      }
    } else {
      queries.add(new OrgUnitQueryParams.Builder(params).build());
      return queries;
    }

    return queries;
  }
}

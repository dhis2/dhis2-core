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
package org.hisp.dhis.dxf2.deprecated.tracker.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackerTest;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityAggregateUserTest extends TrackerTest {
  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  private User superUser;

  private User nonSuperUser;

  @BeforeEach
  void setUp() {
    doInTransaction(
        () -> {
          superUser = preCreateInjectAdminUser();
          injectSecurityContext(superUser);

          nonSuperUser = createUserWithAuth("testUser2");
          nonSuperUser.addOrganisationUnit(organisationUnitA);
          nonSuperUser.getTeiSearchOrganisationUnits().add(organisationUnitA);
          nonSuperUser.getTeiSearchOrganisationUnits().add(organisationUnitB);
          userService.updateUser(nonSuperUser);

          dbmsManager.clearSession();
        });
  }

  @Test
  void testFetchTrackedEntityInstances() {
    doInTransaction(
        () -> {
          this.persistTrackedEntity();
          this.persistTrackedEntity();
          this.persistTrackedEntity();
          this.persistTrackedEntity();
        });
    TrackedEntityQueryParams queryParams = new TrackedEntityQueryParams();
    queryParams.setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    queryParams.setTrackedEntityType(trackedEntityTypeA);
    queryParams.setIncludeAllAttributes(true);
    TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
    final List<TrackedEntityInstance> trackedEntityInstances =
        trackedEntityInstanceService.getTrackedEntityInstances(queryParams, params, false, true);
    assertThat(trackedEntityInstances, hasSize(4));
    assertThat(trackedEntityInstances.get(0).getEnrollments(), hasSize(0));
    // Check further for explicit uid in param
    queryParams
        .getTrackedEntityUids()
        .addAll(
            trackedEntityInstances.stream()
                .limit(2)
                .map(TrackedEntityInstance::getTrackedEntityInstance)
                .collect(Collectors.toSet()));
    final List<TrackedEntityInstance> limitedTTrackedEntityInstances =
        trackedEntityInstanceService.getTrackedEntityInstances(queryParams, params, false, true);
    assertThat(limitedTTrackedEntityInstances, hasSize(2));
  }
}

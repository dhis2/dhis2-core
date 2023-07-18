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
import static org.hisp.dhis.security.acl.AccessStringHelper.DATA_READ;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.deprecated.tracker.TrackerTest;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityAttributesAggregateAclTest extends TrackerTest {
  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  private User superUser;

  @Test
  void verifyTeiCantBeAccessedNoPublicAccessOnTrackedEntityType() {
    doInTransaction(
        () -> {
          this.persistTrackedEntity();
          this.persistTrackedEntity();
          this.persistTrackedEntity();
          this.persistTrackedEntity();
        });
    TrackedEntityQueryParams queryParams = new TrackedEntityQueryParams();
    queryParams.setOrgUnits(Sets.newHashSet(organisationUnitA));
    queryParams.setTrackedEntityType(trackedEntityTypeA);
    queryParams.setIncludeAllAttributes(true);
    TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
    final List<TrackedEntityInstance> trackedEntityInstances =
        trackedEntityInstanceService.getTrackedEntityInstances(queryParams, params, false, true);
    assertThat(trackedEntityInstances, hasSize(0));
  }

  @Test
  void verifyTeiCanBeAccessedWhenDATA_READPublicAccessOnTrackedEntityType() {
    final String tetUid = CodeGenerator.generateUid();
    doInTransaction(
        () -> {
          injectSecurityContext(superUser);
          TrackedEntityType trackedEntityTypeZ = createTrackedEntityType('Z');
          trackedEntityTypeZ.setUid(tetUid);
          trackedEntityTypeZ.setName("TrackedEntityTypeZ" + trackedEntityTypeZ.getUid());
          trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeZ);
          // When saving the trackedEntityType using addTrackedEntityType, the
          // public access value is ignored
          // therefore we need to update the previously saved TeiType
          final TrackedEntityType trackedEntityType =
              trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeZ.getUid());
          trackedEntityType.setPublicAccess(DATA_READ);
          trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);
          this.persistTrackedEntity(Map.of("trackedEntityType", trackedEntityType));
          this.persistTrackedEntity(Map.of("trackedEntityType", trackedEntityType));
          this.persistTrackedEntity();
          this.persistTrackedEntity();
        });
    final TrackedEntityType trackedEntityType =
        trackedEntityTypeService.getTrackedEntityType(tetUid);
    TrackedEntityQueryParams queryParams = new TrackedEntityQueryParams();
    queryParams.setOrgUnits(Sets.newHashSet(organisationUnitA));
    queryParams.setTrackedEntityType(trackedEntityType);
    queryParams.setIncludeAllAttributes(true);
    TrackedEntityInstanceParams params = TrackedEntityInstanceParams.FALSE;
    final List<TrackedEntityInstance> trackedEntityInstances =
        trackedEntityInstanceService.getTrackedEntityInstances(queryParams, params, false, true);
    assertThat(trackedEntityInstances, hasSize(2));
  }
}

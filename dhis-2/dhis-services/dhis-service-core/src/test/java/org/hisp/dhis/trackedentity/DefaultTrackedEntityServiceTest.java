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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTrackedEntityServiceTest {

  @Mock private TrackedEntityStore trackedEntityStore;

  @Mock private TrackedEntityAttributeValueService attributeValueService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CurrentUserService currentUserService;

  @Mock private AclService aclService;

  @Mock private TrackerOwnershipManager trackerOwnershipAccessManager;

  @Mock private TrackedEntityAuditService trackedEntityAuditService;

  @Mock private TrackedEntityAttributeValueAuditService attributeValueAuditService;

  private TrackedEntityQueryParams params;

  private DefaultTrackedEntityService teiService;

  @BeforeEach
  void setup() {
    teiService =
        new DefaultTrackedEntityService(
            trackedEntityStore,
            attributeValueService,
            attributeService,
            trackedEntityTypeService,
            organisationUnitService,
            currentUserService,
            aclService,
            trackerOwnershipAccessManager,
            trackedEntityAuditService,
            attributeValueAuditService);

    User user = new User();
    user.setOrganisationUnits(Set.of(new OrganisationUnit("A")));
    user.setTeiSearchOrganisationUnits(Set.of(new OrganisationUnit("B")));
    when(currentUserService.getCurrentUser()).thenReturn(user);

    params = new TrackedEntityQueryParams();
    params.setOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE);
    params.setProgram(new Program("Test program"));
    params.getProgram().setMaxTeiCountToReturn(10);
    params.setTrackedEntityUids(Set.of("1"));
  }

  @Test
  void exceptionThrownWhenTeiLimitReached() {
    when(trackedEntityStore.getTrackedEntityCountForGridWithMaxTeiLimit(
            any(TrackedEntityQueryParams.class)))
        .thenReturn(20);

    IllegalQueryException expectedException =
        assertThrows(
            IllegalQueryException.class,
            () -> teiService.validateSearchScope(params, true),
            "test message");

    assertEquals("maxteicountreached", expectedException.getMessage());
  }

  @Test
  void noExceptionThrownWhenTeiLimitNotReached() {
    when(trackedEntityStore.getTrackedEntityCountForGridWithMaxTeiLimit(
            any(TrackedEntityQueryParams.class)))
        .thenReturn(0);

    teiService.validateSearchScope(params, true);
  }
}

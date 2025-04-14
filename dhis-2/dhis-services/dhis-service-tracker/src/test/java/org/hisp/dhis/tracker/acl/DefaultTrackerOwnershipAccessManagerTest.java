/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createTrackedEntity;
import static org.hisp.dhis.test.TestBase.createTrackedEntityType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTempOwnerService;
import org.hisp.dhis.program.ProgramTempOwnershipAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAuditService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DefaultTrackerOwnershipAccessManagerTest {

  @Mock private UserService userService;

  @Mock private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Mock private ProgramTempOwnershipAuditService programTempOwnershipAuditService;

  @Mock private ProgramTempOwnerService programTempOwnerService;

  @Mock private ProgramOwnershipHistoryService programOwnershipHistoryService;

  @Mock private ProgramService programService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private CacheProvider cacheProvider;

  @Mock private Cache<Object> ownerCache;

  @Mock private Cache<Object> tempOwnerCache;

  @Mock private AclService aclService;

  @Mock private TrackerOwnershipAccessManager trackerOwnershipAccessManager;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private TrackerProgramService trackerProgramService;

  @Mock private OrganisationUnitService organisationUnitService;

  @InjectMocks private TrackerOwnershipTransferManager trackerOwnershipTransferManager;

  private Program program;
  private UserDetails userDetails;
  private String reason;
  private OrganisationUnit orgUnit;
  private TrackedEntityType trackedEntityType;
  private MockedStatic<SecurityContextHolder> mockedStatic;

  @BeforeEach
  void setUp() throws BadRequestException {
    when(cacheProvider.createProgramOwnerCache()).thenReturn(ownerCache);
    when(cacheProvider.createProgramTempOwnerCache()).thenReturn(tempOwnerCache);

    trackerOwnershipTransferManager =
        new TrackerOwnershipTransferManager(
            userService,
            trackedEntityProgramOwnerService,
            cacheProvider,
            programTempOwnershipAuditService,
            programTempOwnerService,
            programOwnershipHistoryService,
            programService,
            manager,
            aclService,
            trackerOwnershipAccessManager,
            trackedEntityService,
            trackerProgramService,
            organisationUnitService);

    orgUnit = createOrganisationUnit("org unit");
    orgUnit.setPath(orgUnit.getUid());
    program = createProgram('A');
    program.setAccessLevel(AccessLevel.PROTECTED);
    trackedEntityType = createTrackedEntityType('A');
    program.setTrackedEntityType(trackedEntityType);
    when(trackerProgramService.getTrackerProgram(UID.of(program))).thenReturn(program);

    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    userDetails = UserDetails.fromUser(user);
    reason = "breaking the glass";

    mockedStatic = mockStatic(SecurityContextHolder.class);
    SecurityContext context = mock(SecurityContext.class);
    Authentication auth = mock(Authentication.class);

    mockedStatic.when(SecurityContextHolder::getContext).thenReturn(context);
    when(context.getAuthentication()).thenReturn(auth);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(userDetails);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void shouldLogProgramOwnershipChangeWhenTrackedEntityTypeAuditEnabled()
      throws ForbiddenException, BadRequestException {
    TrackedEntity trackedEntity = createTrackedEntityWithAuditLog(true);
    when(manager.get(TrackedEntity.class, trackedEntity.getUid())).thenReturn(trackedEntity);
    when(trackerOwnershipAccessManager.isOwnerInUserSearchScope(
            userDetails, trackedEntity, program))
        .thenReturn(true);
    when(aclService.canDataRead(userDetails, trackedEntity.getTrackedEntityType()))
        .thenReturn(true);

    trackerOwnershipTransferManager.grantTemporaryOwnership(
        UID.of(trackedEntity), UID.of(program), reason);

    verify(programTempOwnershipAuditService, times(1))
        .addProgramTempOwnershipAudit(any(ProgramTempOwnershipAudit.class));
  }

  @Test
  void shouldNotLogProgramOwnershipChangeWhenTrackedEntityTypeAuditDisabled()
      throws ForbiddenException, BadRequestException {
    TrackedEntity trackedEntity = createTrackedEntityWithAuditLog(false);
    when(manager.get(TrackedEntity.class, trackedEntity.getUid())).thenReturn(trackedEntity);
    when(trackerOwnershipAccessManager.isOwnerInUserSearchScope(
            userDetails, trackedEntity, program))
        .thenReturn(true);
    when(aclService.canDataRead(userDetails, trackedEntity.getTrackedEntityType()))
        .thenReturn(true);

    trackerOwnershipTransferManager.grantTemporaryOwnership(
        UID.of(trackedEntity), UID.of(program), reason);

    verify(programTempOwnershipAuditService, never())
        .addProgramTempOwnershipAudit(any(ProgramTempOwnershipAudit.class));
  }

  private TrackedEntity createTrackedEntityWithAuditLog(boolean isAllowAuditLog) {
    trackedEntityType.setAllowAuditLog(isAllowAuditLog);
    TrackedEntity trackedEntity = createTrackedEntity('A', orgUnit, trackedEntityType);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }
}

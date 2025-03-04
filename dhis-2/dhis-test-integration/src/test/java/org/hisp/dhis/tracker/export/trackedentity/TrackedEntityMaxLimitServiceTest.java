/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TrackedEntityMaxLimitServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private TrackedEntityService trackedEntityService;
  @Autowired private SystemSettingsService systemSettingsService;

  private User importUser;
  private TrackedEntityType trackedEntityType;
  private Program program;
  private OrganisationUnit organisationUnit;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    program = manager.get(Program.class, "BFcipDERJnf");
    organisationUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
  }

  @BeforeEach
  void resetLimits() throws NotFoundException, BadRequestException {
    injectSecurityContextUser(importUser);
    updateTrackedEntityTypeMaxLimit(trackedEntityType.getUid(), 0);
    updateProgramMaxLimit(program.getUid(), 0);
    updateSystemSettingLimit("100");
  }

  @Test
  void shouldReturnEntitiesWhenGlobalSearchAndProgramMaxLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    updateProgramMaxLimit(program.getUid(), 5);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createProgramOperationParams();

    assertNotEmpty(trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void shouldFailWhenGlobalSearchAndProgramMaxLimitReached() {
    updateProgramMaxLimit(program.getUid(), 1);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createProgramOperationParams();

    Exception exception =
        Assertions.assertThrows(
            Exception.class, () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenGlobalSearchAndTETLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    updateTrackedEntityTypeMaxLimit(trackedEntityType.getUid(), 5);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    assertNotEmpty(trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void shouldFailWhenGlobalSearchAndTETMaxLimitReached() {
    updateTrackedEntityTypeMaxLimit(trackedEntityType.getUid(), 1);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    IllegalQueryException exception =
        Assertions.assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenSystemSettingLimitNotReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit("10");
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    assertNotEmpty(trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void shouldLimitResultsWhenSystemSettingLimitReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit("1");
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void
      shouldLimitResultsWhenGlobalSearchAndSystemLimitSmallerThanProgramLimitAndProgramLimitNotReached()
          throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit("1");
    updateProgramMaxLimit(program.getUid(), 2);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createProgramOperationParams();

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void shouldLimitResultsWhenGlobalSearchAndSystemLimitSmallerThanTETLimitAndTETLimitNotReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit("1");
    // TODO When this works as expected, change the limit to 2, so it makes more sense with the test
    // above
    updateTrackedEntityTypeMaxLimit(trackedEntityType.getUid(), 4);
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  void shouldReturnPaginatedResultsWhenPageSizeSmallerThanSystemSettingLimit()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit("10");
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 1, false);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.getTrackedEntities(operationParams, pageParams).getItems());
  }

  @Test
  void shouldFailWhenPageSizeIsBiggerThanSystemSettingLimit()
      throws NotFoundException, BadRequestException {
    updateSystemSettingLimit("1");
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () ->
                trackedEntityService.getTrackedEntities(
                    operationParams, PageParams.of(1, 10, false)));
    assertEquals("Page size can't be bigger than system setting limit", exception.getMessage());
  }

  private void updateTrackedEntityTypeMaxLimit(String tetId, int limit) {
    TrackedEntityType tet = manager.get(TrackedEntityType.class, tetId);
    assertNotNull(tet);
    tet.setMaxTeiCountToReturn(limit);
    manager.save(tet, false);
  }

  private void updateProgramMaxLimit(String programId, int limit) {
    Program program = manager.get(Program.class, programId);
    assertNotNull(program);
    program.setMaxTeiCountToReturn(limit);
    manager.save(program, false);
  }

  private void updateSystemSettingLimit(String value)
      throws NotFoundException, BadRequestException {
    systemSettingsService.putAll(Map.of("KeyTrackedEntityMaxLimit", value));
    systemSettingsService.clearCurrentSettings();
  }

  private int getCurrentSystemSettingLimit() {
    return systemSettingsService.getCurrentSettings().getTrackedEntityMaxLimit();
  }

  private TrackedEntityOperationParams createTrackedEntityTypeOperationParams() {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .trackedEntityType(trackedEntityType)
        .filterBy(UID.of("numericAttr"))
        .build();
  }

  private TrackedEntityOperationParams createProgramOperationParams() {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .program(program)
        .filterBy(UID.of("dIVt4l5vIOa"))
        .build();
  }

  // TODO Does pagination affect any of these limits?
}

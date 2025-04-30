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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertLessOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityMaxLimitServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private TrackedEntityService trackedEntityService;
  @Autowired private SystemSettingsService systemSettingsService;

  private User importUser;
  private User regularUser;
  private TrackedEntityType trackedEntityType;
  private Program program;
  private OrganisationUnit organisationUnit;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    regularUser = userService.getUser("FIgVWzUCkpw");
    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    program = manager.get(Program.class, "BFcipDERJnf");
    organisationUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
  }

  @AfterEach
  void resetLimits() throws NotFoundException, BadRequestException {
    injectSecurityContextUser(importUser);
    updateTrackedEntityTypeMaxLimit(0);
    updateProgramMaxLimit(0);
    updateSystemSettingLimit(50000);
  }

  @Test
  void shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndProgramMaxLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Program program = updateProgramMaxLimit(5);
    TrackedEntityOperationParams operationParams = createProgramOperationParams();
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        program.getMaxTeiCountToReturn(),
        trackedEntityService.findTrackedEntities(operationParams).size());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndProgramMaxLimitReached() {
    updateProgramMaxLimit(1);
    TrackedEntityOperationParams operationParams = createProgramOperationParams();
    injectSecurityContextUser(regularUser);

    Exception exception =
        assertThrows(
            Exception.class, () -> trackedEntityService.findTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndTETLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityType trackedEntityType = updateTrackedEntityTypeMaxLimit(5);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        trackedEntityType.getMaxTeiCountToReturn(),
        trackedEntityService.findTrackedEntities(operationParams).size());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndTETMaxLimitReached() {
    updateTrackedEntityTypeMaxLimit(1);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndTETMaxLimitReachedAndPageSizeSmallerThanLimit()
      throws BadRequestException {
    updateTrackedEntityTypeMaxLimit(2);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 1, false);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams, pageParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void
      shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndTETLimitNotReachedAndPageSizeSmallerThanLimit()
          throws ForbiddenException, BadRequestException, NotFoundException {
    updateTrackedEntityTypeMaxLimit(10);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.findTrackedEntities(operationParams, pageParams).getItems());
  }

  @Test
  void shouldNotApplyLimitsWhenSearchOutsideOfCaptureScopeIfOnlyTrackedEntitiesSpecified()
      throws ForbiddenException, BadRequestException, NotFoundException {
    updateProgramMaxLimit(1);
    updateTrackedEntityTypeMaxLimit(1);
    Set<UID> trackedEntities = Set.of(UID.of("dUE514NMOlo"), UID.of("QS6w44flWAf"));
    TrackedEntityOperationParams operationParams =
        createTrackedEntitiesOperationParams(trackedEntities);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        trackedEntities.size(), trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void shouldFailWhenSearchOutsideOfCaptureScopeIfProgramAndTrackedEntitiesSpecified() {
    updateProgramMaxLimit(1);
    Set<UID> trackedEntities = Set.of(UID.of("dUE514NMOlo"), UID.of("QS6w44flWAf"));
    TrackedEntityOperationParams operationParams =
        createProgramAndTrackedEntitiesOperationParams(trackedEntities);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldFailWhenSearchOutsideOfCaptureScopeIfTETAndTrackedEntitiesSpecified() {
    updateTrackedEntityTypeMaxLimit(1);
    Set<UID> trackedEntities = Set.of(UID.of("dUE514NMOlo"), UID.of("QS6w44flWAf"));
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeAndTrackedEntitiesOperationParams(trackedEntities);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenSystemSettingLimitNotReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(10);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        getCurrentSystemSettingLimit(),
        trackedEntityService.findTrackedEntities(operationParams).size());
  }

  @Test
  void shouldLimitResultsWhenSystemSettingLimitReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void
      shouldLimitResultsWhenSearchOutsideCaptureScopeAndSystemLimitSmallerThanProgramLimitAndProgramLimitNotReached()
          throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    updateProgramMaxLimit(5);
    TrackedEntityOperationParams operationParams = createProgramOperationParams();
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void
      shouldLimitResultsWhenSearchOutsideCaptureScopeAndSystemLimitSmallerThanTETLimitAndTETLimitNotReached()
          throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    updateTrackedEntityTypeMaxLimit(5);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(), trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void shouldReturnPaginatedResultsWhenPageSizeSmallerThanSystemSettingLimit()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(10);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.findTrackedEntities(operationParams, pageParams).getItems());
  }

  @Test
  void shouldReturnPaginatedResultsWhenPageSizeSpecifiedAndSystemSettingIsZero()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(0);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.findTrackedEntities(operationParams, pageParams).getItems());
  }

  @Test
  void shouldFailWhenPageSizeIsBiggerThanSystemSettingLimit()
      throws NotFoundException, BadRequestException {
    updateSystemSettingLimit(1);
    TrackedEntityOperationParams operationParams = createTrackedEntityTypeOperationParams();
    PageParams pageParams = PageParams.of(1, 10, false);
    injectSecurityContextUser(regularUser);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams, pageParams));
    assertEquals(
        String.format(
            "Invalid page size: %d. It must not exceed the system limit of KeyTrackedEntityMaxLimit %d.",
            pageParams.getPageSize(), getCurrentSystemSettingLimit()),
        exception.getMessage());
  }

  private TrackedEntityType updateTrackedEntityTypeMaxLimit(int limit) {
    TrackedEntityType tet = manager.get(TrackedEntityType.class, trackedEntityType.getId());
    assertNotNull(tet);
    tet.setMaxTeiCountToReturn(limit);
    manager.update(tet);
    return tet;
  }

  private Program updateProgramMaxLimit(int limit) {
    Program p = manager.get(Program.class, program.getId());
    assertNotNull(p);
    p.setMaxTeiCountToReturn(limit);
    manager.update(p);
    return p;
  }

  private void updateSystemSettingLimit(int value) throws NotFoundException, BadRequestException {
    systemSettingsService.putAll(Map.of("KeyTrackedEntityMaxLimit", String.valueOf(value)));
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
        .filterBy(UID.of("integerAttr"))
        .build();
  }

  private TrackedEntityOperationParams createProgramOperationParams() {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .program(program)
        .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntitiesOperationParams(
      Set<UID> trackedEntities) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .trackedEntities(trackedEntities)
        .build();
  }

  private TrackedEntityOperationParams createProgramAndTrackedEntitiesOperationParams(
      Set<UID> trackedEntities) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .program(program)
        .trackedEntities(trackedEntities)
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntityTypeAndTrackedEntitiesOperationParams(
      Set<UID> trackedEntities) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(organisationUnit)
        .trackedEntityType(trackedEntityType)
        .trackedEntities(trackedEntities)
        .build();
  }
}

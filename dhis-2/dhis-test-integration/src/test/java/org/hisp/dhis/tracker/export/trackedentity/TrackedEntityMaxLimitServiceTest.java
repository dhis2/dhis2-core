/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.hisp.dhis.setting.SettingKey.TRACKED_ENTITY_MAX_LIMIT;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertHasSize;
import static org.hisp.dhis.utils.Assertions.assertLessOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntityMaxLimitServiceTest extends TrackerTest {
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private TrackedEntityService trackedEntityService;
  @Autowired private SystemSettingManager systemSettingManager;
  @Autowired private TrackerImportService trackerImportService;
  @Autowired protected UserService _userService;

  private User importUser;
  private User regularUser;
  private TrackedEntityType trackedEntityType;
  private Program program;
  private OrganisationUnit organisationUnit;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");

    regularUser = userService.getUser("FIgVWzUCkpw");
    importUser = userService.getUser("M5zQapPyTZI");
    injectSecurityContextUser(importUser);
    TrackerImportParams params = TrackerImportParams.builder().userId(importUser.getUid()).build();

    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
    trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    program = manager.get(Program.class, "BFcipDERJnf");
    organisationUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
  }

  @AfterEach
  void resetLimits() {
    injectSecurityContextUser(importUser);
    updateTrackedEntityTypeMaxLimit(0);
    updateProgramMaxLimit(0);
    updateSystemSettingLimit(50000);
  }

  @Test
  void shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndProgramMaxLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Program program = updateProgramMaxLimit(5);
    TrackedEntityOperationParams operationParams = createProgramOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        program.getMaxTeiCountToReturn(),
        trackedEntityService.getTrackedEntities(operationParams).size());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndProgramMaxLimitReached() {
    updateProgramMaxLimit(1);
    TrackedEntityOperationParams operationParams = createProgramOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    Exception exception =
        assertThrows(
            Exception.class, () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndTETLimitNotReached()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityType trackedEntityType = updateTrackedEntityTypeMaxLimit(5);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        trackedEntityType.getMaxTeiCountToReturn(),
        trackedEntityService.getTrackedEntities(operationParams).size());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndTETMaxLimitReached() {
    updateTrackedEntityTypeMaxLimit(1);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldFailWhenSearchOutsideCaptureScopeAndTETMaxLimitReachedAndPageSizeSmallerThanLimit() {
    updateTrackedEntityTypeMaxLimit(2);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    PageParams pageParams = new PageParams(1, 1, false);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams, pageParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void
      shouldReturnEntitiesWhenSearchOutsideCaptureScopeAndTETLimitNotReachedAndPageSizeSmallerThanLimit()
          throws ForbiddenException, BadRequestException, NotFoundException {
    updateTrackedEntityTypeMaxLimit(10);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    PageParams pageParams = new PageParams(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.getTrackedEntities(operationParams, pageParams).getItems(),
        "Tracked entity size not expected when search outside capture scope and no limit reached");
  }

  @Test
  void shouldNotApplyLimitsWhenSearchOutsideOfCaptureScopeIfOnlyTrackedEntitiesSpecified()
      throws ForbiddenException, BadRequestException, NotFoundException {
    updateProgramMaxLimit(1);
    updateTrackedEntityTypeMaxLimit(1);
    Set<String> trackedEntities = Set.of("dUE514NMOlo", "QS6w44flWAf");
    TrackedEntityOperationParams operationParams =
        createTrackedEntitiesOperationParams(trackedEntities, regularUser);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        trackedEntities.size(),
        trackedEntityService.getTrackedEntities(operationParams),
        "Tracked entity size not expected when search outside capture scope and tracked entities specified");
  }

  @Test
  void shouldFailWhenSearchOutsideOfCaptureScopeIfProgramAndTrackedEntitiesSpecified() {
    updateProgramMaxLimit(1);
    Set<String> trackedEntities = Set.of("dUE514NMOlo", "QS6w44flWAf");
    TrackedEntityOperationParams operationParams =
        createProgramAndTrackedEntitiesOperationParams(trackedEntities, regularUser);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldFailWhenSearchOutsideOfCaptureScopeIfTETAndTrackedEntitiesSpecified() {
    updateTrackedEntityTypeMaxLimit(1);
    Set<String> trackedEntities = Set.of("dUE514NMOlo", "QS6w44flWAf");
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeAndTrackedEntitiesOperationParams(trackedEntities, regularUser);
    injectSecurityContextUser(regularUser);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));
    assertEquals("maxteicountreached", exception.getMessage());
  }

  @Test
  void shouldReturnEntitiesWhenSystemSettingLimitNotReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(10);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertLessOrEqual(
        getCurrentSystemSettingLimit(),
        trackedEntityService.getTrackedEntities(operationParams).size());
  }

  @Test
  void shouldLimitResultsWhenSystemSettingLimitReached()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(),
        trackedEntityService.getTrackedEntities(operationParams),
        "Tracked entity size not expected when system setting limit reached.");
  }

  @Test
  void
      shouldLimitResultsWhenSearchOutsideCaptureScopeAndSystemLimitSmallerThanProgramLimitAndProgramLimitNotReached()
          throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    updateProgramMaxLimit(5);
    TrackedEntityOperationParams operationParams = createProgramOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(),
        trackedEntityService.getTrackedEntities(operationParams),
        "Tracked entity size not expected when search outside capture scope and program limit not reached.");
  }

  @Test
  void
      shouldLimitResultsWhenSearchOutsideCaptureScopeAndSystemLimitSmallerThanTETLimitAndTETLimitNotReached()
          throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(1);
    updateTrackedEntityTypeMaxLimit(5);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        getCurrentSystemSettingLimit(),
        trackedEntityService.getTrackedEntities(operationParams),
        "Tracked entity size not expected when search outside capture scope and TET not reached.");
  }

  @Test
  void shouldReturnPaginatedResultsWhenPageSizeSmallerThanSystemSettingLimit()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(10);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    PageParams pageParams = new PageParams(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.getTrackedEntities(operationParams, pageParams).getItems(),
        "Tracked entity size not expected when page size smaller than system setting.");
  }

  @Test
  void shouldReturnPaginatedResultsWhenPageSizeSpecifiedAndSystemSettingIsZero()
      throws NotFoundException, BadRequestException, ForbiddenException {
    updateSystemSettingLimit(0);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    PageParams pageParams = new PageParams(1, 1, false);
    injectSecurityContextUser(regularUser);

    assertHasSize(
        pageParams.getPageSize(),
        trackedEntityService.getTrackedEntities(operationParams, pageParams).getItems(),
        "Tracked entity size not expected when page size specified and system setting is zero.");
  }

  @Test
  void shouldFailWhenPageSizeIsBiggerThanSystemSettingLimit() {
    updateSystemSettingLimit(1);
    TrackedEntityOperationParams operationParams =
        createTrackedEntityTypeOperationParams(regularUser);
    PageParams pageParams = new PageParams(1, 10, false);
    injectSecurityContextUser(regularUser);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams, pageParams));
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

  private void updateSystemSettingLimit(int value) {
    systemSettingManager.saveSystemSetting(TRACKED_ENTITY_MAX_LIMIT, value);
    systemSettingManager.invalidateCache();
  }

  private int getCurrentSystemSettingLimit() {
    return systemSettingManager.getIntegerSetting(TRACKED_ENTITY_MAX_LIMIT);
  }

  private TrackedEntityOperationParams createTrackedEntityTypeOperationParams(User user) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(Set.of(organisationUnit.getUid()))
        .trackedEntityTypeUid(trackedEntityType.getUid())
        .filters(Map.of("dIVt4l5vIOa", List.of()))
        .user(user)
        .build();
  }

  private TrackedEntityOperationParams createProgramOperationParams(User user) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(Set.of(organisationUnit.getUid()))
        .programUid(program.getUid())
        .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
        .user(user)
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntitiesOperationParams(
      Set<String> trackedEntities, User user) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(Set.of(organisationUnit.getUid()))
        .trackedEntityUids(trackedEntities)
        .user(user)
        .build();
  }

  private TrackedEntityOperationParams createProgramAndTrackedEntitiesOperationParams(
      Set<String> trackedEntities, User user) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(Set.of(organisationUnit.getUid()))
        .programUid(program.getUid())
        .trackedEntityUids(trackedEntities)
        .user(user)
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntityTypeAndTrackedEntitiesOperationParams(
      Set<String> trackedEntities, User user) {
    return TrackedEntityOperationParams.builder()
        .orgUnitMode(SELECTED)
        .organisationUnits(Set.of(organisationUnit.getUid()))
        .trackedEntityTypeUid(trackedEntityType.getUid())
        .trackedEntityUids(trackedEntities)
        .user(user)
        .build();
  }
}

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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.deduplication.DeduplicationStatus;
import org.hisp.dhis.tracker.deduplication.MergeObject;
import org.hisp.dhis.tracker.deduplication.MergeStrategy;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicate;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author luca@dhis2.org
 */
@Transactional
class DeduplicationControllerTest extends H2ControllerIntegrationTestBase {
  private static final String ENDPOINT = "/" + "potentialDuplicates/";

  @Autowired private IdentifiableObjectManager dbmsManager;

  @Autowired private ObjectMapper objectMapper;

  private OrganisationUnit orgUnit;
  private TrackedEntityType trackedEntityType;
  private TrackedEntity origin;
  private TrackedEntity duplicate;
  private PotentialDuplicate potentialDuplicate1;
  private PotentialDuplicate potentialDuplicate2;
  private User user;

  @BeforeEach
  void setUp() {
    orgUnit = createOrganisationUnit(CodeGenerator.generateUid());
    dbmsManager.save(orgUnit);
    getAdminUser().getOrganisationUnits().add(orgUnit);

    trackedEntityType = createAndSaveTrackedEntityType('A');
    origin = createAndSaveTrackedEntity(orgUnit, trackedEntityType);
    duplicate = createAndSaveTrackedEntity(orgUnit, trackedEntityType);
    TrackedEntity duplicate2 = createAndSaveTrackedEntity(orgUnit, trackedEntityType);

    potentialDuplicate1 = new PotentialDuplicate(UID.of(origin), UID.of(duplicate));
    save(potentialDuplicate1);
    potentialDuplicate2 = new PotentialDuplicate(UID.of(origin), UID.of(duplicate2));
    save(potentialDuplicate2);

    user = createAndAddUser("regular-user");
  }

  @Test
  void shouldPostPotentialDuplicateWhenTrackedEntitiesExist() throws Exception {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    dbmsManager.save(trackedEntity);
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(trackedEntity), UID.of(duplicate));

    POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate)).content(HttpStatus.OK);
  }

  @Test
  void shouldContainDefaultFieldsWhenGetPotentialDuplicates() {
    JsonPage page = GET(ENDPOINT).content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);
    assertContainsOnly(
        List.of(potentialDuplicate1.getUid(), potentialDuplicate2.getUid()),
        list.toList(JsonPotentialDuplicate::getUid));

    JsonPotentialDuplicate jsonPotentialDuplicate = list.get(0);
    assertEquals(potentialDuplicate1.getUid(), jsonPotentialDuplicate.getUid());
    assertEquals(potentialDuplicate1.getStatus().name(), jsonPotentialDuplicate.getStatus());
    assertEquals(potentialDuplicate1.getOriginal(), jsonPotentialDuplicate.getOriginal());
    assertEquals(potentialDuplicate1.getDuplicate(), jsonPotentialDuplicate.getDuplicate());
    assertNotNull(potentialDuplicate1.getCreated());
    assertNotNull(potentialDuplicate1.getLastUpdated());

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
    JsonPage page =
        GET("/potentialDuplicates?page=2&pageSize=1").content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);

    assertEquals(
        1,
        list.size(),
        () ->
            String.format("mismatch in number of expected potential duplicates(s), got %s", list));
    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetPaginatedItemsWithPagingSetToTrue() {
    JsonPage page =
        GET("/potentialDuplicates?paging=true").content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);
    assertContainsOnly(
        List.of(potentialDuplicate1.getUid(), potentialDuplicate2.getUid()),
        list.toList(JsonPotentialDuplicate::getUid));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(page.getPager(), "total");
    assertHasNoMember(page.getPager(), "pageCount");
  }

  @Test
  void shouldGetNonPaginatedItemsWithPagingSetToFalse() {
    JsonPage page =
        GET("/potentialDuplicates?paging=false").content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);
    assertContainsOnly(
        List.of(potentialDuplicate1.getUid(), potentialDuplicate2.getUid()),
        list.toList(JsonPotentialDuplicate::getUid));
    assertHasNoMember(page, "pager");
  }

  @Test
  void shouldThrowBadRequestExceptionWhenMissingDuplicateTeInPayload() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(UID.of(origin), null);

    POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate))
        .content(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTeInPayloadDoesNotExist() throws Exception {
    String nonExistentUID = UID.generate();
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(origin), UID.of(nonExistentUID));

    POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate))
        .content(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenMissingOriginTeInPayload() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(null, UID.of(duplicate));

    POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate))
        .content(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldThrowForbiddenExceptionWhenPostAndUserHasNoTeAccess() throws Exception {
    TrackedEntity trackedEntity = createAndSaveTrackedEntity(orgUnit, trackedEntityType);
    injectSecurityContextUser(user);

    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(trackedEntity), UID.of(duplicate));

    POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate))
        .content(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPutPotentialDuplicateAlreadyMerged() {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(origin), UID.of(duplicate));
    potentialDuplicate.setStatus(DeduplicationStatus.MERGED);
    save(potentialDuplicate);

    PUT(ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.INVALID.name())
        .content(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPutPotentialDuplicateToMergedStatus() {
    PotentialDuplicate potentialDuplicate = potentialDuplicate(origin.getUid(), duplicate.getUid());

    PUT(ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.MERGED.name())
        .content(HttpStatus.BAD_REQUEST);
  }

  @Test
  void shouldUpdatePotentialDuplicateWhenPotentialDuplicateExistsAndCorrectStatus() {
    PotentialDuplicate potentialDuplicate = potentialDuplicate(origin.getUid(), duplicate.getUid());

    PUT(ENDPOINT + potentialDuplicate.getUid() + "?status=" + DeduplicationStatus.INVALID.name())
        .content(HttpStatus.OK);
  }

  @Test
  void shouldGetPotentialDuplicateByIdWhenPotentialDuplicateExists() {
    PotentialDuplicate potentialDuplicate = potentialDuplicate(origin.getUid(), duplicate.getUid());

    GET(ENDPOINT + potentialDuplicate.getUid()).content(HttpStatus.OK);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenPotentialDuplicateDoesNotExist() {
    GET(ENDPOINT + UID.generate()).content(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldAutoMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() throws Exception {
    PotentialDuplicate potentialDuplicate = potentialDuplicate(origin.getUid(), duplicate.getUid());
    MergeObject mergeObject = MergeObject.builder().build();

    POST(
            ENDPOINT + "/" + potentialDuplicate.getUid() + "/merge",
            objectMapper.writeValueAsString(mergeObject))
        .content(HttpStatus.OK);
  }

  @Test
  void shouldManualMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() throws Exception {
    PotentialDuplicate potentialDuplicate = potentialDuplicate(origin.getUid(), duplicate.getUid());
    MergeObject mergeObject = MergeObject.builder().build();

    POST(
            ENDPOINT
                + "/"
                + potentialDuplicate.getUid()
                + "/merge?mergeStrategy="
                + MergeStrategy.MANUAL.name(),
            objectMapper.writeValueAsString(mergeObject))
        .content(HttpStatus.OK);
  }

  @Test
  void shouldThrowForbiddenExceptionWhenAutoMergingAndUserHasNoAccessToTrackedEntityType()
      throws Exception {
    TrackedEntityType trackedEntityType = createAndSaveTrackedEntityType('B');
    TrackedEntity trackedEntity = createAndSaveTrackedEntity(orgUnit, trackedEntityType);
    injectSecurityContextUser(user);
    PotentialDuplicate potentialDuplicate =
        potentialDuplicate(origin.getUid(), trackedEntity.getUid());
    MergeObject mergeObject = MergeObject.builder().build();

    POST(
            ENDPOINT + "/" + potentialDuplicate.getUid() + "/merge",
            objectMapper.writeValueAsString(mergeObject))
        .content(HttpStatus.FORBIDDEN);
  }

  @Test
  void shouldThrowConflictExceptionWhenAutoMergingAndTrackedEntityTypesAreDifferent()
      throws Exception {
    TrackedEntityType trackedEntityType = createAndSaveTrackedEntityType('B');
    TrackedEntity trackedEntity = createAndSaveTrackedEntity(orgUnit, trackedEntityType);
    PotentialDuplicate potentialDuplicate =
        potentialDuplicate(origin.getUid(), trackedEntity.getUid());
    MergeObject mergeObject = MergeObject.builder().build();

    POST(
            ENDPOINT + "/" + potentialDuplicate.getUid() + "/merge",
            objectMapper.writeValueAsString(mergeObject))
        .content(HttpStatus.CONFLICT);
  }

  private PotentialDuplicate potentialDuplicate(String original, String duplicate) {
    return save(new PotentialDuplicate(UID.of(original), UID.of(duplicate)));
  }

  private PotentialDuplicate save(PotentialDuplicate potentialDuplicate) {
    potentialDuplicate.setLastUpdatedByUserName("user");
    potentialDuplicate.setCreatedByUserName("user");
    dbmsManager.save(potentialDuplicate);
    dbmsManager.flush();
    return potentialDuplicate;
  }

  private TrackedEntity createAndSaveTrackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    dbmsManager.save(trackedEntity);

    return trackedEntity;
  }

  private TrackedEntityType createAndSaveTrackedEntityType(char uniqueChar) {
    TrackedEntityType trackedEntityType = createTrackedEntityType(uniqueChar);
    dbmsManager.save(trackedEntityType);

    return trackedEntityType;
  }
}

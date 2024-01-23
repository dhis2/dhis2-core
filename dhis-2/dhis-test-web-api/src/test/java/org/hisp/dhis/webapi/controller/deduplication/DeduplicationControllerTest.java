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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author luca@dhis2.org
 */
class DeduplicationControllerTest extends DhisControllerConvenienceTest {
  private static final String ENDPOINT = "/" + "potentialDuplicates/";

  @Autowired private IdentifiableObjectManager dbmsManager;

  @Autowired private ObjectMapper objectMapper;

  private OrganisationUnit orgUnit;
  private TrackedEntity origin;
  private TrackedEntity duplicate1;
  private PotentialDuplicate potentialDuplicate1;
  private PotentialDuplicate potentialDuplicate2;

  @BeforeEach
  public void setUp() {
    orgUnit = createOrganisationUnit(CodeGenerator.generateUid());
    dbmsManager.save(orgUnit);

    origin = createTrackedEntity(orgUnit);
    duplicate1 = createTrackedEntity(orgUnit);
    TrackedEntity duplicate2 = createTrackedEntity(orgUnit);

    dbmsManager.save(origin);
    dbmsManager.save(duplicate1);
    dbmsManager.save(duplicate2);

    potentialDuplicate1 = new PotentialDuplicate(origin.getUid(), duplicate1.getUid());
    save(potentialDuplicate1);
    potentialDuplicate2 = new PotentialDuplicate(origin.getUid(), duplicate2.getUid());
    save(potentialDuplicate2);
  }

  @Test
  void shouldPostPotentialDuplicateWhenTrackedEntitiesExist() throws Exception {
    TrackedEntity te = createTrackedEntity(orgUnit);
    dbmsManager.save(te);
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(te.getUid(), duplicate1.getUid());

    assertStatus(
        HttpStatus.OK, POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate)));
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

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
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

    // assert deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(1, page.getPageSize());
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

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldGetNonPaginatedItemsWithSkipPaging() {
    JsonPage page =
        GET("/potentialDuplicates?skipPaging=true").content(HttpStatus.OK).asA(JsonPage.class);

    JsonList<JsonPotentialDuplicate> list =
        page.getList("potentialDuplicates", JsonPotentialDuplicate.class);
    assertContainsOnly(
        List.of(potentialDuplicate1.getUid(), potentialDuplicate2.getUid()),
        list.toList(JsonPotentialDuplicate::getUid));
    assertHasNoMember(page, "pager");

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
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

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldFailWhenSkipPagingAndPagingAreFalse() {
    String message =
        GET("/potentialDuplicates?paging=false&skipPaging=false")
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }

  @Test
  void shouldFailWhenSkipPagingAndPagingAreTrue() {
    String message =
        GET("/potentialDuplicates?paging=true&skipPaging=true")
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }

  @Test
  void shouldThrowPostPotentialDuplicateWhenMissingDuplicateTeiInPayload() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(origin.getUid(), null);
    assertStatus(
        HttpStatus.BAD_REQUEST,
        POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate)));
  }

  @Test
  void shouldThrowPostPotentialDuplicateWhenMissingOriginTeiInPayload() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(null, duplicate1.getUid());
    assertStatus(
        HttpStatus.BAD_REQUEST,
        POST(ENDPOINT, objectMapper.writeValueAsString(potentialDuplicate)));
  }

  @Test
  void shouldThrowBadRequestWhenPutPotentialDuplicateAlreadyMerged() {
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(origin.getUid(), duplicate1.getUid());
    potentialDuplicate.setStatus(DeduplicationStatus.MERGED);
    save(potentialDuplicate);

    assertStatus(
        HttpStatus.BAD_REQUEST,
        PUT(
            ENDPOINT
                + potentialDuplicate.getUid()
                + "?status="
                + DeduplicationStatus.INVALID.name()));
  }

  @Test
  void shouldThrowBadRequestWhenPutPotentialDuplicateToMergedStatus() {
    PotentialDuplicate potentialDuplicate =
        potentialDuplicate(origin.getUid(), duplicate1.getUid());
    assertStatus(
        HttpStatus.BAD_REQUEST,
        PUT(
            ENDPOINT
                + potentialDuplicate.getUid()
                + "?status="
                + DeduplicationStatus.MERGED.name()));
  }

  @Test
  void shouldUpdatePotentialDuplicateWhenPotentialDuplicateExistsAndCorrectStatus() {
    PotentialDuplicate potentialDuplicate =
        potentialDuplicate(origin.getUid(), duplicate1.getUid());
    assertStatus(
        HttpStatus.OK,
        PUT(
            ENDPOINT
                + potentialDuplicate.getUid()
                + "?status="
                + DeduplicationStatus.INVALID.name()));
  }

  @Test
  void shouldGetPotentialDuplicateByIdWhenPotentialDuplicateExists() {
    PotentialDuplicate potentialDuplicate =
        potentialDuplicate(origin.getUid(), duplicate1.getUid());
    assertStatus(HttpStatus.OK, GET(ENDPOINT + potentialDuplicate.getUid()));
  }

  @Test
  void shouldThrowNotFoundWhenPotentialDuplicateDoNotExists() {
    assertStatus(HttpStatus.NOT_FOUND, GET(ENDPOINT + "uid"));
  }

  private PotentialDuplicate potentialDuplicate(String original, String duplicate) {
    return save(new PotentialDuplicate(original, duplicate));
  }

  private PotentialDuplicate save(PotentialDuplicate potentialDuplicate) {
    potentialDuplicate.setLastUpdatedByUserName("user");
    potentialDuplicate.setCreatedByUserName("user");
    dbmsManager.save(potentialDuplicate);
    dbmsManager.flush();
    return potentialDuplicate;
  }
}

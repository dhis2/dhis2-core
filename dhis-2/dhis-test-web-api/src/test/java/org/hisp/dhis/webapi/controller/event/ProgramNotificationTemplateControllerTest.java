/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramNotificationTemplateControllerTest extends DhisControllerConvenienceTest {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private ProgramNotificationTemplateService templateTemplateService;
  private Program program;
  private ProgramNotificationTemplate programTemplate1;
  private ProgramNotificationTemplate programTemplate2;

  @BeforeEach
  void setUp() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    idObjectManager.save(ouA);

    programTemplate1 = new ProgramNotificationTemplate();
    programTemplate1.setName("template 1");
    programTemplate1.setMessageTemplate("message 1");
    templateTemplateService.save(programTemplate1);

    programTemplate2 = new ProgramNotificationTemplate();
    programTemplate2.setName("template 2");
    programTemplate2.setMessageTemplate("message 2");
    templateTemplateService.save(programTemplate2);

    program = createProgram('A', Set.of(), ouA);
    program.setNotificationTemplates(Set.of(programTemplate1, programTemplate2));
    idObjectManager.save(program);
  }

  @Test
  void shouldGetPaginatedItemsWithDefaults() {
    JsonPage page =
        GET("/programNotificationTemplates/filter?program={uid}", program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(programTemplate1.getName(), programTemplate2.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(1, page.getPageCount());
  }

  @Disabled(
      "  TODO(tracker): https://dhis2.atlassian.net/browse/DHIS2-16522 pagination is not implemented in the store")
  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
    JsonPage page =
        GET(
                "/programNotificationTemplates/filter?program={uid}&page=2&pageSize=1",
                program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertEquals(
        1,
        list.size(),
        () -> String.format("mismatch in number of expected notification(s), got %s", list));

    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(1, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(2, page.getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithPagingSetToTrue() {
    JsonPage page =
        GET("/programNotificationTemplates/filter?program={uid}&paging=true", program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(programTemplate1.getName(), programTemplate2.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(1, page.getPageCount());
  }

  @Test
  void shouldGetNonPaginatedItemsWithSkipPaging() {
    JsonPage page =
        GET("/programNotificationTemplates/filter?program={uid}&skipPaging=true", program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(programTemplate1.getName(), programTemplate2.getName()),
        list.toList(JsonIdentifiableObject::getName));
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
        GET("/programNotificationTemplates/filter?program={uid}&paging=false", program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(programTemplate1.getName(), programTemplate2.getName()),
        list.toList(JsonIdentifiableObject::getName));
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
        GET(
                "/programNotificationTemplates/filter?program={uid}&paging=false&skipPaging=false",
                program.getUid())
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }

  @Test
  void shouldFailWhenSkipPagingAndPagingAreTrue() {
    String message =
        GET(
                "/programNotificationTemplates/filter?program={uid}&paging=true&skipPaging=true",
                program.getUid())
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }
}

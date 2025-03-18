/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProgramNotificationTemplateControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private ProgramNotificationTemplateService templateTemplateService;
  private Program program;
  private ProgramNotificationTemplate programTemplate1;
  private ProgramNotificationTemplate programTemplate2;
  private ProgramNotificationTemplate programTemplate3;
  private ProgramNotificationTemplate programTemplate4;
  private ProgramNotificationTemplate programTemplate5;

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

    programTemplate3 = new ProgramNotificationTemplate();
    programTemplate3.setName("template 3");
    programTemplate3.setMessageTemplate("message 3");
    templateTemplateService.save(programTemplate3);

    programTemplate4 = new ProgramNotificationTemplate();
    programTemplate4.setName("template 4");
    programTemplate4.setMessageTemplate("message 4");
    templateTemplateService.save(programTemplate4);

    programTemplate5 = new ProgramNotificationTemplate();
    programTemplate5.setName("template 5");
    programTemplate5.setMessageTemplate("message 5");
    templateTemplateService.save(programTemplate5);

    program = createProgram('A', Set.of(), ouA);
    program.setNotificationTemplates(
        Set.of(programTemplate1, programTemplate2, programTemplate3, programTemplate4));
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
        List.of(
            programTemplate1.getName(),
            programTemplate2.getName(),
            programTemplate3.getName(),
            programTemplate4.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(4, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
    JsonPage page =
        GET(
                "/programNotificationTemplates/filter?program={uid}&page=2&pageSize=2",
                program.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationTemplates", JsonIdentifiableObject.class);
    assertEquals(
        2,
        list.size(),
        () -> String.format("mismatch in number of expected notification(s), got %s", list));

    assertEquals(2, page.getPager().getPage());
    assertEquals(2, page.getPager().getPageSize());
    assertEquals(4, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());
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
        List.of(
            programTemplate1.getName(),
            programTemplate2.getName(),
            programTemplate3.getName(),
            programTemplate4.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(4, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());
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
        List.of(
            programTemplate1.getName(),
            programTemplate2.getName(),
            programTemplate3.getName(),
            programTemplate4.getName()),
        list.toList(JsonIdentifiableObject::getName));
    assertHasNoMember(page, "pager");
  }

  @Test
  void shouldFailWhenProgramDoesNotExist() {
    String invalidProgram = CodeGenerator.generateUid();
    String message =
        GET("/programNotificationTemplates/filter?program={uid}", invalidProgram)
            .content(HttpStatus.CONFLICT)
            .getString("message")
            .string();

    assertStartsWith(
        "%s with UID %s does not exist.".formatted(Program.class.getSimpleName(), invalidProgram),
        message);
  }

  @Test
  void shouldFailWhenProgramStageDoesNotExist() {
    String invalidProgramStage = CodeGenerator.generateUid();
    String message =
        GET("/programNotificationTemplates/filter?programStage={uid}", invalidProgramStage)
            .content(HttpStatus.CONFLICT)
            .getString("message")
            .string();

    assertStartsWith(
        "%s with UID %s does not exist."
            .formatted(ProgramStage.class.getSimpleName(), invalidProgramStage),
        message);
  }
}

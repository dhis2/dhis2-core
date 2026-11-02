/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.lang.String.format;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests handling of data values of {@link ValueType#FILE_RESOURCE}. These need to deal with the
 * file resource associated with the value.
 *
 * @author Jan Bernitt
 */
@Transactional
class DataValueFileResourceControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private DbmsManager dbmsManager;

  @Autowired private FileResourceService fileResourceService;
  private String pe;

  private String de;

  private String ou;

  private String coc;

  @BeforeEach
  void setUp() {
    pe = "2021-01";
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    String cc = ccDefault.getString("id").string();
    coc = ccDefault.getArray("categoryOptionCombos").getString(0).string();
    de = addDataElement("file data", "FDE1", ValueType.FILE_RESOURCE, null, cc);
    ou =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    // add OU to users hierarchy
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body("{'additions':[{'id':'" + ou + "'}]}")));
  }

  @Test
  void testClearFileResourceDataValue_EmptyFile() {
    assertClearsFileResourceDataValue(
        () ->
            // making a request to the /dataValues/file with "file" being undefined
            assertStatus(
                HttpStatus.ACCEPTED,
                POST(format("/dataValues/file?de=%s&pe=%s&ou=%s&co=%s&file=", de, pe, ou, coc))));
  }

  @Test
  void testClearFileResourceDataValue_EmptyValue() {
    assertClearsFileResourceDataValue(
        () ->
            // making a request to normal /dataValues endpoint with an undefined "value"
            assertStatus(
                HttpStatus.CREATED,
                POST(format("/dataValues?de=%s&pe=%s&ou=%s&co=%s", de, pe, ou, coc))));
  }

  @Test
  @DisplayName("A data reader who did not upload the file reads its metadata and content")
  void testReadFileDataValueUploadedByAnotherUser() throws ConflictException {
    byte[] content = "value content".getBytes(StandardCharsets.UTF_8);
    FileResource file = createFileResource('D', content);
    file.setName("report.txt");
    file.setContentType("text/plain");
    fileResourceService.syncSaveFileResource(file, content);
    file.setStorageStatus(FileResourceStorageStatus.STORED);
    String fileUid = file.getUid();
    assertStatus(
        HttpStatus.CREATED,
        POST(format("/dataValues?de=%s&pe=%s&ou=%s&co=%s&value=%s", de, pe, ou, coc, fileUid)));

    User reader = createAndAddUser("dataValueReader", manager.get(OrganisationUnit.class, ou));
    switchToNewUser(reader);

    // Capture and Data Entry resolve the file from the data value, then read its metadata
    assertEquals(
        fileUid,
        GET("/dataValues?de={de}&pe={pe}&ou={ou}", de, pe, ou).content().getString(0).string());
    JsonObject metadata = GET("/fileResources/{uid}", fileUid).content(HttpStatus.OK);
    assertEquals("report.txt", metadata.getString("name").string());
    assertEquals("DATA_VALUE", metadata.getString("domain").string());

    // the content is served by the data value API, not by the file resource API
    assertEquals(
        "value content",
        GET("/dataValues/files?de={de}&pe={pe}&ou={ou}&co={co}", de, pe, ou, coc)
            .content("text/plain"));
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("Audit files: readers of the data value read its current and former files")
  void testGetAuditFile_readersOfTheDataValue() {
    String former = uploadDataValueFile("former content");
    String current = uploadDataValueFile("current content");
    storeFileValue(de, coc, former);
    storeFileValue(de, coc, current);

    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, ou);
    User captureUser = createAndAddUser("auditCaptureReader", orgUnit);
    User dataViewUser = createAndAddUser(new HashSet<>(), new HashSet<>(Set.of(orgUnit)));

    for (User reader : List.of(captureUser, dataViewUser)) {
      switchToNewUser(reader);
      assertEquals("former content", GET("/audits/files/{uid}", former).content("text/plain"));
      assertEquals("current content", GET("/audits/files/{uid}", current).content("text/plain"));
    }
  }

  @Test
  @DisplayName("Audit files: users who may not read a data value holding the file are refused")
  void testGetAuditFile_deniedWithoutReadableDataValue() {
    String former = uploadDataValueFile("former content");
    String current = uploadDataValueFile("current content");
    String unbound = uploadDataValueFile("unbound content");
    storeFileValue(de, coc, former);
    storeFileValue(de, coc, current);

    OrganisationUnit otherOrgUnit = manager.get(OrganisationUnit.class, addOrganisationUnit("OU2"));
    User outsider = createAndAddUser("auditOutsider", otherOrgUnit);
    User reader = createAndAddUser("auditReader", manager.get(OrganisationUnit.class, ou));

    switchToNewUser(outsider);
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", former).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", current).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", unbound).status());
    // the uploader of a file keeps reading it
    String own = uploadDataValueFile("own content");
    assertEquals("own content", GET("/audits/files/{uid}", own).content("text/plain"));

    // no data value holds these files
    switchToNewUser(reader);
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", unbound).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", own).status());

    switchToAdminUser();
    assertEquals("unbound content", GET("/audits/files/{uid}", unbound).content("text/plain"));
    assertEquals("former content", GET("/audits/files/{uid}", former).content("text/plain"));
    assertEquals("own content", GET("/audits/files/{uid}", own).content("text/plain"));
  }

  @Test
  @DisplayName("Audit files: the data read sharing of the data value's category options applies")
  void testGetAuditFile_requiresDataReadOnCategoryOptions() {
    String option =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptions",
                "{'name':'Restricted','shortName':'Restricted','sharing':{'public':'rw------'}}"));
    String category =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                format(
                    "{'name':'Restricted','shortName':'Restricted',"
                        + "'dataDimensionType':'DISAGGREGATION','categoryOptions':[{'id':'%s'}]}",
                    option)));
    String combo =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                format(
                    "{'name':'Restricted','dataDimensionType':'DISAGGREGATION',"
                        + "'categories':[{'id':'%s'}]}",
                    category)));
    String restrictedCoc =
        GET("/categoryCombos/{id}?fields=categoryOptionCombos[id]", combo)
            .content()
            .getArray("categoryOptionCombos")
            .getObject(0)
            .getString("id")
            .string();
    String restrictedDe =
        addDataElement("restricted file", "FDE2", ValueType.FILE_RESOURCE, null, combo);
    String file = uploadDataValueFile("restricted content");
    storeFileValue(restrictedDe, restrictedCoc, file);

    User reader = createAndAddUser("auditReader", manager.get(OrganisationUnit.class, ou));
    switchToNewUser(reader);
    assertEquals(HttpStatus.FORBIDDEN, GET("/audits/files/{uid}", file).status());

    switchToAdminUser();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/sharing?type=categoryOption&id=" + option,
            "{'object':{'publicAccess':'rwr-----','userAccesses':[],'userGroupAccesses':[]}}"));
    switchToNewUser(reader);
    assertEquals("restricted content", GET("/audits/files/{uid}", file).content("text/plain"));
  }

  private String uploadDataValueFile(String content) {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "value.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
    return POST_MULTIPART("/fileResources?domain=DATA_VALUE", file)
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  private void storeFileValue(String dataElement, String categoryOptionCombo, String fileUid) {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            format(
                "/dataValues?de=%s&pe=%s&ou=%s&co=%s&value=%s",
                dataElement, pe, ou, categoryOptionCombo, fileUid)));
    // like a new request, the next write loads the stored value, which is what audits a replaced
    // value
    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }

  private void assertClearsFileResourceDataValue(Runnable clearRequest) {
    String url = format("/api/dataValues/file?de=%s&pe=%s&ou=%s&co=%s", de, pe, ou, coc);
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    // create the data value with a file resource that is cleared
    assertStatus(HttpStatus.ACCEPTED, POST_MULTIPART(url, image));

    // check the file resource does exist
    JsonArray values = GET("/dataValues?de={de}&pe={pe}&ou={ou}", de, pe, ou).content();
    assertEquals(1, values.size());
    String fileUid = values.getString(0).string();
    assertTrue(CodeGenerator.isValidUid(fileUid));
    assertStatus(HttpStatus.OK, GET("/fileResources/{id}", fileUid));

    // clear the data value
    clearRequest.run();

    // check the file resource no longer exists
    assertStatus(HttpStatus.NOT_FOUND, GET("/fileResources/{id}", fileUid));
  }
}

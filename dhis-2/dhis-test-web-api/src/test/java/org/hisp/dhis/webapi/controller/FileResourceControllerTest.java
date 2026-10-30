/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FileResourceControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testSaveTooBigFileSize() {
    byte[] bytes = new byte[10_000_001];
    MockMultipartFile image =
        new MockMultipartFile("file", "OU_profile_image.png", "image/png", bytes);
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "File size can't be bigger than 10000000, current file size 10000001",
        errorMessage.string());
  }

  @Test
  void testSaveBadAvatarImageData() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage =
        response.content(HttpStatus.INTERNAL_SERVER_ERROR).getString("message");
    assertEquals("Failed to resize image: src cannot be null", errorMessage.string());
  }

  @Test
  void testSaveBadAvatarContentType() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/tiff", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "Invalid content type, valid content types are: image/jpeg,image/png,image/gif",
        errorMessage.string());
  }

  @Test
  void testSaveBadAvatarFileExtension() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.tiff", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "Wrong file extension, valid extensions are: jpg,jpeg,png,gif", errorMessage.string());
  }

  @Test
  void testSaveBadAvatarFileSize() {
    byte[] bytes = new byte[2_000_001];
    MockMultipartFile image =
        new MockMultipartFile("file", "OU_profile_image.png", "image/png", bytes);
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonString errorMessage = response.content(HttpStatus.CONFLICT).getString("message");
    assertEquals(
        "File size can't be bigger than 2000000, current file size 2000001", errorMessage.string());
  }

  @Test
  void testSaveGoodAvatar() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "dhis2.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("dhis2.png", savedObject.getString("name").string());
  }

  @Test
  void testSaveOrgUnitImage() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());

    String uid = savedObject.getString("id").string();
    JsonObject fr = GET("/fileResources/{uid}", uid).content();
    assertEquals("ORG_UNIT", fr.getString("domain").string());

    JsonObject large = GET("/fileResources/{uid}?dimension=LARGE", uid).content();
    assertEquals("ORG_UNIT", large.getString("domain").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=fr123456789", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("fr123456789", savedObject.getString("id").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid_Update() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=fr987654321", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("fr987654321", savedObject.getString("id").string());

    // now update the resource with a different image but the same UID
    MockMultipartFile image2 =
        new MockMultipartFile(
            "file", "OU_profile_image2.png", "image/png", Files.readAllBytes(file.toPath()));

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "FileResource already exists: `fr987654321`",
            POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=fr987654321", image2)
                .content(HttpStatus.CONFLICT));
    assertEquals(ErrorCode.E1119, message.getErrorCode());
  }

  // ---------------------------------------------------------------------------
  // Access control (DHIS2-20203)
  //
  // Who may read a file resource follows from the object that owns it. Both halves of the contract
  // are pinned here: an unrelated caller is denied, and the callers that own or share the object
  // are served.
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("A document file resource is not readable by an unrelated user")
  void testGetDocument_deniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "report.pdf");
    createDocument(fileUid, "--------");

    switchToNewUser("documentOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A message attachment is not readable by a user outside the conversation")
  void testGetMessageAttachment_deniedForNonParticipant() {
    User recipient = switchToNewUser("messageRecipient");
    switchToAdminUser();

    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    sendMessageWithAttachment(recipient.getUid(), fileUid);

    switchToNewUser("messageOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A job data file resource is not readable without a job authority or ownership")
  void testGetJobData_deniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.JOB_DATA, "payload.json");

    switchToNewUser("jobOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A message attachment is readable by a participant of the conversation")
  void testGetMessageAttachment_allowedForParticipant() {
    User recipient = switchToNewUser("attachmentReader");
    switchToAdminUser();

    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    sendMessageWithAttachment(recipient.getUid(), fileUid);

    switchToNewUser(recipient);

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("A document file resource is readable when the owning document is shared publicly")
  void testGetDocument_allowedWhenDocumentIsPubliclyReadable() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "public.pdf");
    createDocument(fileUid, "rw------");

    switchToNewUser("documentReader");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("The uploader can still read a file resource they created")
  void testGetData_allowedForUploader() {
    switchToNewUser("fileUploader");

    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "draft.pdf");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A superuser can still read any file resource")
  void testGetData_allowedForSuperuser() {
    String fileUid = uploadFileResource(FileResourceDomain.JOB_DATA, "payload.json");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("Data value file content is never served here, but its metadata stays readable")
  void testGetDataValue_contentDeniedMetadataReadable() {
    String fileUid = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");

    // the uploading superuser is refused the content, because data value files are read through the
    // data value API where the data access rules of the owning data value apply
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());

    // clients resolve name, size and content type of a data value file uploaded by someone else
    switchToNewUser("dataValueViewer");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    JsonObject metadata = GET("/fileResources/{uid}", fileUid).content(HttpStatus.OK);
    assertEquals("value.txt", metadata.getString("name").string());
    assertEquals(
        "value.txt",
        GET("/fileResources/{uid}?fields=name", fileUid).content().getString("name").string());
  }

  @Test
  @DisplayName("Field, property and gist reads of a file resource apply the same access rule")
  void testGetDocument_inheritedMetadataRoutesDeniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(fileUid, "--------");

    switchToNewUser("metadataOutsider");

    assertAllMetadataRoutes(HttpStatus.FORBIDDEN, fileUid);
  }

  @Test
  @DisplayName("The generic identifiable object routes do not resolve a file resource")
  void testGetDocument_identifiableObjectRoutesDoNotResolveFileResource() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(fileUid, "--------");

    assertIdentifiableObjectRoutesNotFound(fileUid);
    switchToNewUser("identifiableObjectOutsider");
    assertIdentifiableObjectRoutesNotFound(fileUid);
  }

  @Test
  @DisplayName("Field, property and gist reads stay available to a reader of the owning document")
  void testGetDocument_inheritedMetadataRoutesAllowedForDocumentReader() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "shared.pdf");
    createDocument(fileUid, "r-------");

    switchToNewUser("metadataReader");

    assertAllMetadataRoutes(HttpStatus.OK, fileUid);
    assertEquals(
        "shared.pdf",
        GET("/fileResources/{uid}/name", fileUid).content().getString("name").string());
  }

  @Test
  @DisplayName("A user avatar is readable with F_USER_VIEW only")
  void testGetUserAvatar_requiresUserViewAuthority() throws IOException {
    String fileUid = uploadImage(FileResourceDomain.USER_AVATAR);

    switchToNewUser("avatarOutsider");
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}", fileUid).status());

    switchToNewUser("avatarViewer", "F_USER_VIEW");
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A job data file resource is readable with F_JOB_LOG_READ")
  void testGetJobData_allowedWithJobLogReadAuthority() {
    String fileUid = uploadFileResource(FileResourceDomain.JOB_DATA, "payload.json");

    switchToNewUser("jobLogReader", "F_JOB_LOG_READ");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A participant that leaves the conversation can no longer read its attachment")
  void testGetMessageAttachment_deniedAfterLeavingConversation() {
    User recipient = switchToNewUser("leavingRecipient");
    switchToAdminUser();

    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    String conversationUid = sendMessageWithAttachment(recipient.getUid(), fileUid);

    switchToNewUser(recipient);
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());

    assertStatus(
        HttpStatus.OK,
        DELETE("/messageConversations/" + conversationUid + "/" + recipient.getUid()));

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  @DisplayName("A custom icon stays readable by any authenticated user")
  void testGetIcon_allowedForAnyUser() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "icon.png", "image/png", Files.readAllBytes(file.toPath()));
    String fileUid =
        POST_MULTIPART("/fileResources?domain=ICON", image)
            .content(HttpStatus.ACCEPTED)
            .getObject("response")
            .getObject("fileResource")
            .getString("id")
            .string();

    switchToNewUser("iconViewer");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("An org unit image stays readable by any authenticated user")
  void testGetOrgUnitImage_allowedForAnyUser() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    String fileUid =
        POST_MULTIPART("/fileResources?domain=ORG_UNIT", image)
            .content(HttpStatus.ACCEPTED)
            .getObject("response")
            .getObject("fileResource")
            .getString("id")
            .string();

    switchToNewUser("orgUnitImageViewer");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  private void assertAllMetadataRoutes(HttpStatus expected, String fileUid) {
    assertEquals(expected, GET("/fileResources/{uid}?fields=id,name", fileUid).status());
    assertEquals(expected, GET("/fileResources/{uid}/name", fileUid).status());
    assertEquals(expected, GET("/fileResources/{uid}/contentMd5", fileUid).status());
    assertEquals(expected, GET("/fileResources/{uid}/gist", fileUid).status());
    assertEquals(expected, GET("/fileResources/{uid}/name/gist", fileUid).status());
    for (String csvPath :
        List.of("/fileResources/{uid}/gist.csv", "/fileResources/{uid}/name/gist.csv")) {
      HttpResponse csv = GET(csvPath, fileUid);
      assertEquals(expected, csv.status());
      // a text/csv response cannot carry the JSON error body, so a denial has no body at all
      assertEquals(expected == HttpStatus.OK, csv.hasBody());
    }
  }

  private void assertIdentifiableObjectRoutesNotFound(String fileUid) {
    for (String path :
        List.of(
            "/identifiableObjects/{uid}",
            "/identifiableObjects/{uid}?fields=name,contentMd5,domain",
            "/identifiableObjects/{uid}/name",
            "/identifiableObjects/{uid}/contentMd5")) {
      assertEquals(HttpStatus.NOT_FOUND, GET(path, fileUid).status(), path);
    }
    for (String path :
        List.of(
            "/identifiableObjects/{uid}/gist",
            "/identifiableObjects/{uid}/name/gist",
            "/identifiableObjects?filter=id:eq:{uid}&fields=name")) {
      assertFalse(GET(path, fileUid).contentUnchecked().toString().contains("private.pdf"), path);
    }
  }

  // ---------------------------------------------------------------------------
  // DHIS2-21661: the generic read endpoints inherited from AbstractFullReadOnlyController and
  // AbstractGistReadOnlyController.
  //
  // FileResource is not shareable and its schema declares no authorities, so the type level
  // aclService.canRead check guarding those endpoints holds for every authenticated user, and the
  // Gist endpoints check nothing at all. Every route asserted below answered 200 to any account
  // before this fix: the per-object ones served the same metadata the /{uid} gate protects, and
  // the collection ones let a caller page every uid and domain in the system.
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("The fields variant of the metadata endpoint is not a way around the object gate")
  void testGetObjectWithFields_deniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "report.pdf");
    createDocument(fileUid, "--------");

    switchToNewUser("fieldsOutsider");

    assertEquals(
        HttpStatus.FORBIDDEN, GET("/fileResources/{uid}?fields=id,name", fileUid).status());
  }

  @Test
  @DisplayName("A single property of a file resource is not readable by an unrelated user")
  void testGetObjectProperty_deniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "report.pdf");
    createDocument(fileUid, "--------");

    switchToNewUser("propertyOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/name", fileUid).status());
  }

  @Test
  @DisplayName("The uploader still reads their own file resource through the generic endpoints")
  void testGetObjectWithFields_allowedForUploader() {
    switchToNewUser("fieldsUploader");

    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "draft.pdf");

    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}?fields=id,name", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/name", fileUid).status());
  }

  @Test
  @DisplayName("File resources cannot be enumerated by a plain authenticated user")
  void testGetObjectList_deniedForPlainUser() {
    switchToNewUser("enumerator");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources").status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/gist").status());
  }

  @Test
  @DisplayName("A superuser can still enumerate file resources")
  void testGetObjectList_allowedForSuperuser() {
    assertEquals(HttpStatus.OK, GET("/fileResources").status());
    assertEquals(HttpStatus.OK, GET("/fileResources/gist").status());
  }

  @Test
  @DisplayName("The Gist endpoints do not serve a single file resource either")
  void testGetObjectGist_deniedForPlainUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "report.pdf");
    createDocument(fileUid, "--------");

    switchToNewUser("gistOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/gist", fileUid).status());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/name/gist", fileUid).status());
  }

  @Test
  @DisplayName("The storage key owner lookup requires a superuser")
  void testGetOwners_deniedForPlainUser() {
    switchToNewUser("ownersOutsider");

    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/owners?storageKey=any").status());
  }

  // ---------------------------------------------------------------------------
  // DHIS2-21661: the metadata export resolves every identifiable type by its plural name, so
  // GET /api/metadata?fileResources=true lists file resources outside the endpoints above. It must
  // only contain the ones FileResourceService.canRead allows the caller to read.
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("The metadata export only contains the file resources a plain user may read")
  void testMetadataExport_onlyReadableFileResourcesForPlainUser() throws IOException {
    String privateDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(privateDocument, "--------");
    String publicDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "public.pdf");
    createDocument(publicDocument, "r-------");
    String dataValue = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");
    String unassignedDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "unassigned.pdf");
    String icon = uploadImage(FileResourceDomain.ICON);
    String orgUnitImage = uploadImage(FileResourceDomain.ORG_UNIT);

    switchToNewUser("metadataExporter");
    String own = uploadFileResource(FileResourceDomain.DOCUMENT, "own.pdf");

    Set<String> exported = exportedFileResourceIds("");
    assertTrue(
        exported.containsAll(Set.of(own, publicDocument, icon, orgUnitImage)), exported::toString);
    assertFalse(exported.contains(privateDocument));
    assertFalse(exported.contains(dataValue));
    assertFalse(exported.contains(unassignedDocument));
    assertEquals(Set.of(), exportedFileResourceIds("&filter=id:eq:" + privateDocument));
  }

  @Test
  @DisplayName("The metadata export still contains every file resource for a superuser")
  void testMetadataExport_allFileResourcesForSuperuser() {
    String dataValue = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");
    switchToNewUser("exportedUploader");
    String otherUsers = uploadFileResource(FileResourceDomain.DOCUMENT, "other.pdf");
    switchToAdminUser();

    assertTrue(exportedFileResourceIds("").containsAll(Set.of(dataValue, otherUsers)));
  }

  private Set<String> exportedFileResourceIds(String query) {
    JsonList<JsonObject> fileResources =
        GET("/metadata?fileResources=true&fields=id" + query)
            .content(HttpStatus.OK)
            .getList("fileResources", JsonObject.class);
    if (!fileResources.exists()) {
      return Set.of();
    }
    return fileResources.stream().map(fr -> fr.getString("id").string()).collect(toSet());
  }

  private String uploadFileResource(FileResourceDomain domain, String filename) {
    MockMultipartFile file =
        new MockMultipartFile("file", filename, "application/octet-stream", "payload".getBytes());
    return POST_MULTIPART("/fileResources?domain=" + domain.name(), file)
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  private String uploadImage(FileResourceDomain domain) throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "dhis2.png", "image/png", Files.readAllBytes(file.toPath()));
    return POST_MULTIPART("/fileResources?domain=" + domain.name(), image)
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  /**
   * A non-external {@link org.hisp.dhis.document.Document} carries the file resource UID in its
   * {@code url}; {@code DocumentObjectBundleHook} resolves it and links the two.
   *
   * <p>The metadata importer always applies the default public sharing on create, so the requested
   * sharing is applied afterwards through the sharing API.
   */
  private void createDocument(String fileResourceUid, String publicAccess) {
    String documentUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/documents",
                "{'name':'Doc for "
                    + fileResourceUid
                    + "','external':false,'type':'UPLOAD_FILE','attachment':false,'url':'"
                    + fileResourceUid
                    + "'}"));

    assertStatus(
        HttpStatus.OK,
        PUT(
            "/sharing?type=document&id=" + documentUid,
            "{'object':{'publicAccess':'"
                + publicAccess
                + "','externalAccess':false,'userAccesses':[],'userGroupAccesses':[]}}"));

    assertEquals(
        publicAccess,
        GET("/documents/{uid}?fields=sharing", documentUid)
            .content()
            .getObject("sharing")
            .getString("public")
            .string(),
        "sharing precondition for this test was not applied");
  }

  private String sendMessageWithAttachment(String recipientUid, String fileResourceUid) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/messageConversations",
            "{'subject':'Subject','text':'Body','users':[{'id':'"
                + recipientUid
                + "'}],'attachments':[{'id':'"
                + fileResourceUid
                + "'}]}"));
  }
}

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
package org.hisp.dhis.webapi.controller;

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

class FileResourceControllerTest extends DhisControllerConvenienceTest {
  @Autowired private FileResourceService fileResourceService;

  @Autowired private FileResourceContentStore fileResourceContentStore;

  @Autowired private FileResourceStore fileResourceStore;

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
  void testSaveBadOrgUnitImageData() {
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", "<<png data>>".getBytes());
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT", image);
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
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789a", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789a", savedObject.getString("id").string());
  }

  @Test
  void testSaveOrgUnitImageWithUid_Update() throws IOException {
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile(
            "file", "OU_profile_image.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse response = POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789x", image);
    JsonObject savedObject =
        response.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    assertEquals("OU_profile_image.png", savedObject.getString("name").string());
    assertEquals("0123456789x", savedObject.getString("id").string());

    // now update the resource with a different image but the same UID
    MockMultipartFile image2 =
        new MockMultipartFile(
            "file", "OU_profile_image2.png", "image/png", Files.readAllBytes(file.toPath()));

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "FileResource already exists: `0123456789x`",
            POST_MULTIPART("/fileResources?domain=ORG_UNIT&uid=0123456789x", image2)
                .content(HttpStatus.CONFLICT));
    assertEquals(ErrorCode.E1119, message.getErrorCode());
  }

  @Test
  void testGetDocument_deniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "report.pdf");
    createDocument(fileUid, "--------");
    switchToNewUser("documentOutsider");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetDocument_allowedWhenDocumentIsPubliclyReadable() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "public.pdf");
    createDocument(fileUid, "r-------");
    switchToNewUser("documentReader");
    assertEquals(
        "payload", GET("/fileResources/{uid}/data", fileUid).content("application/octet-stream"));
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetMessageAttachment_deniedForNonParticipant() {
    User recipient = switchToNewUser("messageRecipient");
    switchToNewUser(superUser);
    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    sendMessageWithAttachment(recipient.getUid(), fileUid);
    switchToNewUser("messageOutsider");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetMessageAttachment_allowedForParticipant() {
    User recipient = switchToNewUser("attachmentReader");
    switchToNewUser(superUser);
    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    sendMessageWithAttachment(recipient.getUid(), fileUid);
    switchToNewUser(recipient);
    assertEquals(
        "payload", GET("/fileResources/{uid}/data", fileUid).content("application/octet-stream"));
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetData_allowedForUploader() {
    switchToNewUser("fileUploader");
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "draft.pdf");
    assertEquals(
        "payload", GET("/fileResources/{uid}/data", fileUid).content("application/octet-stream"));
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetObjectWithFields_allowedForUploader() {
    switchToNewUser("fieldsUploader");
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "draft.pdf");
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}?fields=id,name", fileUid).status());
  }

  @Test
  void testUnassignedMessageAttachmentHasNoParticipants() {
    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "orphan.txt");
    assertTrue(fileResourceStore.findMessageAttachmentParticipants(fileUid).isEmpty());
    switchToNewUser("orphanOutsider");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetData_allowedForSuperuser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "payload.json");
    assertEquals(
        "payload", GET("/fileResources/{uid}/data", fileUid).content("application/octet-stream"));
  }

  @Test
  void testGetDataValue_contentDeniedMetadataReadable() {
    String fileUid = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
    switchToNewUser("dataValueViewer");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(
        "value.txt",
        GET("/fileResources/{uid}", fileUid).content(HttpStatus.OK).getString("name").string());
    assertEquals(
        "value.txt",
        GET("/fileResources/{uid}?fields=name", fileUid)
            .content(HttpStatus.OK)
            .getString("name")
            .string());
  }

  @Test
  void testGetDocument_metadataRoutesDeniedForUnrelatedUser() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(fileUid, "--------");
    switchToNewUser("metadataOutsider");
    assertEquals(
        HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}?fields=id,name", fileUid).status());
    assertUnsupportedMetadataRoutes(fileUid);
  }

  @Test
  void testGetDocument_identifiableObjectRoutesDoNotResolveFileResource() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(fileUid, "--------");
    assertIdentifiableObjectRoutesNotFound(fileUid);
    switchToNewUser("identifiableObjectOutsider");
    assertIdentifiableObjectRoutesNotFound(fileUid);
  }

  @Test
  void testGetDocument_metadataRoutesForDocumentReader() {
    String fileUid = uploadFileResource(FileResourceDomain.DOCUMENT, "shared.pdf");
    createDocument(fileUid, "r-------");
    switchToNewUser("metadataReader");
    assertEquals(
        "shared.pdf",
        GET("/fileResources/{uid}?fields=name", fileUid)
            .content(HttpStatus.OK)
            .getString("name")
            .string());
    assertUnsupportedMetadataRoutes(fileUid);
  }

  @Test
  void testGetUserAvatar_requiresUserViewAuthority() throws IOException {
    String fileUid = uploadImage(FileResourceDomain.USER_AVATAR);
    switchToNewUser("avatarOutsider");
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}", fileUid).status());
    switchToNewUser("avatarViewer", "F_USER_VIEW");
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetMessageAttachment_deniedAfterLeavingConversation() {
    User recipient = switchToNewUser("leavingRecipient");
    switchToNewUser(superUser);
    String fileUid = uploadFileResource(FileResourceDomain.MESSAGE_ATTACHMENT, "attachment.txt");
    String conversationUid = sendMessageWithAttachment(recipient.getUid(), fileUid);
    switchToNewUser(recipient);
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
    assertStatus(
        HttpStatus.OK,
        DELETE("/messageConversations/" + conversationUid + "/" + recipient.getUid()));
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}/data", fileUid).status());
    assertEquals(HttpStatus.UNAUTHORIZED, GET("/fileResources/{uid}", fileUid).status());
  }

  @Test
  void testGetOrgUnitImage_allowedForAnyUser() throws IOException {
    String fileUid = uploadImage(FileResourceDomain.ORG_UNIT);
    switchToNewUser("orgUnitImageViewer");
    assertEquals(HttpStatus.OK, GET("/fileResources/{uid}/data", fileUid).status());
  }

  // DHIS2-21661: this line has no collection, Gist or storage key owner route for file resources,
  // so a caller cannot enumerate them through this controller.
  @Test
  void testCollectionRoutesAreNotExposed() {
    switchToNewUser("enumerator");
    uploadFileResource(FileResourceDomain.DOCUMENT, "own.pdf");

    for (String path :
        List.of("/fileResources", "/fileResources/gist", "/fileResources/owners?storageKey=any")) {
      HttpResponse response = GET(path);
      assertNotEquals(HttpStatus.OK, response.status(), path);
      assertFalse(response.contentUnchecked().toString().contains("own.pdf"), path);
    }
  }

  // DHIS2-21661: the metadata export resolves every identifiable type by its plural name, so
  // GET /api/metadata?fileResources=true lists file resources outside this controller. It must
  // only contain the ones FileResourceService.canRead allows the caller to read.

  @Test
  void testMetadataExport_onlyReadableFileResourcesForPlainUser() throws IOException {
    String privateDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "private.pdf");
    createDocument(privateDocument, "--------");
    String publicDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "public.pdf");
    createDocument(publicDocument, "r-------");
    String dataValue = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");
    String unassignedDocument = uploadFileResource(FileResourceDomain.DOCUMENT, "unassigned.pdf");
    String orgUnitImage = uploadImage(FileResourceDomain.ORG_UNIT);

    switchToNewUser("metadataExporter");
    String own = uploadFileResource(FileResourceDomain.DOCUMENT, "own.pdf");

    Set<String> exported = exportedFileResourceIds("");
    assertTrue(exported.containsAll(Set.of(own, publicDocument, orgUnitImage)), exported::toString);
    assertFalse(exported.contains(privateDocument));
    assertFalse(exported.contains(dataValue));
    assertFalse(exported.contains(unassignedDocument));
    assertEquals(Set.of(), exportedFileResourceIds("&filter=id:eq:" + privateDocument));
  }

  @Test
  void testMetadataExport_allFileResourcesForSuperuser() {
    String dataValue = uploadFileResource(FileResourceDomain.DATA_VALUE, "value.txt");
    switchToNewUser("exportedUploader");
    String otherUsers = uploadFileResource(FileResourceDomain.DOCUMENT, "other.pdf");
    switchToNewUser(superUser);

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

  private void assertUnsupportedMetadataRoutes(String fileUid) {
    for (String suffix :
        List.of("/name", "/contentMd5", "/gist", "/gist.csv", "/name/gist", "/name/gist.csv")) {
      assertEquals(
          HttpStatus.NOT_FOUND, GET("/fileResources/" + fileUid + suffix).status(), suffix);
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

  private String uploadFileResource(FileResourceDomain domain, String filename) {
    return upload(domain, filename, "application/octet-stream", "payload".getBytes());
  }

  private String uploadImage(FileResourceDomain domain) throws IOException {
    byte[] bytes = Files.readAllBytes(new ClassPathResource("file/dhis2.png").getFile().toPath());
    FileResource file = createFileResource('I', bytes);
    file.setName("dhis2.png");
    file.setContentType("image/png");
    file.setDomain(domain);
    file.setCreatedBy(getCurrentUser());
    fileResourceService.saveFileResource(file, bytes);
    fileResourceContentStore.saveFileResourceContent(file, bytes);
    return file.getUid();
  }

  private String upload(
      FileResourceDomain domain, String filename, String contentType, byte[] bytes) {
    String uid =
        POST_MULTIPART(
                "/fileResources?domain=" + domain.name(),
                new MockMultipartFile("file", filename, contentType, bytes))
            .content(HttpStatus.ACCEPTED)
            .getObject("response")
            .getObject("fileResource")
            .getString("id")
            .string();
    // File content events run after commit; supply fixture bytes within this test transaction.
    fileResourceContentStore.saveFileResourceContent(
        fileResourceService.getFileResource(uid), bytes);
    return uid;
  }

  private void createDocument(String fileResourceUid, String publicAccess) {
    String documentUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/documents",
                "{'name':'Doc for "
                    + fileResourceUid
                    + "','external':false,'attachment':false,'url':'"
                    + fileResourceUid
                    + "'}"));
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/sharing?type=document&id=" + documentUid,
            "{'object':{'publicAccess':'"
                + publicAccess
                + "','externalAccess':false,'userAccesses':[],'userGroupAccesses':[]}}"));
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

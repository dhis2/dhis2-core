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
package org.hisp.dhis.icon;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IconTest extends PostgresIntegrationTestBase {

  @Autowired private FileResourceService fileResourceService;
  @Autowired private IconService iconService;

  private final Set<String> keywords = Set.of("k1", "k2", "m1");
  private final String key = "iconKey";

  private Icon icon;
  private FileResource fileResource;
  private User currentUser;

  @BeforeAll
  void setUp() {
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    currentUser = userService.getUserByUsername(currentUsername);
    injectSecurityContextUser(currentUser);

    fileResource = addFileResource('A');
    icon = assertDoesNotThrow(() -> addIcon(key, "description", keywords, fileResource));
  }

  @Test
  void shouldGetIconByKey() throws NotFoundException {
    assertIcon(iconService.getIcon(key));
  }

  @Test
  void shouldGetIconsMatchingILikeKeyOrKeywords() throws Exception {
    Icon icon1 = addIcon("tb-ward", keywords, addFileResource('I'));
    Icon icon2 = addIcon("malaria-ward", keywords, addFileResource('J'));
    Icon icon3 = addIcon("non-matching-key", Set.of("corona-ward"), addFileResource('K'));

    IconQueryParams operationParams = new IconQueryParams();
    operationParams.setSearch("ward");
    List<Icon> icons = iconService.getIcons(operationParams);

    assertContainsOnly(List.of(icon1, icon2, icon3), icons);
  }

  @Test
  void shouldSaveIconWithNoKeywords() throws Exception {
    Icon iconWithNoKeywords = addIcon("iconKey2", null);

    assertEquals(Set.of(), iconWithNoKeywords.getKeywords());
  }

  @Test
  void shouldFailWhenUpdatingDefaultIcon() throws BadRequestException, NotFoundException {
    Icon defaultIcon =
        addIcon("3G", "description", keywords, addFileResource('G'), DefaultIcon._3G);

    UpdateIconRequest update =
        UpdateIconRequest.builder().keywords(keywords).description("description updated").build();

    assertBadRequestException(
        "Not allowed to update default icon",
        () -> iconService.updateIcon(defaultIcon.getKey(), update));
  }

  @Test
  void shouldUpdate() throws BadRequestException, NotFoundException {
    UpdateIconRequest update =
        UpdateIconRequest.builder()
            .keywords(Set.of("new", "words"))
            .description("description updated")
            .build();

    iconService.updateIcon(icon.getKey(), update);

    Icon fetched = iconService.getIcon(icon.getKey());

    assertEquals("description updated", fetched.getDescription());
    assertEquals(Set.of("new", "words"), fetched.getKeywords());
  }

  @Test
  void shouldFailWhenSavingIconWithNoKey() {
    assertBadRequestException("Icon key not specified.", () -> addIcon(null, keywords));
  }

  @Test
  void shouldFailWhenSavingIconWithEmptyKey() {
    assertBadRequestException("Icon key not specified.", () -> addIcon("", keywords));
  }

  @Test
  void shouldFailWhenSavingIconWithInvalidKey() {
    assertBadRequestException(
        "Icon key k1 m1 is not valid. Alphanumeric and special characters '-' and '_' are allowed",
        () -> addIcon("k1 m1", keywords));
  }

  @Test
  void shouldFailWhenSavingIconWithNonExistentFileResourceId() {
    AddIconRequest addRequest =
        AddIconRequest.builder()
            .key("another_key")
            .keywords(keywords)
            .fileResourceId("c1234567890")
            .build();
    assertNotFoundException(
        "FileResource with id c1234567890 could not be found.",
        () -> iconService.addIcon(addRequest, null));
  }

  @Test
  void shouldFailWhenSavingIconWithNoFileResourceId() {
    AddIconRequest addRequest =
        AddIconRequest.builder().key("another_key").keywords(keywords).fileResourceId(null).build();
    assertNotFoundException(
        "FileResource with id null could not be found.",
        () -> iconService.addIcon(addRequest, null));
  }

  @Test
  void shouldFailWhenSavingIconAndIconWithSameKeyExists() {
    assertBadRequestException("Icon with key iconKey already exists.", () -> addIcon(key, null));
  }

  @Test
  void shouldFailWhenIconKeyDoesNotExist() {
    assertNotFoundException(
        "Icon with id non-existent-Key could not be found.",
        () -> iconService.getIcon("non-existent-Key"));
  }

  @Test
  void shouldFailWhenSavingIconWithExistingKey() {
    FileResource fileResource = addFileResource('A');
    AddIconRequest addRequest =
        AddIconRequest.builder()
            .key(key)
            .description("description")
            .keywords(keywords)
            .fileResourceId(fileResource.getUid())
            .build();

    assertBadRequestException(
        format("Icon with key %s already exists.", key),
        () -> iconService.addIcon(addRequest, null));
  }

  @Test
  void shouldFailWhenFetchingIconDataWithNonExistentKey() {
    assertNotFoundException(
        "Icon with id non-existent-Key could not be found.",
        () -> iconService.getIcon("non-existent-Key"));
  }

  @Test
  void shouldFailWhenUpdatingIconWithoutKey() {
    UpdateIconRequest update =
        UpdateIconRequest.builder()
            .keywords(Set.of("new", "words"))
            .description("description updated")
            .build();

    assertNotFoundException(
        "Icon with id null could not be found.", () -> iconService.updateIcon(null, update));
  }

  @Test
  void shouldFailWhenDeletingNonExistingIcon() {
    assertNotFoundException(
        "Icon with id unknown could not be found.", () -> iconService.deleteIcon("unknown"));
  }

  @Test
  void shouldFailWhenDeletingIconWithoutKey() {
    assertNotFoundException(
        "Icon with id null could not be found.", () -> iconService.deleteIcon(null));
  }

  @Test
  void shouldDeleteIconWhenKeyPresentAndIconExists() {
    assertDoesNotThrow(() -> iconService.deleteIcon(icon.getKey()));

    assertNotFoundException(
        "Icon with id iconKey could not be found.", () -> iconService.getIcon(icon.getKey()));
  }

  @Test
  void shouldCreateIconInDatabase() throws Exception {
    DefaultIcon origin = DefaultIcon.DOCTOR;
    for (AddIconRequest icon : origin.toVariantIcons()) {
      String fileResourceId = iconService.addDefaultIconImage(icon.getKey(), origin);
      iconService.addIcon(icon.toBuilder().fileResourceId(fileResourceId).build(), origin);
    }

    IconQueryParams params = new IconQueryParams();
    params.setSearch("doctor");
    List<Icon> icons = iconService.getIcons(params);

    assertNotEmpty(icons);
    List<String> keys = icons.stream().map(Icon::getKey).toList();

    assertEquals(3, keys.size(), format("Should have 3 icons with key %s", origin.getKeyPrefix()));
    assertTrue(keys.contains("doctor_outline"), "list should contain doctor_outline");
    assertTrue(keys.contains("doctor_negative"), "list should contain doctor_negative");
    assertTrue(keys.contains("doctor_positive"), "list should contain doctor_positive");
  }

  private Icon addIcon(String key, Set<String> keywords)
      throws BadRequestException, NotFoundException {
    return addIcon(key, keywords, addFileResource('Z'));
  }

  private Icon addIcon(String key, Set<String> keywords, FileResource image)
      throws BadRequestException, NotFoundException {
    return addIcon(key, null, keywords, image);
  }

  private Icon addIcon(String key, String description, Set<String> keywords, FileResource image)
      throws BadRequestException, NotFoundException {
    return addIcon(key, description, keywords, image, null);
  }

  private Icon addIcon(
      String key, String description, Set<String> keywords, FileResource image, DefaultIcon origin)
      throws BadRequestException, NotFoundException {
    AddIconRequest addRequest =
        AddIconRequest.builder()
            .key(key)
            .description(description)
            .keywords(keywords)
            .fileResourceId(image.getUid())
            .build();
    return iconService.addIcon(addRequest, origin);
  }

  private FileResource addFileResource(char uniqueChar) {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    String filename = "filename" + uniqueChar;

    HashCode contentMd5 = Hashing.md5().hashBytes(content);
    String contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

    FileResource fileResource =
        new FileResource(
            filename, contentType, content.length, contentMd5.toString(), FileResourceDomain.ICON);
    fileResource.setAssigned(false);
    fileResource.setCreated(new Date());
    fileResource.setAutoFields();

    try {
      String fileResourceUid = fileResourceService.syncSaveFileResource(fileResource, content);
      return fileResourceService.getFileResource(fileResourceUid);
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }

  private void assertBadRequestException(String expectedMsg, Executable task) {
    Exception exception = assertThrows(BadRequestException.class, task);
    assertEquals(expectedMsg, exception.getMessage());
  }

  private void assertNotFoundException(String expectedMsg, Executable task) {
    Exception exception = assertThrows(NotFoundException.class, task);
    assertEquals(expectedMsg, exception.getMessage());
  }

  private void assertIcon(Icon icon) {
    assertEquals(key, icon.getKey());
    assertEquals("description", icon.getDescription());
    assertEquals(keywords, icon.getKeywords());

    assertThat(icon.getKeywords(), hasSize(3));
    assertThat(fileResource, is(icon.getFileResource()));
    assertThat(currentUser, is(icon.getCreatedBy()));
  }
}

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
package org.hisp.dhis.icon;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

@Slf4j
class IconTest extends TrackerTest {

  @Autowired private FileResourceService fileResourceService;

  @Autowired private IconService iconService;

  @Autowired private UserService _userService;

  private final Set<String> keywords = new HashSet<>();
  private final String Key = "iconKey";

  private Icon icon;
  private FileResource fileResource;
  private User currentUser;

  @Override
  protected void initTest() throws IOException {

    userService = _userService;
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    currentUser = userService.getUserByUsername(currentUsername);
    injectSecurityContextUser(currentUser);

    keywords.addAll(Set.of("k1", "k2", "m1"));
    fileResource = createAndPersistFileResource('A');
    icon = new Icon(Key, "description", keywords, true, fileResource);

    try {
      iconService.addIcon(icon);
    } catch (NotFoundException | BadRequestException | SQLException e) {
      log.error("Icon creation failed", e);
    }
  }

  @Test
  void shouldGetIconByKey() throws NotFoundException {
    assertIcon(iconService.getIcon(Key));
  }

  @Test
  void shouldGetIconDataWhenKeyBelongsToIcon() throws NotFoundException {

    assertNotNull(iconService.getDefaultIconResource(Key));
  }

  @Test
  void shouldGetIconsMatchingILikeKey()
      throws SQLException, BadRequestException, NotFoundException {
    Icon icon1 = createIcon('I', keywords, createAndPersistFileResource('I'));
    Icon icon2 = createIcon('J', keywords, createAndPersistFileResource('J'));

    icon1.setKey("lab-agent");
    icon2.setKey("facility-agent");

    iconService.addIcon(icon1);
    iconService.addIcon(icon2);

    IconQueryParams operationParams = new IconQueryParams();
    operationParams.setSearch("agent");
    List<Icon> icons = iconService.getIcons(operationParams);

    Assertions.assertContainsOnly(List.of(icon1, icon2), icons);
  }

  @Test
  void shouldSaveIconWithNoKeywords() throws BadRequestException, NotFoundException, SQLException {
    FileResource fileResource = createAndPersistFileResource('D');

    Icon iconWithNoKeywords = new Icon("iconKey2", "description", null, true, fileResource);

    iconService.addIcon(iconWithNoKeywords);

    assertIcon(iconService.getIcon("iconKey"));
  }

  @Test
  void shouldFailWhenUpdatingDefaultIcon() {

    Icon defaultIcon = new Icon("iconKey2", "description", null, false, null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.updateIcon(defaultIcon));

    assertEquals("Not allowed to update default icon", exception.getMessage());
  }

  @Test
  void shouldUpdateLastUpdatedWhenIconIsUpdated()
      throws BadRequestException, NotFoundException, SQLException {

    Icon iconUpdated = icon;
    iconUpdated.setDescription("description updated");

    iconService.updateIcon(iconUpdated);

    Icon fetched = iconService.getIcon(icon.getKey());

    assertEquals("description updated", fetched.getDescription());
  }

  @Test
  void shouldFailWhenUpdatingIconWithNoKey() {
    String invalidKey = "k1 m2";

    Icon iconUpdated = icon;
    iconUpdated.setKey(invalidKey);
    iconUpdated.setDescription("description updated");

    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.updateIcon(iconUpdated));

    assertEquals(
        String.format(
            "Icon key %s is not valid. Alphanumeric and special characters '-' and '_' are allowed",
            invalidKey),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconWithNoKey() {
    FileResource fileResource = createAndPersistFileResource('B');

    Icon iconWithNullKey = new Icon(null, "description", keywords, true, fileResource);
    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.addIcon(iconWithNullKey));

    assertEquals("Icon key not specified.", exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconWithInvalidKey() {
    FileResource fileResource = createAndPersistFileResource('B');
    String invalidKey = "k1 m1";
    Icon iconWithInvalidKey = new Icon(invalidKey, "description", keywords, true, fileResource);
    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.addIcon(iconWithInvalidKey));

    assertEquals(
        String.format(
            "Icon key %s is not valid. Alphanumeric and special characters '-' and '_' are allowed",
            invalidKey),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenIconKeyDoesNotExist() {

    String nonExistingKey = "non-existent-Key";
    Exception exception =
        assertThrows(NotFoundException.class, () -> iconService.getIcon(nonExistingKey));

    assertEquals(String.format("Icon not found: %s", nonExistingKey), exception.getMessage());
  }

  @Test
  void shouldFailWhenGettingIconDataOfNonDefaultIcon() {
    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.getDefaultIconResource("madeUpIconKey"));

    assertEquals("No Icon found with key madeUpIconKey.", exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconWithExistingKey() {

    FileResource fileResource = createAndPersistFileResource('A');
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> iconService.addIcon(new Icon(Key, "description", keywords, true, fileResource)));

    assertEquals(String.format("Icon with key %s already exists.", Key), exception.getMessage());
  }

  @Test
  void shouldFailWhenFetchingIconDataWithNonExistentKey() {

    String nonExistingKey = "non-existent-Key";

    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.getDefaultIconResource(nonExistingKey));

    assertEquals(
        String.format("No Icon found with key %s.", nonExistingKey), exception.getMessage());
  }

  private FileResource createAndPersistFileResource(char uniqueChar) {
    byte[] content = "content".getBytes(StandardCharsets.UTF_8);
    String filename = "filename" + uniqueChar;

    HashCode contentMd5 = Hashing.md5().hashBytes(content);
    String contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

    FileResource fileResource =
        new FileResource(
            filename,
            contentType,
            content.length,
            contentMd5.toString(),
            FileResourceDomain.CUSTOM_ICON);
    fileResource.setAssigned(false);
    fileResource.setCreated(new Date());
    fileResource.setAutoFields();

    String fileResourceUid = fileResourceService.asyncSaveFileResource(fileResource, content);
    return fileResourceService.getFileResource(fileResourceUid);
  }

  private void assertIcon(Icon icon) {
    assertEquals(Key, icon.getKey());
    assertEquals("description", icon.getDescription());
    assertEquals(keywords, icon.getKeywords());

    assertThat(icon.getKeywords(), hasSize(3));
    assertThat(fileResource, is(icon.getFileResource()));
    assertThat(currentUser, is(icon.getCreatedBy()));
  }
}

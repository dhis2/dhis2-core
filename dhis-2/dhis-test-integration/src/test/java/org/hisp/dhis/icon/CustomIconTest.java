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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

class CustomIconTest extends TrackerTest {

  @Autowired private FileResourceService fileResourceService;

  @Autowired private CustomIconService iconService;

  @Autowired private UserService _userService;

  private final Set<String> keywords = new HashSet<>();
  private final String Key = "iconKey";
  private final String uid = CodeGenerator.generateUid();

  private CustomIcon customIcon;
  private FileResource fileResource;
  private User currentUser;

  @SneakyThrows
  @Override
  protected void initTest() throws IOException {

    userService = _userService;
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    currentUser = userService.getUserByUsername(currentUsername);
    injectSecurityContextUser(currentUser);

    keywords.addAll(Set.of("k1", "k2", "k3"));
    fileResource = createAndPersistFileResource('A');
    customIcon = new CustomIcon(Key, "description", keywords, true, fileResource);
    customIcon.setUid(uid);
    iconService.addCustomIcon(customIcon);
  }

  @Test
  void shouldGetCustomIconByKey() throws NotFoundException {
    assertCustomIcon(iconService.getCustomIcon(Key));
  }

  @Test
  void shouldGetCustomIconByUid() throws NotFoundException {
    assertCustomIcon(iconService.getCustomIconByUid(uid));
  }

  @Test
  void shouldGetAllKeywordsWhenRequested() {}

  @Test
  void shouldGetIconDataWhenKeyBelongsToDefaultIcon() throws NotFoundException, IOException {}

  @Test
  void shouldSaveCustomIconWithNoKeywords() throws BadRequestException, NotFoundException {
    FileResource fileResource = createAndPersistFileResource('D');

    CustomIcon customIconWithNoKeywords =
        new CustomIcon("iconKey2", "description", null, true, fileResource);

    iconService.addCustomIcon(customIconWithNoKeywords);

    assertNotNull(iconService.getCustomIcon("iconKey"));
  }

  @Test
  void shouldFailWhenSavingCustomIconWithNoKey() {
    FileResource fileResource = createAndPersistFileResource('B');

    CustomIcon customIconWithNullKey =
        new CustomIcon(null, "description", keywords, true, fileResource);
    Exception exception =
        assertThrows(
            BadRequestException.class, () -> iconService.addCustomIcon(customIconWithNullKey));

    assertEquals("CustomIcon key not specified.", exception.getMessage());
  }

  @Test
  void shouldFailWhenCustomIconKeyDoesNotExist() {

    String nonExistingKey = "non-existent-Key";
    Exception exception =
        assertThrows(NotFoundException.class, () -> iconService.getCustomIcon(nonExistingKey));

    assertEquals(String.format("CustomIcon not found: %s", nonExistingKey), exception.getMessage());
  }

  @Test
  void shouldFailWhenGettingIconDataOfNonDefaultIcon() {
    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.getCustomIconResource("madeUpIconKey"));

    assertEquals("No CustomIcon found with key madeUpIconKey.", exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconWithExistingKey() {

    FileResource fileResource = createAndPersistFileResource('A');
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addCustomIcon(
                    new CustomIcon(Key, "description", keywords, true, fileResource)));

    assertEquals(
        String.format("CustomIcon with key %s already exists.", Key), exception.getMessage());
  }

  @Test
  void shouldUpdateLastUpdatedWhenCustomIconIsUpdated() {}

  public FileResource createAndPersistFileResource(char uniqueChar) {
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

  private void assertCustomIcon(CustomIcon customIcon) {
    assertEquals(uid, customIcon.getUid());
    assertEquals(Key, customIcon.getIconKey());
    assertEquals("description", customIcon.getDescription());
    assertEquals(keywords, customIcon.getKeywords());

    assertThat(customIcon.getKeywords(), hasSize(3));
    assertThat(fileResource, is(customIcon.getFileResource()));
    assertThat(currentUser, is(customIcon.getCreatedBy()));
    assertThat(currentUser, is(customIcon.getLastUpdatedBy()));
  }
}

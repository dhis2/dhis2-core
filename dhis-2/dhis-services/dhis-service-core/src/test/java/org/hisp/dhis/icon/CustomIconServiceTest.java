/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.fileresource.FileResourceDomain.CUSTOM_ICON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomIconServiceTest extends DhisConvenienceTest {
  @Mock private CustomIconStore customIconStore;

  @Mock private FileResourceService fileResourceService;
  @Mock private UserService userService;

  @Spy @InjectMocks private DefaultCustomIconService iconService;

  @Test
  void shouldSaveCustomIconWhenIconHasNoDuplicatedKeyAndFileResourceExists()
      throws BadRequestException, NotFoundException {
    String uniqueKey = "key";
    String fileResourceUid = "12345";
    FileResource fileResource = createFileResource('A', "file".getBytes());
    fileResource.setUid(fileResourceUid);
    when(customIconStore.getCustomIconByKey(uniqueKey)).thenReturn(null);
    when(fileResourceService.getFileResource(fileResourceUid, CUSTOM_ICON))
        .thenReturn(Optional.of(new FileResource()));

    User user = new User();
    user.setId(1234);
    user.setUsername("user");
    injectSecurityContext(UserDetails.fromUser(user));

    iconService.addCustomIcon(
        new CustomIcon(uniqueKey, "description", Set.of("keyword1"), true, fileResource));

    verify(customIconStore, times(1)).save(any(CustomIcon.class));
  }

  @Test
  void shouldFailWhenSavingCustomIconWithEmptyKey() {
    String emptyKey = "";

    FileResource fileResource = createFileResource('B', "123".getBytes());
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addCustomIcon(
                    new CustomIcon(
                        emptyKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = "CustomIcon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconWithNonExistentFileResourceId() {
    String iconKey = "default key";
    String fileResourceUid = "12345";
    FileResource fileResource = createFileResource('B', "123".getBytes());
    fileResource.setUid(fileResourceUid);
    when(customIconStore.getCustomIconByKey(iconKey)).thenReturn(null);
    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.empty());

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                iconService.addCustomIcon(
                    new CustomIcon(
                        iconKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = String.format("FileResource %s does not exist", fileResourceUid);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconAndIconWithSameKeyExists() {
    FileResource fileResource = createFileResource('B', "123".getBytes());

    String duplicatedKey = "custom key";
    when(customIconStore.getCustomIconByKey(duplicatedKey))
        .thenReturn(new CustomIcon("key", "description", Set.of(), true, fileResource));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addCustomIcon(
                    new CustomIcon(
                        duplicatedKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = "CustomIcon with key " + duplicatedKey + " already exists.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconWithNoFileResourceId() {
    FileResource fileResource = createFileResource('c', "123".getBytes());

    String iconKey = "key";
    CustomIcon customIcon =
        new CustomIcon(iconKey, "description", Set.of("keyword1"), true, fileResource);
    Exception exception =
        assertThrows(NotFoundException.class, () -> iconService.addCustomIcon(customIcon));

    String expectedMessage = String.format("FileResource %s does not exist", fileResource.getUid());
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldUpdateCustomIconIconWhenKeyPresentAndCustomIconExists()
      throws BadRequestException, NotFoundException {
    String uniqueKey = "key";
    when(customIconStore.getCustomIconByKey(anyString())).thenReturn(new CustomIcon());

    CustomIcon customIcon =
        createCustomIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    iconService.updateCustomIcon(customIcon);

    verify(customIconStore, times(1)).update(any(CustomIcon.class));
  }

  @Test
  void shouldFailWhenUpdatingCustomIconWithoutKey() {

    CustomIcon customIcon =
        createCustomIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    customIcon.setIconKey(null);
    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.updateCustomIcon(customIcon));

    String expectedMessage = "CustomIcon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenUpdatingNonExistingCustomIcon() {
    String key = "key";
    when(customIconStore.getCustomIconByKey(key)).thenReturn(null);

    CustomIcon customIcon =
        createCustomIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    customIcon.setIconKey(key);
    Exception exception =
        assertThrows(NotFoundException.class, () -> iconService.updateCustomIcon(customIcon));

    String expectedMessage = String.format("CustomIcon not found: %s", key);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenUpdatingCustomIconWithoutDescriptionNorKeywords() {
    String uniqueKey = "key";

    CustomIcon customIcon =
        createCustomIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    customIcon.setIconKey(uniqueKey);

    customIcon.setDescription(null);
    customIcon.setKeywords(null);

    when(customIconStore.getCustomIconByKey(uniqueKey)).thenReturn(customIcon);
    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.updateCustomIcon(customIcon));

    String expectedMessage =
        String.format(
            "Can't update icon %s if none of description and keywords are present in the request",
            uniqueKey);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldDeleteIconWhenKeyPresentAndCustomIconExists()
      throws BadRequestException, NotFoundException {

    FileResource fileResource = createFileResource('d', "123".getBytes());

    String uniqueKey = "key";
    when(customIconStore.getCustomIconByKey(uniqueKey))
        .thenReturn(new CustomIcon(uniqueKey, "description", Set.of(), true, fileResource));
    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.of(new FileResource()));

    iconService.deleteCustomIcon(uniqueKey);

    verify(customIconStore, times(1)).delete(any(CustomIcon.class));
  }

  @Test
  void shouldFailWhenDeletingCustomIconWithoutKey() {
    String emptyKey = "";

    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.deleteCustomIcon(emptyKey));

    String expectedMessage = "CustomIcon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenDeletingNonExistingCustomIcon() {
    String key = "key";
    when(customIconStore.getCustomIconByKey(key)).thenReturn(null);

    Exception exception =
        assertThrows(NotFoundException.class, () -> iconService.deleteCustomIcon(key));

    String expectedMessage = String.format("CustomIcon not found: %s", key);
    assertEquals(expectedMessage, exception.getMessage());
  }
}

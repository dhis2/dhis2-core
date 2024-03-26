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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IconServiceTest extends DhisConvenienceTest {
  @Mock private IconStore iconStore;

  @Mock private UserService userService;

  @Mock private FileResourceService fileResourceService;

  @Mock private FileResourceContentStore fileResourceContentStore;

  @Spy @InjectMocks private DefaultIconService iconService;

  /*
  @Test
  void shouldSaveIconWhenIconHasNoDuplicatedKeyAndFileResourceExists() throws Exception {
    String uniqueKey = "key";
    String fileResourceUid = "12345";
    FileResource fileResource = createFileResource('A', "file".getBytes());
    fileResource.setUid(fileResourceUid);
    when(iconStore.getIconByKey(uniqueKey)).thenReturn(null);
    when(fileResourceService.getFileResource(fileResourceUid, ICON))
        .thenReturn(Optional.of(new FileResource()));

    User user = new User();
    user.setId(1234);
    user.setUsername("user");
    injectSecurityContext(UserDetails.fromUser(user));

    when(userService.getUserByUsername(anyString())).thenReturn(user);

    iconService.addIcon(new Icon(uniqueKey, "description", Set.of("keyword1"), true, fileResource));

    verify(iconStore, times(1)).save(any(Icon.class));
  }

  @Test
  void shouldFailWhenSavingIconWithEmptyKey() {
    String emptyKey = "";

    FileResource fileResource = createFileResource('B', "123".getBytes());
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addIcon(
                    new Icon(emptyKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = "Icon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconWithNonExistentFileResourceId() {
    String iconKey = "default-key";
    String fileResourceUid = "12345";
    FileResource fileResource = createFileResource('B', "123".getBytes());
    fileResource.setUid(fileResourceUid);
    when(iconStore.getIconByKey(iconKey)).thenReturn(null);
    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.empty());

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                iconService.addIcon(
                    new Icon(iconKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = String.format("FileResource %s does not exist", fileResourceUid);
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconAndIconWithSameKeyExists() {
    FileResource fileResource = createFileResource('B', "123".getBytes());

    String duplicatedKey = "customkey";
    when(iconStore.getIconByKey(duplicatedKey))
        .thenReturn(new Icon("key", "description", Set.of(), true, fileResource));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addIcon(
                    new Icon(
                        duplicatedKey, "description", Set.of("keyword1"), true, fileResource)));

    String expectedMessage = "Icon with key " + duplicatedKey + " already exists.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingIconWithNoFileResourceId() {
    FileResource fileResource = createFileResource('c', "123".getBytes());

    String iconKey = "key";
    Icon icon = new Icon(iconKey, "description", Set.of("keyword1"), true, fileResource);
    Exception exception = assertThrows(NotFoundException.class, () -> iconService.addIcon(icon));

    String expectedMessage = String.format("FileResource %s does not exist", fileResource.getUid());
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldUpdateIcon() throws BadRequestException, SQLException {

    Icon icon = createIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    iconService.updateIcon(icon);

    verify(iconStore, times(1)).update(any(Icon.class));
  }

  @Test
  void shouldFailWhenUpdatingIconWithoutKey() {

    Icon icon = createIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    icon.setKey(null);
    Exception exception =
        assertThrows(BadRequestException.class, () -> iconService.updateIcon(icon));

    String expectedMessage = "Icon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldDeleteIconWhenKeyPresentAndIconExists() throws BadRequestException, NotFoundException {

    Icon icon = createIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));

    when(iconStore.getIconByKey(anyString())).thenReturn(icon);
    when(fileResourceService.getFileResource(anyString(), any(FileResourceDomain.class)))
        .thenReturn(Optional.of(new FileResource()));

    iconService.deleteIcon(icon.getKey());

    verify(iconStore, times(1)).delete(any(Icon.class));
  }

  @Test
  void shouldFailWhenDeletingIconWithoutKey() {

    Icon iconWithNullKey =
        createIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    iconWithNullKey.setKey(null);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> iconService.deleteIcon(iconWithNullKey.getKey()));

    String expectedMessage = "Icon key not specified.";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFailWhenDeletingNonExistingIcon() {
    Icon nonExistentIcon =
        createIcon('I', Set.of("k1", "k2"), createFileResource('F', "123".getBytes()));
    nonExistentIcon.setKey("non-existent-icon");

    when(iconStore.getIconByKey(anyString())).thenReturn(null);

    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.deleteIcon(nonExistentIcon.getKey()));

    assertEquals("Icon with id non-existent-icon could not be found.", exception.getMessage());
  }
  */

}

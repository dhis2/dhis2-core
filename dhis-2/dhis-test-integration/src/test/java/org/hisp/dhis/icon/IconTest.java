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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertGreaterOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

class IconTest extends TrackerTest {
  @Autowired private FileResourceService fileResourceService;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private IconService iconService;

  private final String[] keywordK = {"k1", "k2", "k3"};
  private final String[] keywordM = {"m1", "m2", "m3"};
  private final String[] keywordN = {"n1", "n2", "n3"};
  private final String[] keywordO = {"o1", "o2", "o3"};
  private final String[] keywordP = {"p1", "p2", "p3"};

  private final String ICON_KEY_1 = "iconKey1";
  private final String ICON_KEY_2 = "iconKey2";
  private final String ICON_KEY_3 = "iconKey3";
  private final String ICON_KEY_4 = "iconKey4";
  private final String ICON_KEY_5 = "iconKey5";

  private final int keywordLength =
      keywordK.length + keywordM.length + keywordN.length + keywordP.length + keywordO.length;

  @SneakyThrows
  @Override
  protected void initTest() throws IOException {
    addCustomIcon(createAndPersistFileResource('A'), ICON_KEY_1, keywordK);
    addCustomIcon(createAndPersistFileResource('B'), ICON_KEY_2, keywordM);
    addCustomIcon(createAndPersistFileResource('C'), ICON_KEY_3, keywordN);
    addCustomIcon(createAndPersistFileResource('D'), ICON_KEY_4, keywordO);
    addCustomIcon(createAndPersistFileResource('E'), ICON_KEY_5, keywordP);
  }

  @Test
  void shouldGetAllPaginatedIconsWhenRequested() {
    Map<String, DefaultIcon> defaultIconMap = getAllDefaultIcons();

    IconCriteria iconCriteria = IconCriteria.of(1, 2, false);

    assertEquals(
        defaultIconMap.size() + 2,
        iconService.getIcons(iconCriteria).size(),
        String.format(
            "Expected to find %d icons, but found %d instead",
            defaultIconMap.size() + 2, iconService.getIcons(iconCriteria).size()));
  }

  @Test
  void shouldGetAllIconsWhenRequested() {
    Map<String, DefaultIcon> defaultIconMap = getAllDefaultIcons();

    IconCriteria iconCriteria = IconCriteria.of(0, 0, true);

    assertEquals(
        defaultIconMap.size() + 5,
        iconService.getIcons(iconCriteria).size(),
        String.format(
            "Expected to find %d icons, but found %d instead",
            defaultIconMap.size() + 5, iconService.getIcons(iconCriteria).size()));
  }

  @Test
  void shouldGetDefaultIconWhenKeyBelongsToDefaultIcon() throws NotFoundException {
    String defaultIconKey = getAllDefaultIcons().keySet().stream().findAny().orElse(null);

    Icon icon = iconService.getIcon(defaultIconKey);

    assertEquals(defaultIconKey, icon.getKey());
  }

  @Test
  void shouldGetAllKeywordsWhenRequested() {
    Set<String> keywordList =
        getAllDefaultIcons().values().stream()
            .map(Icon::getKeywords)
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());

    IconCriteria iconCriteria = IconCriteria.of(0, 0, true);

    assertEquals(
        keywordList.size() + keywordLength,
        iconService.getKeywords().size(),
        String.format(
            "Expected to find %d icons, but found %d instead",
            keywordList.size() + keywordLength, iconService.getIcons(iconCriteria).size()));
  }

  @Test
  void shouldGetAllIconsFilteredByKeywordWhenRequested()
      throws BadRequestException, NotFoundException {
    Optional<DefaultIcon> defaultIcon =
        getAllDefaultIcons().values().stream().filter(si -> si.getKeywords().length > 0).findAny();

    if (defaultIcon.isEmpty()) {
      return;
    }

    String keyword = defaultIcon.get().getKeywords()[0];
    FileResource fileResourceD = createAndPersistFileResource('D');
    iconService.addCustomIcon(
        new CustomIcon(
            "iconKeyD",
            "description",
            new String[] {keyword},
            fileResourceD.getUid(),
            currentUserService.getCurrentUser().getUid()));

    assertGreaterOrEqual(2, iconService.getIcons(new String[] {keyword}).size());
  }

  @Test
  void shouldGetCustomIconsFilteredByKeywordWhenRequested()
      throws BadRequestException, NotFoundException {

    FileResource fileResourceB = createAndPersistFileResource('B');
    CustomIcon iconB =
        new CustomIcon(
            "iconKeyB",
            "description",
            new String[] {"k4", "k5", "k6"},
            fileResourceB.getUid(),
            currentUserService.getCurrentUser().getUid());
    iconService.addCustomIcon(iconB);
    FileResource fileResourceC = createAndPersistFileResource('C');
    CustomIcon iconC =
        new CustomIcon(
            "iconKeyC",
            "description",
            new String[] {"k6", "k7", "k8"},
            fileResourceC.getUid(),
            currentUserService.getCurrentUser().getUid());
    iconService.addCustomIcon(iconC);

    assertContainsOnly(List.of(iconB), iconService.getIcons(new String[] {"k4", "k5", "k6"}));
    assertContainsOnly(List.of(iconC), iconService.getIcons(new String[] {"k6", "k7"}));
    assertContainsOnly(List.of(iconB, iconC), iconService.getIcons(new String[] {"k6"}));
  }

  @Test
  void shouldGetIconDataWhenKeyBelongsToDefaultIcon() throws NotFoundException, IOException {
    String defaultIconKey = getAllDefaultIcons().keySet().stream().findAny().orElse(null);

    Resource iconResource = iconService.getDefaultIconResource(defaultIconKey);

    assertNotNull(iconResource.getURL());
  }

  @Test
  void shouldFailWhenGettingIconDataOfNonDefaultIcon() {
    Exception exception =
        assertThrows(
            NotFoundException.class, () -> iconService.getDefaultIconResource("madeUpIconKey"));

    assertEquals("No default icon found with key madeUpIconKey.", exception.getMessage());
  }

  @Test
  void shouldFailWhenSavingCustomIconAndDefaultIconWithSameKeyExists() {
    Map<String, DefaultIcon> defaultIconMap = getAllDefaultIcons();
    String defaultIconKey = defaultIconMap.values().iterator().next().getKey();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                iconService.addCustomIcon(
                    new CustomIcon(
                        defaultIconKey,
                        "description",
                        new String[] {"keyword1"},
                        "fileResourceUid",
                        "userUid")));

    String expectedMessage = String.format("Icon with key %s already exists.", defaultIconKey);
    assertEquals(expectedMessage, exception.getMessage());
  }

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

  private Map<String, DefaultIcon> getAllDefaultIcons() {
    return Arrays.stream(DefaultIcon.Icons.values())
        .map(DefaultIcon.Icons::getVariants)
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(DefaultIcon::getKey, Function.identity()));
  }

  private void addCustomIcon(FileResource fileResource, String key, String[] keywords)
      throws BadRequestException, NotFoundException {
    iconService.addCustomIcon(
        new CustomIcon(
            key,
            "description",
            keywords,
            fileResource.getUid(),
            currentUserService.getCurrentUser().getUid()));
  }
}

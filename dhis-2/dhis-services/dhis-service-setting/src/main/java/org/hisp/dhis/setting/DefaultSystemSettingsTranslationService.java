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
package org.hisp.dhis.setting;

import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;

import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan Bernitt
 * @since 2.42
 */
@Service
@RequiredArgsConstructor
public class DefaultSystemSettingsTranslationService implements SystemSettingsTranslationService {

  /** The namespace used to store settings translations in the datastore. */
  private static final String NS = "settings-translations";

  private final DatastoreService datastore;

  @PostConstruct
  private void init() {
    // Note: this uses a protection to prevent direct access
    // but from within this service it datastore is read-accessed as SystemUser
    datastore.addProtection(
        new DatastoreNamespaceProtection(NS, RESTRICTED, Authorities.F_SYSTEM_SETTING.name()));
  }

  @Override
  @Transactional
  public void putSystemSettingTranslation(
      @Nonnull String key, @Nonnull String locale, String translation)
      throws ForbiddenException, BadRequestException {
    if (!SystemSettings.isTranslatable(key))
      throw new BadRequestException("Not translatable: " + key);
    DatastoreEntry e = datastore.getEntry(NS, key);
    if (translation == null || translation.isEmpty()) {
      if (e == null) return;
      JsonMixed translations = JsonMixed.of(e.getValue());
      if (!translations.has(locale)) return;
      if (translations.size() == 1) {
        datastore.deleteEntry(e);
        return;
      }
      e.setValue(translations.node().removeMembers(Set.of(locale)).getDeclaration());
      datastore.saveOrUpdateEntry(e);
    } else {
      if (e == null) {
        datastore.saveOrUpdateEntry(
            new DatastoreEntry(
                NS, key, Json.object(obj -> obj.addString(locale, translation)).toJson()));
      } else {
        JsonMixed translations = JsonMixed.of(e.getValue());
        e.setValue(
            translations
                .node()
                .addMembers(obj -> obj.addString(locale, translation))
                .getDeclaration());
        datastore.saveOrUpdateEntry(e);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> getSystemSettingTranslation(@Nonnull String key, @Nonnull String locale) {
    DatastoreEntry entry = getEntryAsSystemUser(key);
    if (entry == null) return Optional.empty();
    JsonMap<JsonString> translations = JsonMixed.of(entry.getValue()).asMap(JsonString.class);
    if (!translations.has(locale)) return Optional.empty();
    return Optional.of(translations.get(locale).string());
  }

  @CheckForNull
  private DatastoreEntry getEntryAsSystemUser(@Nonnull String key) {
    try {
      return datastore.getEntry(NS, key, new SystemUser());
    } catch (ForbiddenException e) {
      // this should never happen as we use SuperUser
      // but, we really don't want to propagate the exception
      throw new RuntimeException(e);
    }
  }
}

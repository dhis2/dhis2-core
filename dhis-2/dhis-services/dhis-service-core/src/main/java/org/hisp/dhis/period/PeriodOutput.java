/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.period;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.translation.Translation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PeriodOutput {

  private static final JsonBuilder.PrettyPrint MINIMIZED =
      new JsonBuilder.PrettyPrint(0, 0, false, false, true);

  static void toJson(@Nonnull PeriodTypes.Output types, @Nonnull OutputStream json) {
    try (json) {
      JsonBuilder.streamObject(
          MINIMIZED,
          json,
          root -> {
            root.addString("locale", types.locale().toString());
            root.addArray(
                "periodTypes",
                arr -> {
                  for (PeriodTypes.PeriodTypeEntry.Output e : types.entries()) {
                    arr.addObject(entry -> addEntry(entry, e));
                  }
                });
          });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static void toJsonArray(@Nonnull PeriodTypes.Output types, @Nonnull OutputStream json) {
    try (json) {
      JsonBuilder.streamArray(
          MINIMIZED,
          json,
          arr -> {
            for (PeriodTypes.PeriodTypeEntry.Output e : types.entries()) {
              arr.addObject(entry -> addEntry(entry, e));
            }
          });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static void toJson(
      @Nonnull PeriodTypes.PeriodTypeEntry.Output entry, @Nonnull OutputStream json) {
    try (json) {
      JsonBuilder.streamObject(MINIMIZED, json, eObj -> addEntry(eObj, entry));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void addEntry(
      JsonBuilder.JsonObjectBuilder eObj, PeriodTypes.PeriodTypeEntry.Output entry) {
    PeriodTypeEnum type = entry.type();
    if (type != null) eObj.addString("name", type.getName());
    eObj.addString("isoDuration", entry.isoDuration());
    eObj.addString("isoFormat", entry.isoFormat());
    eObj.addNumber("frequencyOrder", entry.frequencyOrder());
    eObj.addString("defaultName", entry.defaultName());
    eObj.addString("label", entry.label());
    eObj.addString("displayLabel", entry.displayLabel());
    eObj.addString("displayName", entry.displayName());
    addTranslations(eObj, entry.translations());
    Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = entry.relativePeriods();
    if (relativePeriods != null && !relativePeriods.isEmpty()) {
      eObj.addObject(
          "relativePeriods",
          rpMap -> {
            for (Map.Entry<RelativePeriodEnum, PeriodTypes.Labels> rp :
                relativePeriods.entrySet()) {
              rpMap.addObject(
                  rp.getKey().name(),
                  rpObj -> {
                    PeriodTypes.Labels val = rp.getValue();
                    rpObj.addString("defaultName", val.defaultName());
                    rpObj.addString("label", val.label());
                    rpObj.addString("displayLabel", val.displayLabel());
                    rpObj.addString("displayName", val.displayName());
                    addTranslations(rpObj, val.translations());
                  });
            }
          });
    }
  }

  private static void addTranslations(
      JsonBuilder.JsonObjectBuilder entry, List<Translation> translations) {
    if (translations == null || translations.isEmpty()) return;
    entry.addArray(
        "translations",
        arr -> {
          for (Translation t : translations) {
            arr.addObject(
                translation ->
                    translation
                        .addString("locale", t.getLocale().toString())
                        .addString("property", t.getProperty())
                        .addString("value", t.getValue()));
          }
        });
  }
}

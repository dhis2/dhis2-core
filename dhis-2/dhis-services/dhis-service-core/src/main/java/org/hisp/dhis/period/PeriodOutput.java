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
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.translation.Translation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PeriodOutput {

  private static final JsonBuilder.PrettyPrint MINIMIZED =
      new JsonBuilder.PrettyPrint(0, 0, false, false, true);

  static void toJson(@Nonnull PeriodTypes.Output types, @Nonnull OutputStream json) {
    Fields fields = types.fields();
    try (json) {
      JsonBuilder.streamObject(
          MINIMIZED,
          json,
          root -> {
            root.addString("locale", types.locale().toString());
            root.addArray(
                "periodTypes",
                arr -> {
                  for (PeriodTypes.PeriodTypeEntry e : types.entries()) {
                    arr.addObject(entry -> toJson(entry, e, fields));
                  }
                });
          });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static void toJson(
      @Nonnull PeriodTypes.PeriodTypeEntry e, @Nonnull Fields fields, @Nonnull OutputStream json) {
    try (json) {
      JsonBuilder.streamObject(MINIMIZED, json, entry -> toJson(entry, e, fields));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void toJson(
      JsonBuilder.JsonObjectBuilder entry, PeriodTypes.PeriodTypeEntry e, Fields fields) {
    boolean hasAll = fields.contains(":all");
    entry.addString("name", e.name());
    if (hasAll || fields.contains("defaultName")) entry.addString("defaultName", e.defaultName());
    if (hasAll || fields.contains("isoDuration")) entry.addString("isoDuration", e.isoDuration());
    if (hasAll || fields.contains("isoFormat")) entry.addString("isoFormat", e.isoFormat());
    if (hasAll || fields.contains("frequencyOrder"))
      entry.addNumber("frequencyOrder", e.frequencyOrder());
    if (hasAll || fields.contains("label")) entry.addString("label", e.label());
    if (hasAll || fields.contains("displayLabel"))
      entry.addString("displayLabel", e.displayLabel());
    if (hasAll || fields.contains("displayName")) entry.addString("displayName", e.displayName());
    if ((hasAll || fields.contains("translations")) && !e.translations().isEmpty())
      entry.addArray(
          "translations",
          translations -> {
            for (Translation t : e.translations()) {
              translations.addObject(
                  translation ->
                      translation
                          .addString("locale", t.getLocale().toString())
                          .addString("property", t.getProperty())
                          .addString("value", t.getValue()));
            }
          });
  }
}

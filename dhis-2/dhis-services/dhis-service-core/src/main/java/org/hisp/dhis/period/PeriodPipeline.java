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

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.setting.UserSettings;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeriodPipeline {

  private final PeriodService service;

  @IndirectTransactional
  public void exportAllAsJson(
      @CheckForNull Locale locale, @Nonnull Fields fields, @Nonnull Supplier<OutputStream> out) {
    if (locale == null) locale = UserSettings.getCurrentSettings().getUserDbLocale();
    PeriodTypes types = service.getAllPeriodTypes(locale);
    PeriodOutput.toJson(new PeriodTypes.Output(locale, types.entries(), fields), out.get());
  }

  @IndirectTransactional
  public void exportAsJson(
      @Nonnull Set<PeriodTypeEnum> types,
      @CheckForNull Locale locale,
      @Nonnull Fields fields,
      @Nonnull Supplier<OutputStream> out) {
    if (locale == null) locale = UserSettings.getCurrentSettings().getUserDbLocale();
    PeriodTypes res = service.getAllPeriodTypes(locale);
    List<PeriodTypes.PeriodTypeEntry> entries =
        res.entries().stream().filter(pt -> types.contains(pt.type())).toList();
    PeriodOutput.toJson(new PeriodTypes.Output(locale, entries, fields), out.get());
  }

  @IndirectTransactional
  public void exportAsJson(
      @Nonnull PeriodTypeEnum type,
      @CheckForNull Locale locale,
      @Nonnull Fields fields,
      @Nonnull Supplier<OutputStream> out)
      throws NotFoundException {
    if (locale == null) locale = UserSettings.getCurrentSettings().getUserDbLocale();
    PeriodTypes.PeriodTypeEntry entry =
        service.getAllPeriodTypes(locale).entries().stream()
            .filter(pt -> type == pt.type())
            .findFirst()
            .orElse(null);
    if (entry == null) throw new NotFoundException(PeriodType.class, type.getName());
    PeriodOutput.toJson(entry, fields, out.get());
  }
}

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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1056;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1057;

import java.time.Instant;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component("org.hisp.dhis.tracker.imports.validation.validator.event.CategoryOptValidator")
@Slf4j
@RequiredArgsConstructor
class CategoryOptValidator implements Validator<Event> {
  private final I18nManager i18nManager;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    Program program = bundle.getPreheat().getProgram(event.getProgram());

    TrackerPreheat preheat = bundle.getPreheat();
    CategoryOptionCombo categoryOptionCombo;
    if (program.getCategoryCombo().isDefault()) {
      categoryOptionCombo = preheat.getDefault(CategoryOptionCombo.class);
    } else {
      categoryOptionCombo = preheat.getCategoryOptionCombo(event.getAttributeOptionCombo());
    }
    Date eventDate;
    if (event instanceof TrackerEvent trackerEvent) {
      eventDate =
          DateUtils.fromInstant(
              ObjectUtils.firstNonNull(
                  trackerEvent.getOccurredAt(), trackerEvent.getScheduledAt(), Instant.now()));
    } else {
      eventDate =
          DateUtils.fromInstant(ObjectUtils.firstNonNull(event.getOccurredAt(), Instant.now()));
    }
    I18nFormat i18nFormat = i18nManager.getI18nFormat();

    for (CategoryOption option : categoryOptionCombo.getCategoryOptions()) {
      if (option.getStartDate() != null && eventDate.compareTo(option.getStartDate()) < 0) {
        reporter.addError(
            event,
            E1056,
            i18nFormat.formatDate(eventDate),
            i18nFormat.formatDate(option.getStartDate()),
            option.getName());
      }

      if (option.getEndDate() != null
          && eventDate.compareTo(option.getAdjustedEndDate(program)) > 0) {
        reporter.addError(
            event,
            E1057,
            i18nFormat.formatDate(eventDate),
            i18nFormat.formatDate(option.getAdjustedEndDate(program)),
            option.getName(),
            program.getName());
      }
    }
  }
}

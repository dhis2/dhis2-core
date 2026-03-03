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
package org.hisp.dhis.datavalue;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDateTime;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Takes care of creating {@link org.hisp.dhis.audit.Audit} entries as a side effect of data entry.
 * To not block or delay the caller thread the public method is {@link Async}.
 *
 * @author Jan Bernitt
 */
@Service
@RequiredArgsConstructor
class DataEntryAuditService {

  private final AuditManager manager;
  private final AuditMatrix configuration;

  @Async
  @IndirectTransactional
  public void auditUpsert(DataEntryGroup group, DataEntrySummary summary, String username) {
    if (summary.succeeded() == 0) return;
    if (!configuration.isEnabled(AuditScope.AGGREGATE, AuditType.UPDATE)) return;
    LocalDateTime now = LocalDateTime.now();
    String partial =
        summary.attempted() == summary.succeeded()
            ? null
            : "%d/%d".formatted(summary.succeeded(), summary.attempted());
    Set<Integer> errors =
        summary.errors().stream().map(error -> error.value().index()).collect(toSet());
    for (DataEntryValue value : group.values()) {
      if (!errors.contains(value.index())) {
        AuditAttributes attributes = createAttributes(value);
        if (partial != null) attributes.put("uncertainty", partial);
        manager.send(
            Audit.builder()
                .auditType(AuditType.UPDATE)
                .auditScope(AuditScope.AGGREGATE)
                .createdAt(now)
                .createdBy(username)
                .attributes(attributes)
                .data("{}")
                .build());
      }
    }
  }

  @Nonnull
  private static AuditAttributes createAttributes(DataEntryValue value) {
    AuditAttributes attributes = new AuditAttributes();
    attributes.put("dataElement", value.dataElement().getValue());
    attributes.put("period", value.period().getIsoDate());
    attributes.put("source", value.orgUnit().getValue());
    UID coc = value.categoryOptionCombo();
    if (coc != null) attributes.put("categoryOptionCombo", coc.getValue());
    UID aoc = value.attributeOptionCombo();
    if (aoc != null) attributes.put("attributeOptionCombo", aoc.getValue());
    attributes.put("deleted", value.deleted());
    attributes.put("value", value.value());
    return attributes;
  }
}

/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.feedback;

import static java.util.stream.Collectors.joining;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

/**
 * @param entered number of rows (values) that were decoded from the user input
 * @param attempted number of rows (values) that were attempted to import (entered - errors.size())
 * @param succeeded number of rows (values) affected by the import (ideally same as upserted)
 */
public record DataEntrySummary(
    int entered, int attempted, int succeeded, @Nonnull List<DataEntryError> errors) {

  public static DataEntryError error(
      @Nonnull DataEntryValue value, @Nonnull ErrorCode code, Object... args) {
    return new DataEntryError(value, code, List.of(args));
  }

  public record DataEntryError(
      @Nonnull DataEntryValue value, @Nonnull ErrorCode code, @Nonnull List<Object> args) {}

  public int ignored() {
    return attempted - succeeded;
  }

  public DataEntrySummary add(DataEntrySummary other) {
    List<DataEntryError> errors = new ArrayList<>(this.errors);
    errors.addAll(other.errors);
    return new DataEntrySummary(
        entered + other.entered, attempted + other.attempted, succeeded + other.succeeded, errors);
  }

  /** Adapter to the extensive legacy summary */
  public ImportSummary toImportSummary() {
    ImportSummary summary = new ImportSummary();
    // any value processed successfully in DataEntrySummary
    // maps to "imported", values attempted but failed become "ignored"
    // "updated" and "deleted" are not used as we cannot tell the difference
    int ignored = ignored();
    summary.setImportCount(new ImportCount(succeeded(), 0, ignored, 0));
    for (DataEntryError error : errors()) {
      summary.addRejected(error.value().index());
      summary.addConflict(toConflict(error));
    }
    ImportStatus status = ImportStatus.SUCCESS;
    if (ignored > 0) status = ImportStatus.WARNING;
    if (!errors.isEmpty()) status = ImportStatus.ERROR;
    summary.setStatus(status);
    return summary;
  }

  private static ImportConflict toConflict(DataEntryError error) {
    return toConflict(IntStream.of(error.value().index()), error.code(), error.args());
  }

  public static ImportConflict toConflict(IntStream indexes, ErrorCode code, Object... args) {
    String message = MessageFormat.format(code.getMessage(), args);
    String key = Stream.of(args).map(String::valueOf).collect(joining("-"));
    return new ImportConflict(null, Map.of("args", key), message, code, null, indexes.toArray());
  }
}

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

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

/**
 * @param attempted number of rows that were attempted to import
 * @param succeeded number of rows affected by the import (ideally same as upserted)
 */
public record ImportResult(int attempted, int succeeded, @Nonnull List<ImportError> errors) {

  public static ImportError error(int index, ErrorCode code, Object... args) {
    return new ImportError(index, code, List.of(args));
  }

  public record ImportError(int index, @Nonnull ErrorCode code, @Nonnull List<Object> args) {}

  /** Adapter to the extensive legacy summary */
  public ImportSummary toImportSummary() {
    ImportSummary summary = new ImportSummary();
    summary.setImportCount(new ImportCount(succeeded(), 0, attempted() - succeeded(), 0));
    for (ImportResult.ImportError error : errors()) {
      summary.addRejected(error.index());
      summary.addConflict(
          ImportConflict.createUniqueConflict(error.index(), error.code(), error.args()));
    }
    return summary;
  }
}

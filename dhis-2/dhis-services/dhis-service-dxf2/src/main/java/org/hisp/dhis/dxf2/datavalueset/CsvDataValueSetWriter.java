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
package org.hisp.dhis.dxf2.datavalueset;

import com.csvreader.CsvWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.AllArgsConstructor;

/**
 * Write {@link DataValueSet}s as CSV data.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
final class CsvDataValueSetWriter implements DataValueSetWriter {

  private static final String[] HEADER_ROW = {
    "dataelement",
    "period",
    "orgunit",
    "categoryoptioncombo",
    "attributeoptioncombo",
    "value",
    "storedby",
    "lastupdated",
    "comment",
    "followup",
    "deleted"
  };

  private final CsvWriter writer;

  @Override
  public void writeHeader() {
    appendRow(HEADER_ROW);
  }

  @Override
  public void writeHeader(
      String dataSetId, String completeDate, String isoPeriod, String orgUnitId) {
    appendRow(HEADER_ROW);
  }

  @Override
  public void writeValue(DataValueEntry entry) {
    appendRow(
        new String[] {
          entry.getDataElement(),
          entry.getPeriod(),
          entry.getOrgUnit(),
          entry.getCategoryOptionCombo(),
          entry.getAttributeOptionCombo(),
          entry.getValue(),
          entry.getStoredBy(),
          entry.getLastUpdated(),
          entry.getComment(),
          String.valueOf(entry.getFollowup()),
          String.valueOf(entry.getDeleted())
        });
  }

  @Override
  public void close() {
    writer.close();
  }

  private void appendRow(String[] row) {
    try {
      writer.writeRecord(row);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write CSV data", ex);
    }
  }
}

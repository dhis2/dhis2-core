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

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.common.ImportOptions;

/**
 * Reads {@link DataValueSet} from CSV input.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
final class CsvDataValueSetReader implements DataValueSetReader, DataValueEntry {
  private final CsvReader reader;

  private final ImportOptions importOptions;

  @Override
  public DataValueSet readHeader() {
    if (importOptions == null || importOptions.isFirstRowIsHeader()) {
      readNext(); // Ignore the first row: assume header row
    }
    return new DataValueSet();
  }

  @Override
  public DataValueEntry readNext() {
    try {
      return reader.readRecord() ? this : null;
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to read record", ex);
    }
  }

  /*
   * When used as DataValueEntry
   */

  @Override
  public void close() {
    reader.close();
  }

  @Override
  public String getDataElement() {
    return getString(0);
  }

  @Override
  public String getPeriod() {
    return getString(1);
  }

  @Override
  public String getOrgUnit() {
    return getString(2);
  }

  @Override
  public String getCategoryOptionCombo() {
    return getString(3);
  }

  @Override
  public String getAttributeOptionCombo() {
    return getString(4);
  }

  @Override
  public String getValue() {
    return getString(5);
  }

  @Override
  public String getStoredBy() {
    return getString(6);
  }

  @Override
  public String getCreated() {
    return null;
  }

  @Override
  public String getLastUpdated() {
    return getString(7);
  }

  @Override
  public String getComment() {
    return getString(8);
  }

  @Override
  public boolean getFollowup() {
    return Boolean.parseBoolean(getString(9));
  }

  @Override
  public Boolean getDeleted() {
    return Boolean.valueOf(getString(10));
  }

  private String getString(int index) {
    try {
      return reader.get(index);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}

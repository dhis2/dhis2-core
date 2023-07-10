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
package org.hisp.dhis.helpers.file;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.hisp.dhis.actions.IdGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class CsvFileReader implements org.hisp.dhis.helpers.file.FileReader {
  private List<String[]> csvTable;

  private CSVReader reader;

  public CsvFileReader(File file) throws IOException {
    reader = new CSVReader(new FileReader(file));
    csvTable = reader.readAll();
  }

  @Override
  public org.hisp.dhis.helpers.file.FileReader read(File file) throws IOException {
    return new CsvFileReader(file);
  }

  @Override
  public org.hisp.dhis.helpers.file.FileReader replacePropertyValuesWithIds(String propertyName) {
    return replacePropertyValuesWith(propertyName, "uniqueid");
  }

  @Override
  public org.hisp.dhis.helpers.file.FileReader replacePropertyValuesWith(
      String propertyName, String replacedValue) {
    int columnIndex = Arrays.asList(csvTable.get(0)).indexOf(propertyName);

    String lastColumnOriginalValue = "";
    String lastColumnReplacedValue = "";
    for (String[] row : csvTable) {
      if (row[columnIndex].equals(propertyName)) {
        continue;
      }
      if (row[columnIndex].equals(lastColumnOriginalValue)) {
        row[columnIndex] = lastColumnReplacedValue;
        continue;
      }

      lastColumnOriginalValue = row[columnIndex];

      lastColumnReplacedValue = replacedValue;

      if (replacedValue.equalsIgnoreCase("uniqueid")) {
        lastColumnReplacedValue = new IdGenerator().generateUniqueId();
      }

      row[columnIndex] = lastColumnReplacedValue;
    }

    return this;
  }

  @Override
  public org.hisp.dhis.helpers.file.FileReader replacePropertyValuesRecursivelyWith(
      String propertyName, String replacedValue) {
    return null;
  }

  @Override
  public org.hisp.dhis.helpers.file.FileReader replace(Function<Object, Object> function) {
    return null;
  }

  public String get() {
    StringWriter writer = new StringWriter();
    new CSVWriterBuilder(writer).build().writeAll(csvTable);

    return writer.toString();
  }
}

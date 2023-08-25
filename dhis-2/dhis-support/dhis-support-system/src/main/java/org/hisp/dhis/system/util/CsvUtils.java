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
package org.hisp.dhis.system.util;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Lars Helge Overland
 */
public class CsvUtils {
  public static final char DELIMITER = ',';

  /**
   * Returns a {@link CsvReader} using the UTF-8 char set.
   *
   * @param in the {@link InputStream}.
   * @return a {@link CsvReader}.
   */
  public static CsvReader getReader(InputStream in) {
    return new CsvReader(in, StandardCharsets.UTF_8);
  }

  /**
   * Returns the CSV file represented by the given file path as a list of string arrays. The file
   * must exist on the class path.
   *
   * @param filePath the file path on the class path.
   * @param ignoreFirstRow whether to ignore the first row.
   * @return a list of string arrays.
   * @throws IOException
   */
  public static List<String[]> readCsvAsListFromClasspath(String filePath, boolean ignoreFirstRow)
      throws IOException {
    InputStream in = new ClassPathResource(filePath).getInputStream();
    return readCsvAsList(in, ignoreFirstRow);
  }

  /**
   * Returns the CSV file represented by the given input stream as a list of string arrays.
   *
   * @param in the {@link InputStream} representing the CSV file.
   * @param ignoreFirstRow whether to ignore the first row.
   * @return a list of string arrays.
   * @throws IOException
   */
  public static List<String[]> readCsvAsList(InputStream in, boolean ignoreFirstRow)
      throws IOException {
    CsvReader reader = getReader(in);

    if (ignoreFirstRow) {
      reader.readRecord();
    }

    List<String[]> lines = new ArrayList<>();

    while (reader.readRecord()) {
      lines.add(reader.getValues());
    }

    return lines;
  }

  /**
   * Returns a {@link CsvWriter} using the UTF-8 char set.
   *
   * @param writer the {@link Writer}.
   * @return a {@link CsvWriter}.
   */
  public static CsvWriter getWriter(Writer writer) {
    return new CsvWriter(writer, DELIMITER);
  }
}

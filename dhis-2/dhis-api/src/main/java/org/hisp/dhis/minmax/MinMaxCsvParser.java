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
package org.hisp.dhis.minmax;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;

public class MinMaxCsvParser {
  private MinMaxCsvParser() {}

  /**
   * Parses a CSV file containing min/max values for data elements.
   *
   * @param inputStream the input stream of the CSV file
   * @return a list of MinMaxValueDto objects
   * @throws BadRequestException if an error occurs while reading the file
   */
  public static List<MinMaxValueDto> parse(InputStream inputStream) throws BadRequestException {
    List<MinMaxValueDto> result = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String header = reader.readLine();
      if (header == null || header.isEmpty()) {
        return result;
      }

      String line;
      int rowNum = 1;
      while ((line = reader.readLine()) != null) {
        rowNum++;
        String[] fields = line.split(",", 6);
        if (fields.length < 5) {
          continue;
        }

        MinMaxValueDto dto = new MinMaxValueDto();
        try {
          dto.setDataElement(trimToEmpty(fields[0]));
          dto.setOrgUnit(trimToEmpty(fields[1]));
          dto.setCategoryOptionCombo(trimToEmpty(fields[2]));
          dto.setMinValue(Integer.parseInt(trimToEmpty(fields[3])));
          dto.setMaxValue(Integer.parseInt(trimToEmpty(fields[4])));
          if (fields.length > 5 && !trimToEmpty(fields[5]).isEmpty()) {
            dto.setGenerated(Boolean.parseBoolean(trimToEmpty(fields[5])));
          }

          result.add(dto);
        } catch (NumberFormatException e) {
          throw new IOException(
              String.format("Invalid number format at row %d: %s", rowNum, e.getMessage()), e);
        }
      }
    } catch (IOException e) {
      throw new BadRequestException(ErrorCode.E7800, e);
    }
    return result;
  }
}

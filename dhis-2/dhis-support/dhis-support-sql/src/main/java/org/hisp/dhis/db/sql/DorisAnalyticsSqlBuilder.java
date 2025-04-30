/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import java.time.LocalDateTime;
import org.apache.commons.lang3.StringUtils;

public class DorisAnalyticsSqlBuilder extends DorisSqlBuilder implements AnalyticsSqlBuilder {

  public DorisAnalyticsSqlBuilder(String catalog, String driverFilename) {
    super(catalog, driverFilename);
  }

  @Override
  public String getEventDataValues() {
    return "ev.eventdatavalues";
  }

  @Override
  public String renderTimestamp(String timestampAsString) {
    if (StringUtils.isBlank(timestampAsString)) return null;
    LocalDateTime dateTime = LocalDateTime.parse(timestampAsString);
    String formattedDate = dateTime.format(TIMESTAMP_FORMATTER);

    // Find the position of the decimal point
    int decimalPoint = formattedDate.lastIndexOf('.');
    if (decimalPoint != -1) {
      // Remove trailing zeros after decimal point
      String millisPart = formattedDate.substring(decimalPoint + 1);
      millisPart = millisPart.replaceAll("0+$", ""); // Remove all trailing zeros

      // If all digits were zeros, use "0" instead of empty string
      if (millisPart.isEmpty()) {
        millisPart = "0";
      }

      formattedDate = formattedDate.substring(0, decimalPoint + 1) + millisPart;
    }

    return formattedDate;
  }
}

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
package org.hisp.dhis.webapi.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtils {

  /**
   * Extracts identifiers from a filter string.
   *
   * @param filter The filter string in format "OPERATION:IDENTIFIER" or "OPERATION:ID1,ID2,..."
   * @return List of extracted identifiers
   */
  public static List<String> fromFilter(String filter) {
    List<String> result = new ArrayList<>();

    // Return empty list for null or empty input
    if (filter == null || filter.isEmpty()) {
      return result;
    }

    // Check if the filter contains an operation (contains a colon)
    if (filter.contains(":")) {
      // Split the filter into operation and identifiers
      String[] parts = filter.split(":", 2);
      String operation = parts[0];
      String identifiersPart = parts[1];

      // Handle operations
      if ("IN".equals(operation)) {
        // For IN operation, split by comma and add each identifier
        String[] identifiers = identifiersPart.split(";");
        Collections.addAll(result, identifiers);
      } else {
        // For other operations (like EQ), add the identifier as is
        result.add(identifiersPart);
      }
    } else {
      // If no operation is specified, add the whole string as an identifier
      result.add(filter);
    }

    return result;
  }
}

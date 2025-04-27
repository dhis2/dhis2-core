/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.apache.commons.lang3.StringUtils.trimToNull;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jason P. Pickering
 */
@Slf4j
public class MinMaxDataElementUtils {

  private MinMaxDataElementUtils() {}

  public static void validateDto(MinMaxValueDto dto) {

    String dataElement = trimToNull(dto.getDataElement());
    String orgUnit = trimToNull(dto.getOrgUnit());
    String coc = trimToNull(dto.getCategoryOptionCombo());
    Integer min = dto.getMinValue();
    Integer max = dto.getMaxValue();

    if (dataElement == null || orgUnit == null || coc == null || min == null || max == null) {
      throw new MinMaxImportException("Missing required field(s) in: " + formatDtoInfo(dto));
    }

    if (min >= max) {
      throw new MinMaxImportException(
          "Min value is greater than or equal to Max value for: " + formatDtoInfo(dto));
    }
  }

  public static String formatDtoInfo(MinMaxValueDto dto) {
    return String.format(
        "dataElement=%s, orgUnit=%s, categoryOptionCombo=%s, min=%s, max=%s",
        dto.getDataElement(),
        dto.getOrgUnit(),
        dto.getCategoryOptionCombo(),
        dto.getMinValue(),
        dto.getMaxValue());
  }
}

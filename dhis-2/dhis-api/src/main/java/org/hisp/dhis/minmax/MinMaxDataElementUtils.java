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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * @author Jason P. Pickering
 */
@Slf4j
public class MinMaxDataElementUtils {

  private MinMaxDataElementUtils() {}

  public static void validateRequiredFields(MinMaxValueDto dto) throws BadRequestException {

    if (dto.getDataElement() == null
        || dto.getOrgUnit() == null
        || dto.getCategoryOptionCombo() == null) {
      throw new BadRequestException(ErrorCode.E7801, formatDtoInfo(dto));
    }
  }

  public static void validateMinMaxValues(MinMaxValueDto dto) throws BadRequestException {
    if (dto.getMinValue() == null || dto.getMaxValue() == null) {
      throw new BadRequestException(ErrorCode.E7801, formatDtoInfo(dto));
    }

    if (dto.getMinValue() >= dto.getMaxValue()) {
      throw new BadRequestException(ErrorCode.E7804, formatDtoInfo(dto));
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

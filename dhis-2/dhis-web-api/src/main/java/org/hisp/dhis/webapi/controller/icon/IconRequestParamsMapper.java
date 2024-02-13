/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.icon;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.icon.IconOperationParams;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class IconRequestParamsMapper {

  public IconOperationParams map(IconRequestParams iconRequestParams) throws BadRequestException {

    validateRequestParams(iconRequestParams);
    IconOperationParams operationParams = new IconOperationParams();
    operationParams.setKeywords(
        iconRequestParams.getKeywords() != null
            ? iconRequestParams.getKeywords()
            : new ArrayList<>());

    operationParams.setKeys(iconRequestParams.getKeys());
    operationParams.setIconTypeFilter(iconRequestParams.getType());
    operationParams.setCreatedStartDate(iconRequestParams.getCreatedStartDate());
    operationParams.setCreatedEndDate(iconRequestParams.getCreatedEndDate());
    operationParams.setLastUpdatedStartDate(iconRequestParams.getLastUpdatedStartDate());
    operationParams.setLastUpdatedEndDate(iconRequestParams.getLastUpdatedEndDate());
    operationParams.setPaging(iconRequestParams.isPaging());

    return operationParams;
  }

  private void validateRequestParams(IconRequestParams iconRequestParams)
      throws BadRequestException {

    if (iconRequestParams.hasCreatedStartDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.getLongDateString(iconRequestParams.getCreatedStartDate()))) {
      throw new BadRequestException(
          String.format(
              "createdStartDate %s is not valid",
              iconRequestParams.getCreatedStartDate().toString()));
    }

    if (iconRequestParams.hasCreatedEndDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.getLongDateString(iconRequestParams.getCreatedEndDate()))) {
      throw new BadRequestException(
          String.format(
              "createdEndDate %s is not valid", iconRequestParams.getCreatedEndDate().toString()));
    }

    if (iconRequestParams.hasLastUpdatedStartDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.getLongDateString(iconRequestParams.getLastUpdatedStartDate()))) {
      throw new BadRequestException(
          String.format(
              "lastUpdatedStartDate %s is not valid",
              iconRequestParams.getLastUpdatedStartDate().toString()));
    }

    if (iconRequestParams.hasLastUpdatedEndDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.getLongDateString(iconRequestParams.getLastUpdatedEndDate()))) {
      throw new BadRequestException(
          String.format(
              "lastUpdatedEndDate %s is not valid",
              iconRequestParams.getLastUpdatedEndDate().toString()));
    }
  }
}

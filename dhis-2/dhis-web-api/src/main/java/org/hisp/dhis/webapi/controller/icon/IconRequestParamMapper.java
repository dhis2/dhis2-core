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

import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.icon.IconQueryParams;
import org.hisp.dhis.icon.IconTypeFilter;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
@RequiredArgsConstructor
public class IconRequestParamMapper {

  private static final Set<String> ORDER_FIELD_NAMES = Set.of("created", "lastUpdated", "key");

  private static final ImmutableMap<IconTypeFilter, Boolean> TYPE_MAPPER =
      ImmutableMap.<IconTypeFilter, Boolean>builder()
          .put(IconTypeFilter.DEFAULT, Boolean.FALSE)
          .put(IconTypeFilter.CUSTOM, Boolean.TRUE)
          .build();

  public IconQueryParams map(IconRequestParams iconRequestParams) throws BadRequestException {

    validateRequestParams(iconRequestParams);
    validateOrderParams(iconRequestParams.getOrder(), ORDER_FIELD_NAMES);

    IconQueryParams queryParams = new IconQueryParams();
    queryParams.setKeywords(
        iconRequestParams.getKeywords() != null
            ? iconRequestParams.getKeywords()
            : new ArrayList<>());

    queryParams.setOrder(iconRequestParams.getOrder());
    queryParams.setKeys(iconRequestParams.getKeys());
    queryParams.setCreatedStartDate(iconRequestParams.getCreatedStartDate());
    queryParams.setCreatedEndDate(iconRequestParams.getCreatedEndDate());
    queryParams.setLastUpdatedStartDate(iconRequestParams.getLastUpdatedStartDate());
    queryParams.setLastUpdatedEndDate(iconRequestParams.getLastUpdatedEndDate());
    queryParams.setPaging(iconRequestParams.isPaging());
    queryParams.setIncludeCustomIcon(TYPE_MAPPER.getOrDefault(iconRequestParams.getType(), null));
    queryParams.setSearch(iconRequestParams.getSearch());

    return queryParams;
  }

  private void validateRequestParams(IconRequestParams iconRequestParams)
      throws BadRequestException {

    if (iconRequestParams.hasCreatedStartDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.toLongDate(iconRequestParams.getCreatedStartDate()))) {
      throw new BadRequestException(
          String.format(
              "createdStartDate %s is not valid",
              iconRequestParams.getCreatedStartDate().toString()));
    }

    if (iconRequestParams.hasCreatedEndDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.toLongDate(iconRequestParams.getCreatedEndDate()))) {
      throw new BadRequestException(
          String.format(
              "createdEndDate %s is not valid", iconRequestParams.getCreatedEndDate().toString()));
    }

    if (iconRequestParams.hasLastUpdatedStartDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.toLongDate(iconRequestParams.getLastUpdatedStartDate()))) {
      throw new BadRequestException(
          String.format(
              "lastUpdatedStartDate %s is not valid",
              iconRequestParams.getLastUpdatedStartDate().toString()));
    }

    if (iconRequestParams.hasLastUpdatedEndDate()
        && !DateUtils.dateTimeIsValid(
            DateUtils.toLongDate(iconRequestParams.getLastUpdatedEndDate()))) {
      throw new BadRequestException(
          String.format(
              "lastUpdatedEndDate %s is not valid",
              iconRequestParams.getLastUpdatedEndDate().toString()));
    }
  }
}

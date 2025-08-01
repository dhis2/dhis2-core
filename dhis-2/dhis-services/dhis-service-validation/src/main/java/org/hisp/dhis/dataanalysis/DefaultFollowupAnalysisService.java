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
package org.hisp.dhis.dataanalysis;

import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service("org.hisp.dhis.dataanalysis.FollowupAnalysisService")
@RequiredArgsConstructor
public class DefaultFollowupAnalysisService implements FollowupAnalysisService {
  private static final int MAX_LIMIT = 10_000;

  private final DataAnalysisStore dataAnalysisStore;

  private final FollowupValueManager followupValueManager;

  private final UserService userService;

  private final I18nManager i18nManager;

  @Override
  @Transactional(readOnly = true)
  public List<DeflatedDataValue> getFollowupDataValues(
      OrganisationUnit orgUnit,
      Collection<DataElement> dataElements,
      Collection<Period> periods,
      int limit) {
    if (orgUnit == null || limit < 1) {
      return List.of();
    }

    Set<DataElement> elements =
        dataElements.stream()
            .filter(de -> de.getValueType().isNumeric())
            .collect(Collectors.toSet());

    Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

    for (DataElement dataElement : elements) {
      categoryOptionCombos.addAll(dataElement.getCategoryOptionCombos());
    }

    log.debug("Starting min-max analysis, no of data elements: {}", elements.size());

    return dataAnalysisStore.getFollowupDataValues(
        elements, categoryOptionCombos, periods, orgUnit, limit);
  }

  @Override
  @Transactional(readOnly = true)
  public FollowupAnalysisResponse getFollowupDataValues(FollowupAnalysisRequest request) {
    validate(request);

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    List<FollowupValue> followupValues =
        followupValueManager.getFollowupDataValues(currentUser, request);

    I18nFormat format = i18nManager.getI18nFormat();
    followupValues.forEach(value -> value.setPeName(format.formatPeriod(value.getPeAsPeriod())));
    return new FollowupAnalysisResponse(new FollowupAnalysisMetadata(request), followupValues);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid generateAnalysisReport(FollowupAnalysisResponse followupAnalysisResponse) {
    Grid grid = new ListGrid();
    if (followupAnalysisResponse.getFollowupValues() != null) {
      I18n i18n = i18nManager.getI18n();

      grid.setTitle(i18n.getString("data_analysis_report"));

      grid.addHeader(new GridHeader(i18n.getString("dataelement"), false, true));
      grid.addHeader(new GridHeader(i18n.getString("source"), false, true));
      grid.addHeader(new GridHeader(i18n.getString("period"), false, true));
      grid.addHeader(new GridHeader(i18n.getString("min"), false, false));
      grid.addHeader(new GridHeader(i18n.getString("value"), false, false));
      grid.addHeader(new GridHeader(i18n.getString("max"), false, false));

      for (FollowupValue dataValue : followupAnalysisResponse.getFollowupValues()) {
        grid.addRow();
        grid.addValue(dataValue.getDeName());
        grid.addValue(dataValue.getOuName());
        grid.addValue(dataValue.getPe());
        grid.addValue(dataValue.getMin());
        grid.addValue(dataValue.getValue());
        grid.addValue(dataValue.getMax());
      }
    }

    return grid;
  }

  private void validate(FollowupAnalysisRequest request) {
    if (isEmpty(request.getDe()) && isEmpty(request.getDs())) {
      throw validationError(ErrorCode.E2300);
    }
    if (isEmpty(request.getOu())) {
      throw validationError(ErrorCode.E2203);
    }
    if ((request.getStartDate() == null || request.getEndDate() == null)
        && request.getPe() == null) {
      throw validationError(ErrorCode.E2301);
    }
    if (request.getStartDate() != null
        && request.getEndDate() != null
        && !request.getStartDate().before(request.getEndDate())) {
      throw validationError(ErrorCode.E2202);
    }
    if (request.getMaxResults() <= 0) {
      throw validationError(ErrorCode.E2205);
    }
    if (request.getMaxResults() > MAX_LIMIT) {
      throw validationError(ErrorCode.E2206, MAX_LIMIT);
    }
  }

  private IllegalQueryException validationError(ErrorCode error, Object... args) {
    ErrorMessage message = new ErrorMessage(error, args);
    log.warn(
        String.format(
            "Followup analysis request validation failed, code: '%s', message: '%s'",
            error, message.getMessage()));
    return new IllegalQueryException(message);
  }
}

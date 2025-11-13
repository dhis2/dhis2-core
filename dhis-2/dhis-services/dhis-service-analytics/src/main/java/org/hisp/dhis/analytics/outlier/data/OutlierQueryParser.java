/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.outlier.data;

import static org.hisp.dhis.analytics.outlier.Order.getOrderBy;
import static org.hisp.dhis.feedback.ErrorCode.E7617;
import static org.hisp.dhis.feedback.ErrorCode.E7622;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/** Parse and transform the incoming query params into the OutlierDetectionRequest. */
@Component
@AllArgsConstructor
public class OutlierQueryParser {
  private final IdentifiableObjectManager idObjectManager;
  private final DimensionalObjectProvider dimensionalObjectProducer;
  private final UserService userService;

  /**
   * Creates a {@link OutlierRequest} from the given query.
   *
   * @param queryParams the {@link OutlierQueryParams}.
   * @return a {@link OutlierRequest}.
   */
  public OutlierRequest getFromQuery(OutlierQueryParams queryParams, boolean analyzeOnly) {
    List<DataDimension> dimensions = getDataDimensions(queryParams);

    OutlierRequest.OutlierRequestBuilder builder =
        OutlierRequest.builder()
            .dataDimensions(dimensions)
            .startDate(queryParams.getStartDate())
            .endDate(queryParams.getEndDate())
            .periods(getPeriods(queryParams.getPe(), queryParams.getRelativePeriodDate()))
            .orgUnits(getOrganisationUnits(queryParams))
            .analyzeOnly(analyzeOnly)
            .dataStartDate(queryParams.getDataStartDate())
            .dataEndDate(queryParams.getDataEndDate())
            .outputIdScheme(queryParams.getOutputIdScheme())
            .skipRounding(queryParams.isSkipRounding())
            .queryKey(queryParams.queryKey());

    if (queryParams.getAlgorithm() != null) {
      builder.algorithm(queryParams.getAlgorithm());
    }

    if (queryParams.getThreshold() != null) {
      builder.threshold(queryParams.getThreshold());
    }

    if (queryParams.getOrderBy() != null) {
      builder.orderBy(getOrderBy(queryParams.getOrderBy()));
    }

    if (queryParams.getSortOrder() != null) {
      builder.sortOrder(queryParams.getSortOrder());
    }

    if (queryParams.getMaxResults() != null) {
      builder.maxResults(queryParams.getMaxResults());
    }

    if (analyzeOnly) {
      builder.explainOrderId(UUID.randomUUID().toString());
    }

    return builder.build();
  }

  /**
   * Retrieves the list of outlier data dimensions
   *
   * @param queryParams the {@link OutlierQueryParams}.
   * @return the list of {@link DataDimension}.
   */
  private List<DataDimension> getDataDimensions(OutlierQueryParams queryParams) {
    List<DataSet> dataSets = idObjectManager.getByUid(DataSet.class, queryParams.getDs());

    // Re-fetch data elements to maintain access control.
    // Only data elements and category option combos are supported for now.

    // DataSet
    List<String> dx =
        dataSets.stream()
            .map(DataSet::getDataElements)
            .flatMap(Collection::stream)
            .filter(d -> d.getValueType().isNumeric())
            .map(DataElement::getUid)
            .toList();

    List<DataDimension> dataDimensions =
        new ArrayList<>(
            idObjectManager.getByUid(DataElement.class, dx).stream()
                .map(de -> new DataDimension(de, null))
                .toList());

    // DataElement and CategoryOptionCombo
    dataDimensions.addAll(
        queryParams.getDx().stream()
            .map(
                dd -> {
                  String[] tokens = dd.split("\\.");
                  List<DataElement> dataElements =
                      idObjectManager.getByUid(DataElement.class, List.of(tokens[0]));
                  List<CategoryOptionCombo> categoryOptionCombos =
                      tokens.length == 2
                          ? idObjectManager.getByUid(CategoryOptionCombo.class, List.of(tokens[1]))
                          : new ArrayList<>();
                  if (dataElements.size() == 1 && categoryOptionCombos.size() == 1) {
                    return new DataDimension(dataElements.get(0), categoryOptionCombos.get(0));
                  } else if (dataElements.size() == 1) {
                    return new DataDimension(dataElements.get(0), null);
                  }
                  return null;
                })
            .toList());

    return dataDimensions;
  }

  /**
   * The function retrieves all required organisation units, accepting all forms of ou requirements
   * like uids, levels, groups, user organisations ...
   *
   * @param queryParams the {@link OutlierQueryParams}.
   * @return a list of the {@link OrganisationUnit}.
   */
  private List<OrganisationUnit> getOrganisationUnits(OutlierQueryParams queryParams) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    Set<OrganisationUnit> userOrganisationUnits;

    if (currentUser.hasOrganisationUnit()) {
      userOrganisationUnits = currentUser.getOrganisationUnits();
    } else if (currentUser.hasDataViewOrganisationUnit()) {
      userOrganisationUnits = currentUser.getDataViewOrganisationUnits();
    } else {
      throw new IllegalQueryException(E7622, currentUser.getUsername());
    }

    if (queryParams.getOu().isEmpty()) {
      return userOrganisationUnits.stream().toList();
    }

    List<OrganisationUnit> validOrganisationUnits =
        applySecurityConstrain(userOrganisationUnits, queryParams.getOu(), currentUser);

    if (validOrganisationUnits.isEmpty()) {
      throw new IllegalQueryException(
          E7617, String.join(",", queryParams.getOu()), currentUser.getUsername());
    }

    return validOrganisationUnits;
  }

  /**
   * The function retrieves all required organisation units compatible the with security constrain
   *
   * @param organisationUnitsSecurityConstrain list of the {@link OrganisationUnit}
   * @param organisationUnits list of the requested organisation unit Uids
   * @param currentUser the {@link User}.
   * @return a list of the {@link OrganisationUnit}.
   */
  private List<OrganisationUnit> applySecurityConstrain(
      Set<OrganisationUnit> organisationUnitsSecurityConstrain,
      Set<String> organisationUnits,
      User currentUser) {
    DimensionalObject orgUnitDimension =
        dimensionalObjectProducer.getOrgUnitDimension(
            organisationUnits.stream().toList(),
            DisplayProperty.NAME,
            currentUser != null ? currentUser.getOrganisationUnits().stream().toList() : List.of(),
            IdScheme.UID);

    return orgUnitDimension.getItems().stream()
        .filter(
            bdo ->
                organisationUnitsSecurityConstrain.isEmpty()
                    || organisationUnitsSecurityConstrain.stream()
                        .anyMatch(((OrganisationUnit) bdo)::isDescendant))
        .map(ou -> (OrganisationUnit) ou)
        .toList();
  }

  /**
   * The method retrieves the list of the periods
   *
   * @param relativePeriod, the period dimension.
   * @return list of the {@link Period}.
   */
  private List<PeriodDimension> getPeriods(String relativePeriod, Date relativePeriodDate) {
    if (StringUtils.isBlank(relativePeriod)) {
      return List.of();
    }

    return dimensionalObjectProducer
        .getPeriodDimension(List.of(relativePeriod), relativePeriodDate)
        .getItems()
        .stream()
        .map(PeriodDimension.class::cast)
        .toList();
  }
}

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
package org.hisp.dhis.webapi.controller.datavalue;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;

import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryDto;
import org.springframework.stereotype.Component;

/**
 * This a simple component responsible for extracting and encapsulating objects from the controller
 * layer. This can be seen as an extension of the controller.
 */
@Component
@RequiredArgsConstructor
public class DataValidator {
  private final CategoryService categoryService;

  private final OrganisationUnitService organisationUnitService;

  private final IdentifiableObjectManager idObjectManager;

  private final InputUtils inputUtils;

  private final AggregateAccessManager accessManager;

  private final UserService userService;

  /**
   * Retrieves and verifies a data set.
   *
   * @param uid the data set identifier.
   * @return the {@link DataSet}.
   * @throws IllegalQueryException if the validation fails.
   */
  public DataSet getAndValidateDataSet(String uid) {
    return idObjectManager.load(DataSet.class, ErrorCode.E1105, uid);
  }

  /**
   * Retrieves and verifies a data element.
   *
   * @param uid the data element identifier.
   * @return the {@link DataElement}.
   * @throws IllegalQueryException if the validation fails.
   */
  public DataElement getAndValidateDataElement(String uid) {
    return idObjectManager.load(DataElement.class, ErrorCode.E1100, uid);
  }

  /**
   * Retrieves and verifies a category option combination.
   *
   * @param uid the category option combination identifier.
   * @return the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public CategoryOptionCombo getAndValidateCategoryOptionCombo(String uid) {
    return idObjectManager.load(CategoryOptionCombo.class, ErrorCode.E1103, uid);
  }

  /**
   * Retrieves and verifies a category option combo. If not required, and if the given identifier is
   * null, the default category option combo will be returned if an object with the given identifier
   * does not exist.
   *
   * @param uid the category option combo identifier.
   * @param requireCategoryOptionCombo whether an exception should be thrown if the category option
   *     combo does not exist.
   * @return the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public CategoryOptionCombo getAndValidateCategoryOptionCombo(
      String uid, boolean requireCategoryOptionCombo) {
    CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo(uid);

    if (categoryOptionCombo == null) {
      if (requireCategoryOptionCombo) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2018));
      } else if (uid != null) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1103, uid));
      } else {
        categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
      }
    }

    return categoryOptionCombo;
  }

  /**
   * Retrieves and verifies a category (attribute) option combo.
   *
   * @param attribute the {@link DataValueCategoryDto}.
   * @return the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public CategoryOptionCombo getAndValidateAttributeOptionCombo(DataValueCategoryDto attribute) {
    attribute = ObjectUtils.firstNonNull(attribute, new DataValueCategoryDto());

    CategoryOptionCombo attributeOptionCombo =
        inputUtils.getAttributeOptionCombo(attribute.getCombo(), attribute.getOptions(), false);

    if (attributeOptionCombo == null) {
      throw new IllegalQueryException(
          new ErrorMessage(
              ErrorCode.E1104,
              String.format("%s %s", attribute.getCombo(), attribute.getOptions())));
    }

    return attributeOptionCombo;
  }

  /**
   * Retrieves and verifies a category (attribute) option combo.
   *
   * @param cc the category combo identifier.
   * @param cp the category option string.
   * @return the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public CategoryOptionCombo getAndValidateAttributeOptionCombo(String cc, String cp) {
    Set<String> options = TextUtils.splitToSet(cp, TextUtils.SEMICOLON);

    DataValueCategoryDto attribute = new DataValueCategoryDto(cc, options);

    return getAndValidateAttributeOptionCombo(attribute);
  }

  /**
   * Retrieves and verifies a period.
   *
   * @param pe the period ISO identifier.
   * @return the {@link Period}.
   * @throws IllegalQueryException if the validation fails.
   */
  public Period getAndValidatePeriod(String pe) {
    Period period = PeriodType.getPeriodFromIsoString(pe);

    if (period == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1101, pe));
    }

    return period;
  }

  /**
   * Retrieves and verifies an organisation unit.
   *
   * @param uid the organisation unit identifier.
   * @return the {@link OrganisationUnit}.
   * @throws IllegalQueryException if the validation fails.
   */
  public OrganisationUnit getAndValidateOrganisationUnit(String uid) {
    OrganisationUnit organisationUnit =
        idObjectManager.load(OrganisationUnit.class, ErrorCode.E1102, uid);

    boolean isInHierarchy =
        organisationUnitService.isInUserHierarchyCached(
            userService.getUserByUsername(CurrentUserUtil.getCurrentUsername()), organisationUnit);

    if (!isInHierarchy) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2020, uid));
    }

    return organisationUnit;
  }

  /**
   * Validates the OrganisationUnit dates against the given period.
   *
   * @param organisationUnit the {@link OrganisationUnit} and its dates.
   * @param period the {@link Period} to be checked.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateOrganisationUnitPeriod(OrganisationUnit organisationUnit, Period period) {
    Date openingDate = organisationUnit.getOpeningDate();
    Date closedDate = organisationUnit.getClosedDate();
    Date startDate = period.getStartDate();
    Date endDate = period.getEndDate();

    if ((closedDate != null && closedDate.before(startDate)) || openingDate.after(endDate)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2019, organisationUnit.getUid()));
    }
  }

  /**
   * Check if the respective User has read access to the given DataValue.
   *
   * @param userDetails the User.
   * @param dataValue the {@link DataValue}.
   * @throws WebMessageException if the validation fails.
   */
  public void checkDataValueSharing(UserDetails userDetails, DataValue dataValue)
      throws WebMessageException {
    final List<String> errors = accessManager.canRead(userDetails, dataValue);

    if (!errors.isEmpty()) {
      throw new WebMessageException(forbidden(errors.toString()));
    }
  }
}

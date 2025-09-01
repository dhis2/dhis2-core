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

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryParams;
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
   * Retrieves and verifies a category (attribute) option combo.
   *
   * @param attribute the {@link DataValueCategoryParams}.
   * @return the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public CategoryOptionCombo getAndValidateAttributeOptionCombo(DataValueCategoryParams attribute) {
    attribute = ObjectUtils.firstNonNull(attribute, new DataValueCategoryParams());

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

    DataValueCategoryParams attribute = new DataValueCategoryParams(cc, options);

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
}

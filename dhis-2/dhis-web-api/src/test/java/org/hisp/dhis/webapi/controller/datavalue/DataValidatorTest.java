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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataValidatorTest {
  @Mock private CategoryService categoryService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private InputUtils inputUtils;

  @Mock private AggregateAccessManager accessManager;

  @Mock private DataValidator dataValidator;
  @Mock private UserService userService;

  private DataSet dsA;

  private DataElement deA;

  private CategoryOption coA;

  private CategoryOptionCombo cocA;

  @BeforeEach
  public void setUp() {
    dataValidator =
        new DataValidator(
            categoryService,
            organisationUnitService,
            idObjectManager,
            inputUtils,
            accessManager,
            userService);

    dsA = new DataSet("dataSet", new MonthlyPeriodType());
    deA = new DataElement();
    coA = new CategoryOption();
    cocA = new CategoryOptionCombo();

    dsA.addDataSetElement(deA);
    deA.getDataSetElements().addAll(dsA.getDataSetElements());

    cocA.addCategoryOption(coA);
  }

  @Test
  void testGetAndValidateAttributeOptionComboNull() {
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> dataValidator.getAndValidateAttributeOptionCombo(null, null));
    assertEquals(ErrorCode.E1104, ex.getErrorCode());
  }

  @Test
  void testInvalidPeriod() {
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> dataValidator.getAndValidatePeriod("502"));

    assertEquals(ErrorCode.E1101, ex.getErrorCode());
  }
}

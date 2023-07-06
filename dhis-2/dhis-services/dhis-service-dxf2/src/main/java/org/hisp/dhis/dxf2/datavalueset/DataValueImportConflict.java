/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.importsummary.ImportConflictDescriptor;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;

/**
 * Import conflicts related to a single {@link org.hisp.dhis.dxf2.datavalue.DataValue} during the
 * import of a {@link DataValueSet}.
 *
 * @author Jan Bernitt
 */
public enum DataValueImportConflict implements ImportConflictDescriptor {
  DATA_ELEMENT_NOT_FOUND(ErrorCode.E7610, "dataElement", DataElement.class),
  PERIOD_NOT_VALID(ErrorCode.E7611, "period", Period.class),
  ORG_UNIT_NOT_FOUND(ErrorCode.E7612, "orgUnit", OrganisationUnit.class),
  CATEGORY_OPTION_COMBO_NOT_FOUND(
      ErrorCode.E7613, "categoryOptionCombo", CategoryOptionCombo.class),
  CATEGORY_OPTION_COMBO_NOT_ACCESSIBLE(
      ErrorCode.E7614, "categoryOptionCombo", CategoryOptionCombo.class, CategoryOption.class),
  ATTR_OPTION_COMBO_NOT_FOUND(ErrorCode.E7615, "attributeOptionCombo", CategoryOptionCombo.class),
  ATTR_OPTION_COMBO_NOT_ACCESSIBLE(
      ErrorCode.E7616, "attributeOptionCombo", CategoryOptionCombo.class, CategoryOption.class),
  ORG_UNIT_NOT_IN_USER_HIERARCHY(ErrorCode.E7617, "orgUnit", OrganisationUnit.class, User.class),
  DATA_ELEMENT_VALUE_NOT_DEFINED(ErrorCode.E7618, "value", DataElement.class),
  DATA_ELEMENT_VALUE_NOT_VALID(ErrorCode.E7619, "value", DataElement.class, I18n.class),
  COMMENT_NOT_VALID(ErrorCode.E7620, "comment", I18n.class),
  DATA_ELEMENT_INVALID_OPTION(ErrorCode.E7621, "value", DataElement.class),

  // Constraints
  CATEGORY_OPTION_COMBO_NOT_SPECIFIED(ErrorCode.E7630, "categoryOptionCombo"),
  ATTR_OPTION_COMBO_NOT_SPECIFIED(ErrorCode.E7631, "attributeOptionCombo"),
  PERIOD_TYPE_NOT_VALID_FOR_DATA_ELEMENT(ErrorCode.E7632, null, Period.class, DataElement.class),
  DATA_ELEMENT_STRICT(ErrorCode.E7633, "dataElement", DataElement.class, DataSet.class),
  CATEGORY_OPTION_COMBO_STRICT(
      ErrorCode.E7634, "categoryOptionCombo", CategoryOptionCombo.class, DataElement.class),
  ATTR_OPTION_COMBO_STRICT(
      ErrorCode.E7635, "attributeOptionCombo", CategoryOptionCombo.class, DataElement.class),
  ORG_UNIT_STRICT(ErrorCode.E7636, "orgUnit", OrganisationUnit.class, DataElement.class),
  STORED_BY_NOT_VALID(ErrorCode.E7637, "storedBy", I18n.class),
  PERIOD_NOT_VALID_FOR_ATTR_OPTION_COMBO(
      ErrorCode.E7638, "period", Period.class, CategoryOptionCombo.class),
  ORG_UNIT_NOT_VALID_FOR_ATTR_OPTION_COMBO(
      ErrorCode.E7639, "orgUnit", OrganisationUnit.class, CategoryOptionCombo.class),
  PERIOD_EXPIRED(ErrorCode.E7640, "period", Period.class, DataSet.class),
  PERIOD_AFTER_DATA_ELEMENT_PERIODS(ErrorCode.E7641, "period", Period.class, DataElement.class),
  VALUE_ALREADY_APPROVED(
      ErrorCode.E7642,
      null,
      OrganisationUnit.class,
      Period.class,
      CategoryOptionCombo.class,
      DataSet.class),
  PERIOD_NOT_OPEN_FOR_DATA_SET(ErrorCode.E7643, "period", Period.class, DataSet.class),
  PERIOD_NOT_CONFORM_TO_OPEN_PERIODS(ErrorCode.E7644, "period", Period.class),
  FILE_RESOURCE_NOT_FOUND(ErrorCode.E7645, "dataElement", DataElement.class);

  private final ErrorCode errorCode;

  private final Class<?>[] objectTypes;

  private final String property;

  DataValueImportConflict(ErrorCode errorCode, String property, Class<?>... objectTypes) {
    this.errorCode = errorCode;
    this.property = property;
    this.objectTypes = objectTypes;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  @Override
  public Class<?>[] getObjectTypes() {
    return objectTypes;
  }

  @Override
  public String getProperty() {
    return property;
  }
}

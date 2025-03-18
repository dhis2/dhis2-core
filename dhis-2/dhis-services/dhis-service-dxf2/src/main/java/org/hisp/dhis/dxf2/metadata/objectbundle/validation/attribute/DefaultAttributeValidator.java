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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation.attribute;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * Validate {@link org.hisp.dhis.attribute.Attribute} values based on the {@link
 * org.hisp.dhis.attribute.Attribute}'s {@link ValueType}.
 *
 * @author viet
 */
@Component
@RequiredArgsConstructor
public class DefaultAttributeValidator implements AttributeValidator {
  private final IdentifiableObjectManager manager;

  private final UserService userService;

  /**
   * Call all predefined map of validators for checking given value and {@link ValueType}.
   *
   * <p>If there is no validator defined for given {@link ValueType} then return emptyList.
   *
   * @param valueType Metadata Attribute {@link ValueType}.
   * @param value the value for validating.
   * @param addError {@link Consumer} which will accept generated {@link ErrorReport}
   */
  @Override
  public void validate(ValueType valueType, String value, Consumer<ErrorReport> addError) {
    if (isEmpty(value)) return;
    switch (valueType) {
      case INTEGER -> validateValue(value, MathUtils::isInteger, ErrorCode.E6006, addError);
      case INTEGER_POSITIVE ->
          validateValue(value, MathUtils::isPositiveInteger, ErrorCode.E6007, addError);
      case INTEGER_NEGATIVE ->
          validateValue(value, MathUtils::isNegativeInteger, ErrorCode.E6013, addError);
      case NUMBER -> validateValue(value, MathUtils::isNumeric, ErrorCode.E6008, addError);
      case INTEGER_ZERO_OR_POSITIVE ->
          validateValue(value, MathUtils::isZeroOrPositiveInteger, ErrorCode.E6009, addError);
      case PERCENTAGE -> validateValue(value, MathUtils::isPercentage, ErrorCode.E6010, addError);
      case UNIT_INTERVAL ->
          validateValue(value, MathUtils::isUnitInterval, ErrorCode.E6011, addError);
      case PHONE_NUMBER ->
          validateValue(value, ValidationUtils::isPhoneNumber, ErrorCode.E6021, addError);
      case DATE -> validateValue(value, DateUtils::dateIsValid, ErrorCode.E6014, addError);
      case DATETIME -> validateValue(value, DateUtils::dateTimeIsValid, ErrorCode.E6015, addError);
      case BOOLEAN -> validateValue(value, MathUtils::isBool, ErrorCode.E6016, addError);
      case TRUE_ONLY -> validateValue(value, "true"::equals, ErrorCode.E6017, addError);
      case EMAIL -> validateValue(value, ValidationUtils::emailIsValid, ErrorCode.E6018, addError);
      case ORGANISATION_UNIT -> validateOrganisationUnitExists(value, addError);
      case FILE_RESOURCE -> validateFileResourceExists(value, addError);
      case USERNAME -> validateUserExists(value, addError);
    }
  }

  private void validateValue(
      String value, Predicate<String> validator, ErrorCode error, Consumer<ErrorReport> addError) {
    if (!validator.test(value)) addError.accept(error(error, value));
  }

  private void validateFileResourceExists(String value, Consumer<ErrorReport> addError) {
    FileResource fr = manager.get(FileResource.class, value);
    if (fr == null)
      addError.accept(error(ErrorCode.E6019, value, FileResource.class.getSimpleName()));
  }

  private void validateOrganisationUnitExists(String value, Consumer<ErrorReport> addError) {
    OrganisationUnit ou = manager.get(OrganisationUnit.class, value);
    if (ou == null)
      addError.accept(error(ErrorCode.E6019, value, OrganisationUnit.class.getSimpleName()));
  }

  private void validateUserExists(String value, Consumer<ErrorReport> addError) {
    User user = userService.getUserByUsername(value);
    if (user == null) addError.accept(error(ErrorCode.E6020, value));
  }

  private static ErrorReport error(ErrorCode error, Object... args) {
    return new ErrorReport(AttributeValues.class, error, args);
  }
}

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
package org.hisp.dhis.webapi.controller.datavalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.fileresource.FileResourceDomain.DATA_VALUE;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.normalizeBoolean;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import com.google.common.base.Preconditions;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.webdomain.DataValueFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryDto;
import org.springframework.stereotype.Component;

/**
 * This a simple component responsible for extracting and encapsulating objects from the controller
 * layer. This can be seen as an extension of the controller.
 */
@Component
public class DataValidator {
  private final CategoryService categoryService;

  private final OrganisationUnitService organisationUnitService;

  private final DataSetService dataSetService;

  private final IdentifiableObjectManager idObjectManager;

  private final DataValueService dataValueService;

  private final InputUtils inputUtils;

  private final FileResourceService fileResourceService;

  private final CalendarService calendarService;

  private final AggregateAccessManager accessManager;

  public DataValidator(
      final CategoryService categoryService,
      final OrganisationUnitService organisationUnitService,
      final DataSetService dataSetService,
      final IdentifiableObjectManager idObjectManager,
      final DataValueService dataValueService,
      final InputUtils inputUtils,
      final FileResourceService fileResourceService,
      final CalendarService calendarService,
      final AggregateAccessManager accessManager) {
    checkNotNull(categoryService);
    checkNotNull(organisationUnitService);
    checkNotNull(dataSetService);
    checkNotNull(idObjectManager);
    checkNotNull(dataValueService);
    checkNotNull(inputUtils);
    checkNotNull(fileResourceService);
    checkNotNull(calendarService);
    checkNotNull(accessManager);

    this.categoryService = categoryService;
    this.organisationUnitService = organisationUnitService;
    this.dataSetService = dataSetService;
    this.idObjectManager = idObjectManager;
    this.dataValueService = dataValueService;
    this.inputUtils = inputUtils;
    this.fileResourceService = fileResourceService;
    this.calendarService = calendarService;
    this.accessManager = accessManager;
  }

  /**
   * Retrieves and verifies a data element.
   *
   * @param uid the data element identifier.
   * @return the {@link DataElement}.
   * @throws IllegalQueryException if the validation fails.
   */
  public DataElement getAndValidateDataElement(String uid) {
    final DataElement dataElement = idObjectManager.get(DataElement.class, uid);

    if (dataElement == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1100, uid));
    }

    return dataElement;
  }

  /**
   * Retrieves and verifies a category option combo.
   *
   * @param uid the category option combo identifier.
   * @param requireCategoryOptionCombo flag used as part of the validation.
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

    final CategoryOptionCombo attributeOptionCombo =
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
   * Retrieves a {@link DataValueCategoryDto} based on the given parameters.
   *
   * @param cc the category combo identifier.
   * @param cp the category option string.
   * @return a {@link DataValueCategoryDto}.
   */
  public DataValueCategoryDto getDataValueCategoryDto(String cc, String cp) {
    Set<String> options = TextUtils.splitToSet(cp, TextUtils.SEMICOLON);

    return new DataValueCategoryDto(cc, options);
  }

  /**
   * Retrieves and verifies a period.
   *
   * @param pe the period ISO identifier.
   * @return the {@link Period}.
   * @throws IllegalQueryException if the validation fails.
   */
  public Period getAndValidatePeriod(final String pe) {
    final Period period = PeriodType.getPeriodFromIsoString(pe);

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
    final OrganisationUnit organisationUnit = idObjectManager.get(OrganisationUnit.class, uid);

    if (organisationUnit == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1102, uid));
    }

    final boolean isInHierarchy = organisationUnitService.isInUserHierarchyCached(organisationUnit);

    if (!isInHierarchy) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2020, uid));
    }

    return organisationUnit;
  }

  /**
   * Retrieves and verifies a data set.
   *
   * @param uid the DataSet uid.
   * @param dataElement the {@link DataElement} to be checked in the DataSet.
   * @return the valid DataSet.
   * @throws IllegalQueryException if the validation fails.
   */
  public DataSet getAndValidateOptionalDataSet(String uid, DataElement dataElement) {
    if (uid == null) {
      return null;
    }

    final DataSet dataSet = dataSetService.getDataSet(uid);

    if (dataSet == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1105, uid));
    }

    if (!dataSet.getDataElements().contains(dataElement)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2021, uid, dataElement.getUid()));
    }

    return dataSet;
  }

  /**
   * Validates and retrieves a data value.
   *
   * @param dataValueRequest the {@link DataValueFollowUpRequest}.
   * @return a data value.
   * @throws IllegalQueryException if the validation fails.
   */
  public DataValue getAndValidateDataValue(DataValueFollowUpRequest dataValueRequest) {
    DataElement dataElement = getAndValidateDataElement(dataValueRequest.getDataElement());
    Period period = PeriodType.getPeriodFromIsoString(dataValueRequest.getPeriod());
    OrganisationUnit orgUnit = getAndValidateOrganisationUnit(dataValueRequest.getOrgUnit());
    CategoryOptionCombo categoryOptionCombo =
        getAndValidateCategoryOptionCombo(dataValueRequest.getCategoryOptionCombo(), false);
    CategoryOptionCombo attributeOptionCombo =
        getAndValidateCategoryOptionCombo(dataValueRequest.getAttributeOptionCombo(), false);
    DataValue dataValue =
        dataValueService.getDataValue(
            dataElement, period, orgUnit, categoryOptionCombo, attributeOptionCombo);

    if (dataValue == null) {
      throw new IllegalQueryException(ErrorCode.E2032);
    }

    return dataValue;
  }

  /**
   * Validates the OrganisationUnit dates against the given period.
   *
   * @param organisationUnit the {@link OrganisationUnit} and its dates.
   * @param period the {@link Period} to be checked.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateOrganisationUnitPeriod(OrganisationUnit organisationUnit, Period period) {
    final Date openingDate = organisationUnit.getOpeningDate();
    final Date closedDate = organisationUnit.getClosedDate();
    final Date startDate = period.getStartDate();
    final Date endDate = period.getEndDate();

    if ((closedDate != null && closedDate.before(startDate)) || openingDate.after(endDate)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2019, organisationUnit.getUid()));
    }
  }

  /**
   * Validate if the is after the last future period allowed by the DataElement.
   *
   * @param period the period to be validated.
   * @param dataElement the {@link DataElement}.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateInvalidFuturePeriod(Period period, DataElement dataElement) {
    final Period latestFuturePeriod = dataElement.getLatestOpenFuturePeriod();

    if (period.isAfter(latestFuturePeriod) && calendarService.getSystemCalendar().isIso8601()) {
      throw new IllegalQueryException(
          new ErrorMessage(
              ErrorCode.E2022,
              period.getIsoDate(),
              latestFuturePeriod.getIsoDate(),
              dataElement.getUid()));
    }
  }

  /**
   * Check for an invalid period within the given CategoryOptionCombo (attribute option combo).
   *
   * @param attributeOptionCombo is the {@link CategoryOptionCombo}.
   * @param period the {@link Period} to be checked.
   * @param dataSet the {@link DataSet} (if present) to be checked.
   * @param dataElement the {@link DataElement} to be checked.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateAttributeOptionCombo(
      CategoryOptionCombo attributeOptionCombo,
      Period period,
      DataSet dataSet,
      DataElement dataElement) {
    for (CategoryOption option : attributeOptionCombo.getCategoryOptions()) {
      if (option.getStartDate() != null && period.getEndDate().before(option.getStartDate())) {
        throw new IllegalQueryException(
            new ErrorMessage(
                ErrorCode.E2023,
                period.getIsoDate(),
                getMediumDateString(option.getStartDate()),
                option.getUid()));
      }

      Date adjustedEndDate =
          (dataSet != null)
              ? option.getAdjustedEndDate(dataSet)
              : option.getAdjustedEndDate(dataElement);

      if (adjustedEndDate != null && period.getStartDate().after(adjustedEndDate)) {
        throw new IllegalQueryException(
            new ErrorMessage(
                ErrorCode.E2024,
                period.getIsoDate(),
                getMediumDateString(adjustedEndDate),
                option.getUid()));
      }
    }
  }

  /**
   * Validate if the DataSet or DataElement is locked based on the input arguments.
   *
   * @param user the current User.
   * @param dataElement the {@link DataElement}.
   * @param period the {@link Period}.
   * @param dataSet the {@link DataSet}.
   * @param organisationUnit the {@link OrganisationUnit}.
   * @param attributeOptionCombo the CategoryOptionCombo.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateDataSetNotLocked(
      User user,
      DataElement dataElement,
      Period period,
      DataSet dataSet,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo) {
    if (dataSet == null
        ? !dataSetService
            .getLockStatus(user, dataElement, period, organisationUnit, attributeOptionCombo, null)
            .isOpen()
        : !dataSetService
            .getLockStatus(user, dataSet, period, organisationUnit, attributeOptionCombo, null)
            .isOpen()) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2017));
    }
  }

  /**
   * Validate if the period is open for the given DataSet or DataElement.
   *
   * @param dataElement the {@link DataElement}.
   * @param dataSet the {@link DataSet}.
   * @param period the {@link Period}.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateDataInputPeriodForDataElementAndPeriod(
      DataElement dataElement, DataSet dataSet, final Period period) {
    if (!(dataSet == null
        ? dataElement.isDataInputAllowedForPeriodAndDate(period, new Date())
        : dataSet.isDataInputPeriodAndDateAllowed(period, new Date()))) {
      throw new IllegalQueryException(
          new ErrorMessage(ErrorCode.E2025, period.getIsoDate(), dataSet.getUid()));
    }
  }

  /**
   * Validates if the given file resource uid has a valid FileResource associated with.
   *
   * @param fileResourceUid the uid of the FileResource.
   * @param valueType
   * @param valueTypeOptions
   * @return a valid FileResource.
   * @throws WebMessageException if any validation fails.
   */
  public FileResource validateAndSetAssigned(
      String fileResourceUid, ValueType valueType, ValueTypeOptions valueTypeOptions)
      throws WebMessageException {
    Preconditions.checkNotNull(fileResourceUid);

    final FileResource fileResource = fileResourceService.getFileResource(fileResourceUid);

    if (fileResource == null || fileResource.getDomain() != DATA_VALUE) {
      throw new WebMessageException(notFound(FileResource.class, fileResourceUid));
    }

    if (fileResource.isAssigned()) {
      throw new IllegalQueryException(ErrorCode.E2026);
    }

    if (valueType != null && valueTypeOptions != null) {
      String validationResult = dataValueIsValid(fileResource, valueType, valueTypeOptions);

      if (validationResult != null) {
        fileResourceService.deleteFileResource(fileResource);

        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2027, validationResult));
      }
    }

    fileResource.setAssigned(true);

    return fileResource;
  }

  /**
   * Validates a comment.
   *
   * @param comment the comment to be validated.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateComment(String comment) {
    final String commentValid = ValidationUtils.commentIsValid(comment);

    if (commentValid != null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2028, commentValid));
    }
  }

  /**
   * Checks if the given data value is a valid association with the OptionSet.
   *
   * @param dataValue the data value.
   * @param optionSet the {@link OptionSet}.
   * @param dataElement the {@link DataElement}.
   * @throws IllegalQueryException if the validation fails.
   */
  public void validateOptionSet(String dataValue, OptionSet optionSet, DataElement dataElement) {
    if (!isNullOrEmpty(dataValue)
        && optionSet != null
        && !optionSet.getOptionCodesAsSet().contains(dataValue)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2029, dataElement.getUid()));
    }
  }

  /**
   * Validates if the given data value is valid for the given DataElement, and normalize it if the
   * dataValue is a boolean type.
   *
   * @param dataValue the data value.
   * @param dataElement the {@link DataElement}.
   * @return the normalized boolean or the same dataValue provided.
   * @throws IllegalQueryException if the validation fails.
   */
  public String validateAndNormalizeDataValue(String dataValue, DataElement dataElement) {
    final String normalizedBoolean = normalizeBoolean(dataValue, dataElement.getValueType());

    final String valueValid = dataValueIsValid(normalizedBoolean, dataElement);

    if (valueValid != null) {
      throw new IllegalQueryException(
          new ErrorMessage(ErrorCode.E2030, dataElement.getValueType()));
    }

    return normalizedBoolean;
  }

  /**
   * Checks if the user has write access to the given category option combo.
   *
   * @param user the user.
   * @param categoryOptionCombo the {@link CategoryOptionCombo}.
   * @throws IllegalQueryException if the validation fails.
   */
  public void checkCategoryOptionComboAccess(User user, CategoryOptionCombo categoryOptionCombo) {
    final List<String> categoryOptionComboErrors =
        accessManager.canWriteCached(user, categoryOptionCombo);

    if (!categoryOptionComboErrors.isEmpty()) {
      String arg = String.format("%s %s", categoryOptionCombo.getUid(), categoryOptionComboErrors);

      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2031, arg));
    }
  }

  /**
   * Check if the respective User has read access to the given DataValue.
   *
   * @param user the User.
   * @param dataValue the {@link DataValue}.
   * @throws WebMessageException if the validation fails.
   */
  public void checkDataValueSharing(User user, DataValue dataValue) throws WebMessageException {
    final List<String> errors = accessManager.canRead(user, dataValue);

    if (!errors.isEmpty()) {
      throw new WebMessageException(forbidden(errors.toString()));
    }
  }
}

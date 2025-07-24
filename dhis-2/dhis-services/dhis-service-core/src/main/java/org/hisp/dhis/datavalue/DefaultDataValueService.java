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
package org.hisp.dhis.datavalue;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data value service implementation. Note that data values are softly deleted, which implies having
 * the deleted property set to true and updated.
 *
 * @author Kristian Nordal
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.datavalue.DataValueService")
public class DefaultDataValueService implements DataValueService {

  private final DataEntryService dataEntryService;
  private final DataValueStore dataValueStore;
  private final CategoryService categoryService;

  // -------------------------------------------------------------------------
  // Basic DataValue
  // -------------------------------------------------------------------------

  @Nonnull
  private DataEntryValue.Input toDataEntryValue(DataValue dataValue) {
    return new DataEntryValue.Input(
        getUid(dataValue.getDataElement()),
        getUid(dataValue.getSource()),
        getUid(dataValue.getCategoryOptionCombo()),
        null,
        getUid(dataValue.getAttributeOptionCombo()),
        dataValue.getPeriod().getIsoDate(),
        dataValue.getValue(),
        dataValue.getComment(),
        dataValue.isFollowup(),
        dataValue.isDeleted());
  }

  private static String getUid(IdentifiableObject object) {
    return object == null ? null : object.getUid();
  }

  @Override
  @IndirectTransactional
  public boolean addDataValue(DataValue dataValue) {
    try {
      dataEntryService.upsertValue(
          false, null, dataEntryService.decodeValue(null, toDataEntryValue(dataValue)));
      return true;
    } catch (ConflictException | BadRequestException ex) {
      return false;
    }
  }

  @Override
  @IndirectTransactional
  public void updateDataValue(DataValue dv) throws ConflictException, BadRequestException {
    if (isNullOrEmpty(dv.getValue()) && isNullOrEmpty(dv.getComment())) dv.setDeleted(true);
    dataEntryService.upsertValue(
        false, null, dataEntryService.decodeValue(null, toDataEntryValue(dv)));
  }

  @Override
  @IndirectTransactional
  public void updateDataValues(List<DataValue> dataValues)
      throws ConflictException, BadRequestException {
    dataEntryService.upsertGroup(
        new DataEntryGroup.Options(false, true, false),
        dataEntryService.decodeGroup(
            new DataEntryGroup.Input(dataValues.stream().map(this::toDataEntryValue).toList())),
        transitory());
  }

  @Override
  @IndirectTransactional
  public void deleteDataValue(DataValue dataValue) throws ConflictException, BadRequestException {
    DataEntryKey key =
        new DataEntryKey(
            UID.of(dataValue.getDataElement()),
            UID.of(dataValue.getSource()),
            UID.of(dataValue.getCategoryOptionCombo()),
            UID.of(dataValue.getAttributeOptionCombo()),
            dataValue.getPeriod().getIsoDate());
    dataEntryService.deleteValue(false, null, key);
  }

  @Override
  @Transactional
  public void deleteDataValues(OrganisationUnit organisationUnit) {
    dataValueStore.deleteDataValues(organisationUnit);
  }

  @Override
  @Transactional
  public void deleteDataValues(DataElement dataElement) {
    dataValueStore.deleteDataValues(dataElement);
  }

  @Override
  @Transactional(readOnly = true)
  public DataValue getDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo) {
    CategoryOptionCombo defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();

    return dataValueStore.getDataValue(
        dataElement, period, source, categoryOptionCombo, defaultOptionCombo);
  }

  @Override
  @Transactional(readOnly = true)
  public DataValue getDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo) {
    return dataValueStore.getDataValue(
        dataElement, period, source, categoryOptionCombo, attributeOptionCombo);
  }

  // -------------------------------------------------------------------------
  // Collections of DataValues
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<DataValue> getDataValues(DataExportParams params) {
    validate(params);

    return dataValueStore.getDataValues(params);
  }

  @Override
  public void validate(DataExportParams params) throws IllegalQueryException {
    ErrorMessage error = null;

    if (params == null) {
      throw new IllegalQueryException(ErrorCode.E2000);
    }

    if (!params.hasDataElements() && !params.hasDataSets() && !params.hasDataElementGroups()) {
      error = new ErrorMessage(ErrorCode.E2001);
    }

    if (!params.hasPeriods()
        && !params.hasStartEndDate()
        && !params.hasLastUpdated()
        && !params.hasLastUpdatedDuration()) {
      error = new ErrorMessage(ErrorCode.E2002);
    }

    if (params.hasPeriods() && params.hasStartEndDate()) {
      error = new ErrorMessage(ErrorCode.E2003);
    }

    if (params.hasStartEndDate() && params.getStartDate().after(params.getEndDate())) {
      error = new ErrorMessage(ErrorCode.E2004);
    }

    if (params.hasLastUpdatedDuration()
        && DateUtils.getDuration(params.getLastUpdatedDuration()) == null) {
      error = new ErrorMessage(ErrorCode.E2005);
    }

    if (!params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2006);
    }

    if (params.isIncludeDescendants() && params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2007);
    }

    if (params.isIncludeDescendants() && !params.hasOrganisationUnits()) {
      error = new ErrorMessage(ErrorCode.E2008);
    }

    if (params.hasLimit() && params.getLimit() < 0) {
      error = new ErrorMessage(ErrorCode.E2009, params.getLimit());
    }

    if (error != null) {
      log.warn("Validation failed: " + error);

      throw new IllegalQueryException(error);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValue> getAllDataValues() {
    return dataValueStore.getAllDataValues();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DeflatedDataValue> getDeflatedDataValues(DataExportParams params) {
    return dataValueStore.getDeflatedDataValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public int getDataValueCount(int days) {
    Calendar cal = PeriodType.createCalendarInstance();
    cal.add(Calendar.DAY_OF_YEAR, (days * -1));

    return dataValueStore.getDataValueCountLastUpdatedBetween(cal.getTime(), null, false);
  }

  @Override
  @Transactional(readOnly = true)
  public int getDataValueCountLastUpdatedAfter(Date date, boolean includeDeleted) {
    return dataValueStore.getDataValueCountLastUpdatedBetween(date, null, includeDeleted);
  }

  @Override
  @Transactional(readOnly = true)
  public int getDataValueCountLastUpdatedBetween(
      Date startDate, Date endDate, boolean includeDeleted) {
    return dataValueStore.getDataValueCountLastUpdatedBetween(startDate, endDate, includeDeleted);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean dataValueExists(CategoryCombo combo) {
    return dataValueStore.dataValueExists(combo);
  }

  @Override
  public boolean dataValueExistsForDataElement(@Nonnull UID uid) {
    return dataValueStore.dataValueExistsForDataElement(uid.getValue());
  }
}

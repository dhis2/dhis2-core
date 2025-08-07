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
package org.hisp.dhis.webapi.controller.dataentry;

import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.webapi.webdomain.dataentry.DataEntryDtoMapper.toDto;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.datavalue.DataEntryKey;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.webapi.controller.datavalue.DataValidator;
import org.hisp.dhis.webapi.webdomain.dataentry.CompleteStatusDto;
import org.hisp.dhis.webapi.webdomain.datavalue.DataSetValueQueryParams;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueDtoMapper;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValuesDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = DataValue.class,
    classifiers = {"team:platform", "purpose:data"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dataEntry")
public class DataSetValueController {
  private final DataValueService dataValueService;

  private final MinMaxDataElementService minMaxValueService;

  private final DataEntryService dataEntryService;

  private final CompleteDataSetRegistrationService registrationService;

  private final DataValidator dataValidator;

  @GetMapping("/dataValues")
  public DataValuesDto getDataValueSet(DataSetValueQueryParams params) throws ConflictException {
    DataSet ds = dataValidator.getAndValidateDataSet(params.getDs());
    Period pe = dataValidator.getAndValidatePeriod(params.getPe());
    OrganisationUnit ou = dataValidator.getAndValidateOrganisationUnit(params.getOu());
    CategoryOptionCombo ao =
        dataValidator.getAndValidateAttributeOptionCombo(params.getCc(), params.getCp());

    DataExportParams exportParams =
        new DataExportParams()
            .setDataSets(Set.of(ds))
            .setPeriods(Set.of(pe))
            .setOrganisationUnits(Set.of(ou))
            .setAttributeOptionCombos(Set.of(ao));

    List<DataValue> dataValues = dataValueService.getDataValues(exportParams);

    List<MinMaxDataElement> minMaxValues =
        minMaxValueService.getMinMaxDataElements(ou, ds.getDataElements());

    // de is not relevant but required, so we use any of the set
    UID de = UID.of(ds.getDataElements().iterator().next());
    LockStatus lockStatus =
        dataEntryService.getEntryStatus(
            UID.of(ds), new DataEntryKey(de, UID.of(ou), null, UID.of(ao), pe.getIsoDate()));

    CompleteDataSetRegistration registration =
        registrationService.getCompleteDataSetRegistration(ds, pe, ou, ao);

    return new DataValuesDto()
        .setDataValues(mapToList(dataValues, DataValueDtoMapper::toDto))
        .setMinMaxValues(mapToList(minMaxValues, DataValueDtoMapper::toDto))
        .setLockStatus(lockStatus)
        .setCompleteStatus(registration != null ? toDto(registration) : new CompleteStatusDto());
  }
}

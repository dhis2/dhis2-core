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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1007;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1009;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1076;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1084;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1303;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.needsToValidateDataValues;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateMandatoryDataValue;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateOptionSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors;

/**
 * @author Enrico Colasante
 */
class DataValuesValidator implements Validator<Event> {
  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());

    checkNotNull(programStage, TrackerImporterAssertErrors.PROGRAM_STAGE_CANT_BE_NULL);

    for (DataValue dataValue : event.getDataValues()) {
      // event dates (createdAt, updatedAt) are ignored and set by the
      // system
      TrackerPreheat preheat = bundle.getPreheat();
      DataElement dataElement = preheat.getDataElement(dataValue.getDataElement());

      if (dataElement == null) {
        reporter.addError(event, ValidationCode.E1304, dataValue.getDataElement());
        continue;
      }

      validateDataValue(reporter, bundle, dataElement, dataValue, programStage, event);
    }

    validateMandatoryDataValues(reporter, bundle, event);
    validateDataValueDataElementIsConnectedToProgramStage(reporter, bundle, event, programStage);
  }

  private void validateMandatoryDataValues(Reporter reporter, TrackerBundle bundle, Event event) {
    if (event.getProgramStage().isBlank()) {
      return;
    }

    TrackerPreheat preheat = bundle.getPreheat();
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());
    final List<MetadataIdentifier> mandatoryDataElements =
        programStage.getProgramStageDataElements().stream()
            .filter(ProgramStageDataElement::isCompulsory)
            .map(de -> preheat.getIdSchemes().toMetadataIdentifier(de.getDataElement()))
            .collect(Collectors.toList());
    List<MetadataIdentifier> missingDataValue =
        validateMandatoryDataValue(programStage, event, mandatoryDataElements);
    missingDataValue.forEach(de -> reporter.addError(event, E1303, de));
  }

  private void validateDataValue(
      Reporter reporter,
      TrackerBundle bundle,
      DataElement dataElement,
      DataValue dataValue,
      ProgramStage programStage,
      Event event) {
    String status = null;

    if (dataElement.getValueType() == null) {
      reporter.addError(
          event, ValidationCode.E1302, dataElement.getUid(), "data_element_or_type_null_or_empty");
    } else if (dataElement.hasOptionSet()) {
      validateOptionSet(reporter, event, dataElement, dataValue.getValue());
    } else if (dataElement.getValueType().isFile()) {
      validateFileNotAlreadyAssigned(reporter, bundle, event, dataValue, dataElement);
    } else if (dataElement.getValueType().isOrganisationUnit()) {
      validateOrgUnitValueType(reporter, bundle, event, dataValue, dataElement);
    } else {
      status = ValidationUtils.valueIsValid(dataValue.getValue(), dataElement);
    }

    if (status != null) {
      reporter.addError(event, ValidationCode.E1302, dataElement.getUid(), status);
    } else {
      validateNullDataValues(reporter, dataElement, programStage, dataValue, event);
    }
  }

  private void validateNullDataValues(
      Reporter reporter,
      DataElement dataElement,
      ProgramStage programStage,
      DataValue dataValue,
      Event event) {
    if (dataValue.getValue() != null || !needsToValidateDataValues(event, programStage)) {
      return;
    }

    Optional<ProgramStageDataElement> optionalPsde =
        Optional.of(programStage)
            .map(ps -> ps.getProgramStageDataElements().stream())
            .flatMap(
                psdes ->
                    psdes
                        .filter(
                            psde ->
                                psde.getDataElement().getUid().equals(dataElement.getUid())
                                    && psde.isCompulsory())
                        .findFirst());

    if (optionalPsde.isPresent()) {
      reporter.addError(event, E1076, DataElement.class.getSimpleName(), dataElement.getUid());
    }
  }

  private void validateDataValueDataElementIsConnectedToProgramStage(
      Reporter reporter, TrackerBundle bundle, Event event, ProgramStage programStage) {
    TrackerPreheat preheat = bundle.getPreheat();
    final Set<MetadataIdentifier> dataElements =
        programStage.getProgramStageDataElements().stream()
            .map(de -> preheat.getIdSchemes().toMetadataIdentifier(de.getDataElement()))
            .collect(Collectors.toSet());

    Set<MetadataIdentifier> payloadDataElements =
        event.getDataValues().stream().map(DataValue::getDataElement).collect(Collectors.toSet());

    for (MetadataIdentifier payloadDataElement : payloadDataElements) {
      if (!dataElements.contains(payloadDataElement)) {
        reporter.addError(event, ValidationCode.E1305, payloadDataElement, programStage.getUid());
      }
    }
  }

  private void validateFileNotAlreadyAssigned(
      Reporter reporter,
      TrackerBundle bundle,
      Event event,
      DataValue dataValue,
      DataElement dataElement) {
    boolean isFile = dataElement.getValueType() != null && dataElement.getValueType().isFile();

    if (dataValue == null || dataValue.getValue() == null || !isFile) {
      return;
    }

    TrackerPreheat preheat = bundle.getPreheat();
    FileResource fileResource = preheat.get(FileResource.class, dataValue.getValue());

    reporter.addErrorIfNull(fileResource, event, E1084, dataValue.getValue());

    if (bundle.getStrategy(event).isCreate()) {
      reporter.addErrorIf(
          () -> fileResource != null && fileResource.isAssigned(),
          event,
          E1009,
          dataValue.getValue());
    }

    if (bundle.getStrategy(event).isUpdate()) {
      reporter.addErrorIf(
          () ->
              fileResource != null
                  && fileResource.getFileResourceOwner() != null
                  && !fileResource.getFileResourceOwner().equals(event.getEvent()),
          event,
          E1009,
          dataValue.getValue());
    }
  }

  private void validateOrgUnitValueType(
      Reporter reporter,
      TrackerBundle bundle,
      Event event,
      DataValue dataValue,
      DataElement dataElement) {
    boolean isOrgUnit =
        dataElement.getValueType() != null && dataElement.getValueType().isOrganisationUnit();

    if (dataValue == null || dataValue.getValue() == null || !isOrgUnit) {
      return;
    }

    reporter.addErrorIfNull(
        bundle.getPreheat().getOrganisationUnit(dataValue.getValue()),
        event,
        E1007,
        dataValue.getValue());
  }
}

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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1007;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1009;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1076;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1084;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1302;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1303;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1304;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1305;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateDeletionMandatoryDataValue;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateMandatoryDataValue;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateOptionSet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.OptionService;
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

/**
 * @author Enrico Colasante
 */
@RequiredArgsConstructor
class DataValuesValidator implements Validator<Event> {

  private final OptionService optionService;

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, Event event) {
    ProgramStage programStage = bundle.getPreheat().getProgramStage(event.getProgramStage());

    for (DataValue dataValue : event.getDataValues()) {
      validateDataValue(reporter, bundle, dataValue, event);
    }

    validateEventStatus(reporter, event);
    validateMandatoryDataValues(reporter, bundle, event, programStage);
    validateDataValueDataElementIsConnectedToProgramStage(reporter, bundle, event, programStage);
  }

  private void validateEventStatus(Reporter reporter, Event event) {
    if (EventStatus.STATUSES_WITHOUT_DATA_VALUES.contains(event.getStatus())
        && !event.getDataValues().isEmpty()) {
      reporter.addError(
          event,
          ValidationCode.E1315,
          event.getStatus().name(),
          StringUtils.join(EventStatus.STATUSES_WITH_DATA_VALUES));
    }
  }

  private void validateMandatoryDataValues(
      Reporter reporter, TrackerBundle bundle, Event event, ProgramStage programStage) {
    final List<MetadataIdentifier> mandatoryDataElements =
        programStage.getProgramStageDataElements().stream()
            .filter(ProgramStageDataElement::isCompulsory)
            .map(de -> bundle.getPreheat().getIdSchemes().toMetadataIdentifier(de.getDataElement()))
            .toList();

    validateMandatoryDataValue(bundle, event, programStage, mandatoryDataElements)
        .forEach(de -> reporter.addError(event, E1303, de));
    validateDeletionMandatoryDataValue(event, programStage, mandatoryDataElements)
        .forEach(de -> reporter.addError(event, E1076, DataElement.class.getSimpleName(), de));
  }

  private void validateDataValue(
      Reporter reporter, TrackerBundle bundle, DataValue dataValue, Event event) {
    DataElement dataElement = bundle.getPreheat().getDataElement(dataValue.getDataElement());

    if (dataElement == null) {
      reporter.addError(event, E1304, dataValue.getDataElement());
      return;
    }

    if (dataValue.getValue() == null) {
      return;
    }

    if (dataElement.hasOptionSet()) {
      validateOptionSet(reporter, event, dataElement, dataValue.getValue(), optionService);
    } else if (dataElement.getValueType().isFile()) {
      validateFileNotAlreadyAssigned(reporter, bundle, event, dataValue.getValue());
    } else if (dataElement.getValueType().isOrganisationUnit()) {
      validateOrgUnitValueType(reporter, bundle, event, dataValue.getValue());
    } else {
      String status = ValidationUtils.valueIsValid(dataValue.getValue(), dataElement);
      if (status != null) {
        reporter.addError(event, E1302, dataElement.getUid(), status);
      }
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
        reporter.addError(event, E1305, payloadDataElement, programStage.getUid());
      }
    }
  }

  private void validateFileNotAlreadyAssigned(
      Reporter reporter, TrackerBundle bundle, Event event, @Nonnull String value) {
    FileResource fileResource = bundle.getPreheat().get(FileResource.class, value);

    reporter.addErrorIfNull(fileResource, event, E1084, value);

    if (bundle.getStrategy(event).isCreate()) {
      reporter.addErrorIf(
          () -> fileResource != null && fileResource.isAssigned(), event, E1009, value);
    }

    if (bundle.getStrategy(event).isUpdate()) {
      reporter.addErrorIf(
          () ->
              fileResource != null
                  && fileResource.getFileResourceOwner() != null
                  && !fileResource.getFileResourceOwner().equals(event.getEvent()),
          event,
          E1009,
          value);
    }
  }

  private void validateOrgUnitValueType(
      Reporter reporter, TrackerBundle bundle, Event event, @Nonnull String value) {
    reporter.addErrorIfNull(bundle.getPreheat().getOrganisationUnit(value), event, E1007, value);
  }
}

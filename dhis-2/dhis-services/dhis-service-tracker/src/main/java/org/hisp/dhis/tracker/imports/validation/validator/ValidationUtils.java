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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class ValidationUtils {
  private ValidationUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static void validateGeometry(
      Reporter reporter, TrackerDto dto, @Nonnull Geometry geometry, FeatureType featureType) {

    if (featureType == null) {
      reporter.addError(dto, ValidationCode.E1074);
      return;
    }

    FeatureType typeFromName = FeatureType.getTypeFromName(geometry.getGeometryType());

    if (FeatureType.NONE == featureType || featureType != typeFromName) {
      reporter.addError(dto, ValidationCode.E1012, featureType.name());
    }
  }

  public static List<Note> validateNotes(
      Reporter reporter, TrackerPreheat preheat, TrackerDto dto, List<Note> notesToCheck) {
    final List<Note> notes = new ArrayList<>();
    for (Note note : notesToCheck) {
      if (isNotEmpty(note.getValue())) // Ignore notes with no text
      {
        // If a note having the same UID already exist in the db, raise
        // warning, ignore the note and continue
        if (isNotEmpty(note.getNote()) && preheat.hasNote(note.getNote())) {
          reporter.addWarning(dto, ValidationCode.E1119, note.getNote());
        } else {
          notes.add(note);
        }
      }
    }
    return notes;
  }

  public static List<MetadataIdentifier> validateDeletionMandatoryDataValue(
      Event event,
      @Nonnull ProgramStage programStage,
      List<MetadataIdentifier> mandatoryDataElements) {
    if (!needsToValidateDataValues(event, programStage)) {
      return List.of();
    }

    Set<MetadataIdentifier> eventDataElements =
        event.getDataValues().stream()
            .filter(dv -> dv.getValue() == null)
            .map(DataValue::getDataElement)
            .collect(Collectors.toSet());

    return mandatoryDataElements.stream().filter(eventDataElements::contains).toList();
  }

  public static List<MetadataIdentifier> validateMandatoryDataValue(
      TrackerBundle bundle,
      Event event,
      @Nonnull ProgramStage programStage,
      List<MetadataIdentifier> mandatoryDataElements) {
    if (!needsToValidateDataValues(event, programStage)) {
      return List.of();
    }

    Set<MetadataIdentifier> eventDataElements = getEventDataValues(bundle, event);

    return mandatoryDataElements.stream().filter(de -> !eventDataElements.contains(de)).toList();
  }

  private static Set<MetadataIdentifier> getEventDataValues(TrackerBundle bundle, Event event) {
    Stream<MetadataIdentifier> payloadDataValues =
        event.getDataValues().stream().map(DataValue::getDataElement);
    Stream<MetadataIdentifier> savedDataValues =
        Optional.ofNullable(bundle.getPreheat().getEvent(event.getUid()))
            .map(org.hisp.dhis.program.Event::getEventDataValues)
            .orElse(Set.of())
            .stream()
            .map(dv -> MetadataIdentifier.ofUid(dv.getDataElement()));
    return Stream.concat(payloadDataValues, savedDataValues).collect(Collectors.toSet());
  }

  public static boolean needsToValidateDataValues(Event event, @Nonnull ProgramStage programStage) {
    if (EventStatus.STATUSES_WITHOUT_DATA_VALUES.contains(event.getStatus())) {
      return false;
    } else if (ValidationStrategy.ON_COMPLETE.equals(programStage.getValidationStrategy())
        && EventStatus.COMPLETED.equals(event.getStatus())) {
      return true;
    } else {
      return !ValidationStrategy.ON_COMPLETE.equals(programStage.getValidationStrategy());
    }
  }

  public static Set<MetadataIdentifier> getTrackedEntityAttributes(
      TrackerBundle bundle, UID trackedEntityUid) {
    TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
    Set<MetadataIdentifier> savedTrackedEntityAttributes =
        Optional.of(bundle)
            .map(TrackerBundle::getPreheat)
            .map(trackerPreheat -> trackerPreheat.getTrackedEntity(trackedEntityUid))
            .map(TrackedEntity::getTrackedEntityAttributeValues)
            .orElse(Collections.emptySet())
            .stream()
            .map(TrackedEntityAttributeValue::getAttribute)
            .map(idSchemes::toMetadataIdentifier)
            .collect(Collectors.toSet());
    Set<MetadataIdentifier> payloadTrackedEntityAttributes =
        bundle
            .findTrackedEntityByUid(trackedEntityUid)
            .map(org.hisp.dhis.tracker.imports.domain.TrackedEntity::getAttributes)
            .orElse(List.of())
            .stream()
            .map(Attribute::getAttribute)
            .collect(Collectors.toSet());
    return Stream.concat(
            savedTrackedEntityAttributes.stream(), payloadTrackedEntityAttributes.stream())
        .collect(Collectors.toSet());
  }

  public static void addIssuesToReporter(
      Reporter reporter, TrackerDto dto, List<ProgramRuleIssue> programRuleIssues) {
    programRuleIssues.stream()
        .filter(issue -> ERROR.equals(issue.getIssueType()))
        .forEach(
            issue -> {
              List<String> args = Lists.newArrayList(issue.getRuleUid().getValue());
              args.addAll(issue.getArgs());
              reporter.addError(dto, issue.getIssueCode(), args.toArray());
            });

    programRuleIssues.stream()
        .filter(issue -> WARNING.equals(issue.getIssueType()))
        .forEach(
            issue -> {
              List<String> args = Lists.newArrayList(issue.getRuleUid().getValue());
              args.addAll(issue.getArgs());
              reporter.addWarning(dto, issue.getIssueCode(), args.toArray());
            });
  }

  public static boolean trackedEntityExists(TrackerBundle bundle, UID te) {
    return bundle.getPreheat().getTrackedEntity(te) != null
        || bundle.findTrackedEntityByUid(te).isPresent();
  }

  public static boolean enrollmentExist(TrackerBundle bundle, UID enrollment) {
    return bundle.getPreheat().getEnrollment(enrollment) != null
        || bundle.findEnrollmentByUid(enrollment).isPresent();
  }

  public static boolean eventExist(TrackerBundle bundle, UID event) {
    return bundle.getPreheat().getEvent(event) != null || bundle.findEventByUid(event).isPresent();
  }

  public static <T extends ValueTypedDimensionalItemObject> void validateOptionSet(
      Reporter reporter, TrackerDto dto, T optionalObject, @Nonnull String value) {
    if (!optionalObject.hasOptionSet()) {
      return;
    }

    boolean isValid;

    if (optionalObject.getValueType().isMultiText()) {
      isValid = optionalObject.getOptionSet().hasAllOptions(ValueType.splitMultiText(value));
    } else {
      isValid = optionalObject.getOptionSet().getOptionByCode(value) != null;
    }

    if (!isValid) {
      reporter.addError(dto, ValidationCode.E1125, value, optionalObject.getOptionSet().getUid());
    }
  }

  public static void validateNotesUid(List<Note> notes, Reporter reporter, TrackerDto dto) {
    for (Note note : notes) {
      checkUidFormat(note.getNote(), reporter, dto, note, note.getNote());
    }
  }

  /**
   * Check if the given UID has a valid format.
   *
   * @param checkUid a UID to be checked
   * @param reporter a {@see Reporter} to which the error is added
   * @param dto the dto to which the report will be linked to
   * @param args list of arguments for the Error report
   */
  public static void checkUidFormat(
      String checkUid, Reporter reporter, TrackerDto dto, Object... args) {
    if (!CodeGenerator.isValidUid(checkUid)) {
      reporter.addError(dto, ValidationCode.E1048, checkUid, args[0], args[1]);
    }
  }
}

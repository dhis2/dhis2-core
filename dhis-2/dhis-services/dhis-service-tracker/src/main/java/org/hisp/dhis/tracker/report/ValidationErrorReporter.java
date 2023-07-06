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
package org.hisp.dhis.tracker.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import lombok.Data;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

/**
 * A class that collects {@link TrackerErrorReport} during the validation process.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
// TODO: should this be "ValidationReporter" since it does not only report
// errors ?
public class ValidationErrorReporter {
  private final List<TrackerErrorReport> reportList;

  private final List<TrackerWarningReport> warningsReportList;

  private final boolean isFailFast;

  private final TrackerBundle bundle;

  /*
   * A map that keep tracks of all the invalid Tracker objects encountered
   * during the validation process
   */
  private Map<TrackerType, List<String>> invalidDTOs;

  public static ValidationErrorReporter emptyReporter() {
    return new ValidationErrorReporter();
  }

  private ValidationErrorReporter() {
    this.warningsReportList = new ArrayList<>();
    this.reportList = new ArrayList<>();
    this.isFailFast = false;
    this.bundle = null;
    this.invalidDTOs = new HashMap<>();
  }

  public ValidationErrorReporter(TrackerBundle bundle) {
    this.reportList = new ArrayList<>();
    this.warningsReportList = new ArrayList<>();
    this.isFailFast = bundle.getValidationMode() == ValidationMode.FAIL_FAST;
    this.bundle = bundle;
    this.invalidDTOs = new HashMap<>();
  }

  public boolean hasErrors() {
    return !this.reportList.isEmpty();
  }

  public boolean hasErrorReport(Predicate<TrackerErrorReport> test) {
    return reportList.stream().anyMatch(test);
  }

  public boolean hasWarningReport(Predicate<TrackerWarningReport> test) {
    return warningsReportList.stream().anyMatch(test);
  }

  public boolean hasWarnings() {
    return !this.warningsReportList.isEmpty();
  }

  public void addError(TrackerDto dto, TrackerErrorCode code, Object... args) {
    TrackerErrorReport error =
        TrackerErrorReport.builder()
            .uid(dto.getUid())
            .trackerType(dto.getTrackerType())
            .errorCode(code)
            .addArgs(args)
            .build(bundle);
    addError(error);
  }

  public void addError(TrackerErrorReport error) {
    getReportList().add(error);
    this.invalidDTOs
        .computeIfAbsent(error.getTrackerType(), k -> new ArrayList<>())
        .add(error.getUid());

    if (isFailFast()) {
      throw new ValidationFailFastException(getReportList());
    }
  }

  public void addWarning(TrackerWarningReport warning) {
    getWarningsReportList().add(warning);
  }

  /** Checks if the provided uid and Tracker Type is part of the invalid entities */
  public boolean isInvalid(TrackerType trackerType, String uid) {
    return this.invalidDTOs.getOrDefault(trackerType, new ArrayList<>()).contains(uid);
  }

  public boolean isInvalid(TrackerDto dto) {
    return this.isInvalid(dto.getTrackerType(), dto.getUid());
  }

  public void addWarning(TrackerDto dto, TrackerErrorCode code, Object... args) {
    TrackerWarningReport warn =
        TrackerWarningReport.builder()
            .uid(dto.getUid())
            .trackerType(dto.getTrackerType())
            .warningCode(code)
            .addArgs(args)
            .build(bundle);
    addWarning(warn);
  }

  public void addErrorIf(
      BooleanSupplier expression, TrackerDto dto, TrackerErrorCode code, Object... args) {
    if (expression.getAsBoolean()) {
      addError(dto, code, args);
    }
  }

  public void addErrorIfNull(Object object, TrackerDto dto, TrackerErrorCode code, Object... args) {
    if (object == null) {
      addError(dto, code, args);
    }
  }
}

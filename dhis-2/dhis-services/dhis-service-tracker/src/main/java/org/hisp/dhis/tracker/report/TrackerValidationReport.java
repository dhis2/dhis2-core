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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ToString
@EqualsAndHashCode
public class TrackerValidationReport {
  @JsonProperty private final List<TrackerErrorReport> errorReports;

  @JsonProperty private final List<TrackerWarningReport> warningReports;

  @JsonIgnore private final List<Timing> timings;

  /*
   * Keeps track of all the invalid Tracker objects (i.e. objects with at
   * least one TrackerErrorReport in the TrackerValidationReport) encountered
   * during the validation process.
   */
  @JsonIgnore private final Map<TrackerType, List<String>> invalidDTOs;

  public TrackerValidationReport() {
    this.errorReports = new ArrayList<>();
    this.warningReports = new ArrayList<>();
    this.timings = new ArrayList<>();
    this.invalidDTOs = new HashMap<>();
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  public void addValidationReport(TrackerValidationReport report) {
    addErrors(report.getErrors());
    addWarnings(report.getWarnings());
    addTimings(report.getTimings());
  }

  public List<TrackerErrorReport> getErrors() {
    return Collections.unmodifiableList(errorReports);
  }

  public List<TrackerWarningReport> getWarnings() {
    return Collections.unmodifiableList(warningReports);
  }

  public List<Timing> getTimings() {
    return Collections.unmodifiableList(timings);
  }

  public TrackerValidationReport addError(TrackerErrorReport error) {
    addErrorIfNotExisting(error);
    return this;
  }

  public TrackerValidationReport addErrors(List<TrackerErrorReport> errors) {
    for (TrackerErrorReport error : errors) {
      addErrorIfNotExisting(error);
    }
    return this;
  }

  public TrackerValidationReport addWarning(TrackerWarningReport warning) {
    addWarningIfNotExisting(warning);
    return this;
  }

  public TrackerValidationReport addWarnings(List<TrackerWarningReport> warnings) {
    for (TrackerWarningReport warning : warnings) {
      addWarningIfNotExisting(warning);
    }
    return this;
  }

  public TrackerValidationReport addTiming(Timing timing) {
    timings.add(timing);
    return this;
  }

  public TrackerValidationReport addTimings(List<Timing> timings) {
    this.timings.addAll(timings);
    return this;
  }

  public boolean hasErrors() {
    return !errorReports.isEmpty();
  }

  public boolean hasError(Predicate<TrackerErrorReport> test) {
    return errorReports.stream().anyMatch(test);
  }

  public boolean hasWarnings() {
    return !warningReports.isEmpty();
  }

  public boolean hasWarning(Predicate<TrackerWarningReport> test) {
    return warningReports.stream().anyMatch(test);
  }

  public boolean hasTimings() {
    return !timings.isEmpty();
  }

  /** Returns the size of all the Tracker DTO that did not pass validation */
  public long size() {

    return this.getErrors().stream().map(TrackerErrorReport::getUid).distinct().count();
  }

  private void addErrorIfNotExisting(TrackerErrorReport error) {
    if (!this.errorReports.contains(error)) {
      this.errorReports.add(error);
      this.invalidDTOs
          .computeIfAbsent(error.getTrackerType(), k -> new ArrayList<>())
          .add(error.getUid());
    }
  }

  private void addWarningIfNotExisting(TrackerWarningReport warning) {
    if (!this.warningReports.contains(warning)) {
      this.warningReports.add(warning);
    }
  }

  /**
   * Checks if a TrackerDto with given type and uid is invalid (i.e. has at least one
   * TrackerErrorReport in the TrackerValidationReport).
   */
  public boolean isInvalid(TrackerType type, String uid) {
    return this.invalidDTOs.getOrDefault(type, new ArrayList<>()).contains(uid);
  }

  /**
   * Checks if the given TrackerDto is invalid (i.e. has at least one TrackerErrorReport in the
   * TrackerValidationReport).
   */
  public boolean isInvalid(TrackerDto dto) {
    return this.isInvalid(dto.getTrackerType(), dto.getUid());
  }
}

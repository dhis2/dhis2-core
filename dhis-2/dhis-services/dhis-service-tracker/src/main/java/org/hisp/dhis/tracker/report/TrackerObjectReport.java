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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.hisp.dhis.tracker.TrackerType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
public class TrackerObjectReport {
  /** Type of object this {@link TrackerObjectReport} represents. */
  @JsonProperty private final TrackerType trackerType;

  /** Index into list. */
  @JsonProperty private Integer index;

  /** UID of object (if object is id object). */
  @JsonProperty private String uid;

  private Map<TrackerErrorCode, List<TrackerErrorReport>> errorReportsByCode = new HashMap<>();

  public TrackerObjectReport(TrackerType trackerType) {
    this.trackerType = trackerType;
  }

  public TrackerObjectReport(TrackerType trackerType, String uid, Integer index) {
    this.trackerType = trackerType;
    this.uid = uid;
    this.index = index;
  }

  @JsonCreator
  public TrackerObjectReport(
      @JsonProperty("trackerType") TrackerType trackerType,
      @JsonProperty("uid") String uid,
      @JsonProperty("index") Integer index,
      @JsonProperty("errorReports") List<TrackerErrorReport> errorReports) {
    this.trackerType = trackerType;
    this.uid = uid;
    this.index = index;
    if (errorReports != null) {
      List<TrackerErrorReport> errorCodeReportList;
      for (TrackerErrorReport errorReport : errorReports) {
        errorCodeReportList = this.errorReportsByCode.get(errorReport.getErrorCode());

        if (errorCodeReportList == null) {
          errorCodeReportList = new ArrayList<>();
        }
        errorCodeReportList.add(errorReport);
        this.errorReportsByCode.put(errorReport.getErrorCode(), errorCodeReportList);
      }
    }
  }

  @JsonProperty
  public List<TrackerErrorReport> getErrorReports() {
    List<TrackerErrorReport> errorReports = new ArrayList<>();
    errorReportsByCode.values().forEach(errorReports::addAll);

    return errorReports;
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  public boolean isEmpty() {
    return errorReportsByCode.isEmpty();
  }

  public int size() {
    return errorReportsByCode.size();
  }

  public List<TrackerErrorCode> getErrorCodes() {
    return new ArrayList<>(errorReportsByCode.keySet());
  }
}

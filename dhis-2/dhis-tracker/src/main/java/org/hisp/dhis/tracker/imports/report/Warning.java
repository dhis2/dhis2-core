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
package org.hisp.dhis.tracker.imports.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hisp.dhis.common.UID;

/**
 * @author Enrico Colasante
 */
@Value
@Builder
public class Warning {
  String warningMessage;

  String warningCode;

  String trackerType;

  UID uid;

  @JsonCreator
  public Warning(
      @JsonProperty("message") String warningMessage,
      @JsonProperty("errorCode") String warningCode,
      @JsonProperty("trackerType") String trackerType,
      @JsonProperty("uid") UID uid) {
    this.warningMessage = warningMessage;
    this.warningCode = warningCode;
    this.trackerType = trackerType;
    this.uid = uid;
  }

  @JsonProperty
  public String getWarningCode() {
    return warningCode;
  }

  @JsonProperty
  public String getMessage() {
    return warningMessage;
  }

  @JsonProperty
  public String getTrackerType() {
    return trackerType;
  }

  @JsonProperty
  public UID getUid() {
    return uid;
  }

  @Override
  public String toString() {
    return "TrackerWarningReport{"
        + "message="
        + warningMessage
        + ", warningCode="
        + warningCode
        + '}';
  }
}

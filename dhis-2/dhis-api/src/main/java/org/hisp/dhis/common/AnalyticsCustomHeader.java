/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.common;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Map;
import org.hisp.dhis.program.ProgramStage;

/**
 * Data class that encapsulate analytics metadata and header information that can be used by the
 * Metadata engine when calculating Metadata
 *
 * @param key the header key
 * @param value the header value
 */
public record AnalyticsCustomHeader(String key, String value) {

  private static final String EVENT_DATE_HEADER = "EVENT_DATE";
  private static final String SCHEDULED_DATE_HEADER = "SCHEDULED_DATE";
  private static final String EVENT_STATUS_HEADER = "EVENT_STATUS";
  private static final String ORG_UNIT_HEADER = "ou";

  private static final String EVENT_DATE_LABEL = "Event date";
  private static final String SCHEDULED_DATE_LABEL = "Scheduled date";
  private static final String EVENT_STATUS_LABEL = "Event status";
  private static final String ORG_UNIT_LABEL = "Organisation unit";

  /** Map of metadata header keys to their corresponding analytics header keys. */
  private static final Map<String, String> HEADER_KEYS_MAP =
      Map.of(
          EVENT_DATE_HEADER, "eventdate",
          SCHEDULED_DATE_HEADER, "scheduleddate",
          EVENT_STATUS_HEADER, "eventstatus",
          ORG_UNIT_HEADER, "ou");

  public static AnalyticsCustomHeader forEventDate(ProgramStage programStage) {
    String label = firstNonNull(programStage.getExecutionDateLabel(), EVENT_DATE_LABEL);
    return create(programStage, EVENT_DATE_HEADER, label);
  }

  public static AnalyticsCustomHeader forScheduledDate(ProgramStage programStage) {
    String label = firstNonNull(programStage.getDisplayDueDateLabel(), SCHEDULED_DATE_LABEL);
    return create(programStage, SCHEDULED_DATE_HEADER, label);
  }

  public static AnalyticsCustomHeader forEventStatus(ProgramStage programStage) {
    return create(programStage, EVENT_STATUS_HEADER, EVENT_STATUS_LABEL);
  }

  public static AnalyticsCustomHeader forOrgUnit(ProgramStage programStage) {
    return create(programStage, ORG_UNIT_HEADER, ORG_UNIT_LABEL);
  }

  /**
   * Converts a metadata header key to its corresponding analytics header key. If the key contains a
   * prefix (e.g., "stageId.NAME"), the prefix is preserved.
   *
   * @param key the metadata header key
   * @return the analytics header key
   */
  public String headerKey(String key) {
    int dotIndex = key.indexOf('.');
    String prefix = "";
    String lookupKey = key;

    if (dotIndex != -1) {
      prefix = key.substring(0, dotIndex + 1); // "abc."
      lookupKey = key.substring(dotIndex + 1); // "NAME"
    }

    String result = HEADER_KEYS_MAP.getOrDefault(lookupKey, lookupKey);
    return prefix + result;
  }

  private static AnalyticsCustomHeader create(
      ProgramStage programStage, String keySuffix, String label) {
    String programStageName =
        firstNonNull(programStage.getProgramStageLabel(), programStage.getName());
    return new AnalyticsCustomHeader(
        "%s.%s".formatted(programStage.getUid(), keySuffix),
        "%s, %s".formatted(label, programStageName));
  }
}

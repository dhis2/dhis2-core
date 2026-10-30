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
package org.hisp.dhis.analytics.event.data.stage;

import java.util.Locale;

/**
 * Classifies stage-specific header columns for enrollment analytics queries.
 *
 * <p>Stage-specific headers are expected to follow a {@code stageUid.column} format, for example
 * {@code Zj7UnCAulEk.eventdate}.
 */
public final class StageHeaderClassifier {
  public enum StageHeaderType {
    EVENT_DATE,
    SCHEDULED_DATE,
    OU,
    OU_NAME,
    OU_CODE,
    EVENT_STATUS,
    GENERIC_STAGE_ITEM,
    NOT_STAGE_SPECIFIC
  }

  /**
   * Classifies a header column into a stage-specific type.
   *
   * @param header the header column name
   * @return the detected {@link StageHeaderType}
   */
  public StageHeaderType classify(String header) {
    if (header == null || header.isBlank()) {
      return StageHeaderType.NOT_STAGE_SPECIFIC;
    }

    String normalized = normalize(header);
    if (!normalized.contains(".")) {
      return StageHeaderType.NOT_STAGE_SPECIFIC;
    }

    if (normalized.endsWith(".eventdate")) {
      return StageHeaderType.EVENT_DATE;
    }
    if (normalized.endsWith(".scheduleddate")) {
      return StageHeaderType.SCHEDULED_DATE;
    }
    if (normalized.endsWith(".ouname")) {
      return StageHeaderType.OU_NAME;
    }
    if (normalized.endsWith(".oucode")) {
      return StageHeaderType.OU_CODE;
    }
    if (normalized.endsWith(".eventstatus")) {
      return StageHeaderType.EVENT_STATUS;
    }
    if (normalized.endsWith(".ou")) {
      return StageHeaderType.OU;
    }
    return StageHeaderType.GENERIC_STAGE_ITEM;
  }

  /**
   * Returns whether a header uses the stage-specific format.
   *
   * @param header the header column name
   * @return true if the header is stage-specific
   */
  public boolean isStageSpecific(String header) {
    return classify(header) != StageHeaderType.NOT_STAGE_SPECIFIC;
  }

  private String normalize(String header) {
    return header.toLowerCase(Locale.ROOT).replace("\"", "").replace("`", "");
  }
}

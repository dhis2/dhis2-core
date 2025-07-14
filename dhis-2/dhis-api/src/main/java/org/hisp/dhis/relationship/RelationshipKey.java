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
package org.hisp.dhis.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.common.UID;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(staticName = "of")
public class RelationshipKey {

  private static final String RELATIONSHIP_KEY_SEPARATOR = "_";

  private final String type;

  private final RelationshipItemKey from;

  private final RelationshipItemKey to;

  public String asString() {
    return String.join(RELATIONSHIP_KEY_SEPARATOR, type, from.asString(), to.asString());
  }

  public RelationshipKey inverseKey() {
    return toBuilder().from(to).to(from).build();
  }

  @Data
  @Builder
  public static class RelationshipItemKey {
    private final UID trackedEntity;

    private final UID enrollment;

    private final UID event;

    private final UID singleEvent;

    public String asString() {
      if (isTrackedEntity()) {
        return trackedEntity.getValue();
      } else if (isEnrollment()) {
        return enrollment.getValue();
      } else if (isEvent()) {
        return event.getValue();
      } else if (isSingleEvent()) {
        return singleEvent.getValue();
      }

      return "ERROR";
    }

    public boolean isTrackedEntity() {
      return trackedEntity != null;
    }

    public boolean isEnrollment() {
      return enrollment != null;
    }

    public boolean isEvent() {
      return event != null;
    }

    public boolean isSingleEvent() {
      return singleEvent != null;
    }
  }
}

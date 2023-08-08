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
package org.hisp.dhis.tracker.imports.report;

import static org.hisp.dhis.common.OpenApi.Shared.Pattern.TRACKER;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.OpenApi;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Shared(pattern = TRACKER)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stats {
  @JsonProperty @Builder.Default private int created = 0;

  @JsonProperty @Builder.Default private int updated = 0;

  @JsonProperty @Builder.Default private int deleted = 0;

  @JsonProperty @Builder.Default private int ignored = 0;

  @JsonProperty
  public int getTotal() {
    return created + updated + deleted + ignored;
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  public void merge(Stats stats) {
    created += stats.getCreated();
    updated += stats.getUpdated();
    deleted += stats.getDeleted();
    ignored += stats.getIgnored();
  }

  public void ignored() {
    ignored += created;
    ignored += updated;
    ignored += deleted;

    created = 0;
    updated = 0;
    deleted = 0;
  }

  public void incCreated() {
    created++;
  }

  public void incUpdated() {
    updated++;
  }

  public void incDeleted() {
    deleted++;
  }

  public void incIgnored() {
    ignored++;
  }
}

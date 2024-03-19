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
package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Getter
@Setter
public class EventProgramsDataSynchronizationJobParameters implements JobParameters {
  static final int PAGE_SIZE_MIN = 5;

  static final int PAGE_SIZE_MAX = 200;

  @JsonProperty private int pageSize = 60;

  @Override
  public Optional<ErrorReport> validate() {
    if (pageSize < PAGE_SIZE_MIN || pageSize > PAGE_SIZE_MAX) {
      return Optional.of(
          new ErrorReport(
              this.getClass(),
              ErrorCode.E4008,
              "pageSize",
              PAGE_SIZE_MIN,
              PAGE_SIZE_MAX,
              pageSize));
    }

    return Optional.empty();
  }
}

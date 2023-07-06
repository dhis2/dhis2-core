/*
 * Copyright (c) 2004-2023, University of Oslo
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
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * Parameters for the job to test job execution by simulating runs mostly to simulate errors during
 * a run.
 *
 * @author Jan Bernitt
 */
@Getter
@Setter
public class TestJobParameters implements JobParameters {
  /** Initial waiting time in millis (as there is no actual "work" done) */
  @JsonProperty private Long waitMillis;

  /** Number of stages in the job, zero if {@code null} */
  @JsonProperty private Integer stages;

  /** Stage number at which an item fails, none of {@code null} */
  @JsonProperty private Integer failAtStage;

  /** Number of items in each stage, none if {@code null} */
  @JsonProperty private Integer items;

  /** Item number in the failing stage at which the failure occurs, none if {@code null} */
  @JsonProperty private Integer failAtItem;

  /** Duration each item takes in millis, zero if {@code null} */
  @JsonProperty private Long itemDuration;

  /** The message used when failing the item */
  @JsonProperty private String failWithMessage;

  /** When true, an exception is used to fail, otherwise the progress tracking api is used */
  @JsonProperty private boolean failWithException;

  @JsonProperty private boolean runStagesParallel;
}

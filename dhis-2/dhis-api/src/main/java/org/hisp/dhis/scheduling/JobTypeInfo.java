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
package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.schema.Property;

/**
 * Class which represents information about a job type.
 *
 * @author Lars Helge Overland
 */
public class JobTypeInfo {
  private String name;

  private JobType jobType;

  private SchedulingType schedulingType;

  private List<Property> jobParameters = new ArrayList<>();

  /** Default constructor. */
  public JobTypeInfo() {}

  /**
   * Constructor.
   *
   * @param name the job type name.
   * @param jobType the {@link JobType}.
   * @param jobParameters the list of {@link Property}.
   */
  public JobTypeInfo(String name, JobType jobType, List<Property> jobParameters) {
    this.name = name;
    this.jobType = jobType;
    this.schedulingType = jobType.getSchedulingType();
    this.jobParameters = jobParameters;
  }

  @JsonProperty
  public String getName() {
    return name;
  }

  @JsonProperty
  public JobType getJobType() {
    return jobType;
  }

  @JsonProperty
  public SchedulingType getSchedulingType() {
    return schedulingType;
  }

  @JsonProperty
  public List<Property> getJobParameters() {
    return jobParameters;
  }
}

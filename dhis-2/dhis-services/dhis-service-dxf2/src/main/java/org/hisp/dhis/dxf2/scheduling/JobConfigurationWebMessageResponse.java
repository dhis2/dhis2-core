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
package org.hisp.dhis.dxf2.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Date;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.webmessage.WebMessageResponse;

/**
 * @author Henning Håkonsen
 */
public class JobConfigurationWebMessageResponse implements WebMessageResponse {
  private String name;

  private String id;

  private Date created;

  private JobType jobType;

  private JobStatus jobStatus;

  private JobParameters jobParameters;

  private String relativeNotifierEndpoint;

  public JobConfigurationWebMessageResponse(JobConfiguration jobConfiguration) {
    this.name = jobConfiguration.getDisplayName();
    this.id = jobConfiguration.getUid();
    this.created = jobConfiguration.getCreated();
    this.jobType = jobConfiguration.getJobType();
    this.jobStatus = jobConfiguration.getJobStatus();
    this.jobParameters = jobConfiguration.getJobParameters();
    this.relativeNotifierEndpoint = "/api/system/tasks/" + this.jobType + "/" + this.id;
  }

  @JacksonXmlProperty
  @JsonProperty
  public String getName() {
    return name;
  }

  @JacksonXmlProperty
  @JsonProperty
  public String getId() {
    return id;
  }

  @JacksonXmlProperty
  @JsonProperty
  public Date getCreated() {
    return created;
  }

  @JacksonXmlProperty
  @JsonProperty
  public JobType getJobType() {
    return jobType;
  }

  @JacksonXmlProperty
  @JsonProperty
  public JobStatus getJobStatus() {
    return jobStatus;
  }

  @JacksonXmlProperty
  @JsonProperty
  public JobParameters getJobParameters() {
    return jobParameters;
  }

  @JacksonXmlProperty
  @JsonProperty
  public String getRelativeNotifierEndpoint() {
    return relativeNotifierEndpoint;
  }
}

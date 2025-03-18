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
package org.hisp.dhis.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Henning Håkonsen
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class JobConfigurationServiceTest extends PostgresIntegrationTestBase {

  private static final String CRON_EVERY_MIN = "0 * * ? * *";

  @Autowired private JobConfigurationService jobConfigurationService;

  private JobConfiguration jobA;

  private JobConfiguration jobB;

  @BeforeAll
  void setUp() {
    jobA = new JobConfiguration("jobA", JobType.MOCK);
    jobA.setCronExpression(CRON_EVERY_MIN);
    jobA.setSchedulingType(SchedulingType.CRON);
    jobA.setJobParameters(new MockJobParameters("test"));
    jobB = new JobConfiguration("jobB", JobType.DATA_INTEGRITY);
    jobB.setCronExpression(CRON_EVERY_MIN);
    jobB.setSchedulingType(SchedulingType.CRON);
    jobConfigurationService.addJobConfiguration(jobA);
    jobConfigurationService.addJobConfiguration(jobB);
  }

  @Test
  void testGetJobTypeInfo() {
    List<JobTypeInfo> jobTypes = jobConfigurationService.getJobTypeInfo();
    assertNotNull(jobTypes);
    assertFalse(jobTypes.isEmpty());
    JobTypeInfo jobType =
        jobTypes.stream()
            .filter(j -> j.getJobType() == JobType.CONTINUOUS_ANALYTICS_TABLE)
            .findFirst()
            .get();
    assertNotNull(jobType);
    assertEquals(SchedulingType.FIXED_DELAY, jobType.getSchedulingType());
  }

  @Test
  void testGetJob() {
    List<JobConfiguration> jobConfigurationList = jobConfigurationService.getAllJobConfigurations();
    assertEquals(2, jobConfigurationList.size(), "The number of job configurations does not match");
    assertEquals(
        JobType.MOCK, jobConfigurationService.getJobConfigurationByUid(jobA.getUid()).getJobType());
    MockJobParameters jobParameters =
        (MockJobParameters)
            jobConfigurationService.getJobConfigurationByUid(jobA.getUid()).getJobParameters();
    assertNotNull(jobParameters);
    assertEquals("test", jobParameters.getMessage());
    assertEquals(
        JobType.DATA_INTEGRITY,
        jobConfigurationService.getJobConfigurationByUid(jobB.getUid()).getJobType());
    assertNull(jobConfigurationService.getJobConfigurationByUid(jobB.getUid()).getJobParameters());
  }

  @Test
  void testUpdateJob() {
    JobConfiguration test = jobConfigurationService.getJobConfigurationByUid(jobA.getUid());
    test.setName("testUpdate");
    jobConfigurationService.updateJobConfiguration(test);
    assertEquals(
        "testUpdate", jobConfigurationService.getJobConfigurationByUid(jobA.getUid()).getName());
  }

  @Test
  void testDeleteJob() {
    jobConfigurationService.deleteJobConfiguration(jobA);
    assertNull(jobConfigurationService.getJobConfigurationByUid(jobA.getUid()));
  }
}

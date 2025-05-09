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
package org.hisp.dhis.analytics;

import lombok.Getter;

/**
 * @author Lars Helge Overland
 */
@Getter
public enum AnalyticsTableType {
  DATA_VALUE("analytics", true, true),
  COMPLETENESS("analytics_completeness", true, true),
  COMPLETENESS_TARGET("analytics_completenesstarget", false, false),
  ORG_UNIT_TARGET("analytics_orgunittarget", false, false),
  VALIDATION_RESULT("analytics_validationresult", true, false),
  EVENT("analytics_event", false, true),
  ENROLLMENT("analytics_enrollment", false, false),
  OWNERSHIP("analytics_ownership", false, false),
  TRACKED_ENTITY_INSTANCE_EVENTS("analytics_te_event", false, true),
  TRACKED_ENTITY_INSTANCE_ENROLLMENTS("analytics_te_enrollment", false, false),
  TRACKED_ENTITY_INSTANCE("analytics_te", false, false);

  private final String tableName;

  private final boolean periodDimension;

  private final boolean latestPartition;

  AnalyticsTableType(String tableName, boolean periodDimension, boolean latestPartition) {
    this.tableName = tableName;
    this.periodDimension = periodDimension;
    this.latestPartition = latestPartition;
  }
}

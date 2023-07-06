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
package org.hisp.dhis.analytics.event;

import org.hisp.dhis.common.Grid;

/**
 * This interface is responsible for retrieving aggregated event data. Data will be returned in a
 * grid object or as a dimensional key-value mapping.
 *
 * @author Markus Bekken
 */
public interface EnrollmentAnalyticsService {
  String ITEM_TEI = "tei";

  String ITEM_PI = "pi";

  String ITEM_ENROLLMENT_DATE = "enrollmentdate";

  String ITEM_INCIDENT_DATE = "incidentdate";

  String ITEM_STORED_BY = "storedby";

  String ITEM_CREATED_BY_DISPLAY_NAME = "createdbydisplayname";

  String ITEM_LAST_UPDATED_BY_DISPLAY_NAME = "lastupdatedbydisplayname";

  String ITEM_LAST_UPDATED = "lastupdated";

  String ITEM_GEOMETRY = "geometry";

  String ITEM_LONGITUDE = "longitude";

  String ITEM_LATITUDE = "latitude";

  String ITEM_ORG_UNIT_NAME = "ouname";

  String ITEM_ORG_UNIT_CODE = "oucode";

  String ITEM_PROGRAM_STATUS = "programstatus";

  /**
   * Returns a list of enrollments matching the given query.
   *
   * @param params the envent query parameters.
   * @return enrollments with event data as a Grid object.
   */
  Grid getEnrollments(EventQueryParams params);
}

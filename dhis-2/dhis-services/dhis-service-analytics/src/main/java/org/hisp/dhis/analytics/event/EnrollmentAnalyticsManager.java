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
 * @author Markus Bekken
 */
public interface EnrollmentAnalyticsManager {
  /**
   * Retrieves aggregated data based on enrollments.
   *
   * @param params the query to retrieve aggregated data for.
   * @param grid the grid to insert data into.
   * @param maxLimit the max number of records to retrieve.
   * @return a grid with data.
   */
  Grid getAggregatedEventData(EventQueryParams params, Grid grid, int maxLimit);

  /**
   * Retrieves aggregated data based on enrollments.
   *
   * @param params the query to retrieve enrollments for.
   * @param grid the grid to insert data into.
   * @param maxLimit the max number of records to retrieve.
   * @return a grid with data.
   */
  void getEnrollments(EventQueryParams params, Grid grid, int maxLimit);

  /**
   * Retreives count of enrollments based on params.
   *
   * @param params the qyery to count enrollments for,
   * @return number of enrollments macting the parameter criteria.
   */
  long getEnrollmentCount(EventQueryParams params);
}

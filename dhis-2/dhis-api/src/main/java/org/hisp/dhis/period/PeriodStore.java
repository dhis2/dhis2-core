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
package org.hisp.dhis.period;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.GenericStore;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Defines the functionality for persisting Periods and PeriodTypes.
 *
 * @author Torgeir Lorange Ostby
 */
public interface PeriodStore {

  void invalidateCache();

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------

  /**
   * Adds a Period.
   *
   * @param period the Period to add.
   */
  void addPeriod(Period period);

  @CheckForNull
  Period get(long id);

  List<Period> getAll();

  /**
   * Returns a Period.
   *
   * @param startDate the start date of the Period.
   * @param periodType the PeriodType of the Period
   * @return the Period matching the dates and periodtype, or null if no match.
   */
  Period getPeriodFromDates(Date startDate, PeriodType periodType);

  /**
   * Returns all Periods with start date after or equal the specified start date and end date before
   * or equal the specified end date.
   *
   * @param startDate the ultimate start date.
   * @param endDate the ultimate end date.
   * @return a list of all Periods with start date after or equal the specified start date and end
   *     date before or equal the specified end date, or an empty collection if no Periods match.
   */
  List<Period> getPeriodsBetweenDates(Date startDate, Date endDate);

  /**
   * Returns all Periods of the specified PeriodType with start date after or equal the specified
   * start date and end date before or equal the specified end date.
   *
   * @param periodType the PeriodType.
   * @param startDate the ultimate start date.
   * @param endDate the ultimate end date.
   * @return a list of all Periods with start date after or equal the specified start date and end
   *     date before or equal the specified end date, or an empty collection if no Periods match.
   */
  List<Period> getPeriodsBetweenDates(PeriodType periodType, Date startDate, Date endDate);

  /**
   * Returns Periods where at least one its days is between the given start date and end date.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @return Periods where at least one its days is between the given start date and end date.
   */
  List<Period> getIntersectingPeriods(Date startDate, Date endDate);

  /**
   * Checks if the given period is associated with the current session and loads it if not. Null is
   * returned if the period does not exist.
   *
   * @param period the Period.
   * @return the Period.
   */
  Period reloadPeriod(Period period);

  /**
   * Checks if the given period is associated with the current session and loads it if not. The
   * period is persisted if it does not exist. The persisted Period is returned.
   *
   * @param period the Period.
   * @return the persisted Period.
   */
  Period reloadForceAddPeriod(Period period);

  // -------------------------------------------------------------------------
  // PeriodType
  // -------------------------------------------------------------------------

  /**
   * Adds a PeriodType.
   *
   * @param periodType the PeriodType to add.
   */
  void addPeriodType(PeriodType periodType);

  /**
   * Returns all PeriodTypes.
   *
   * @return a list of all PeriodTypes, or an empty list if there are no PeriodTypes.
   */
  List<PeriodType> getAllPeriodTypes();

  // -------------------------------------------------------------------------
  // RelativePeriods
  // -------------------------------------------------------------------------

  /**
   * Deletes a RelativePeriods instance.
   *
   * @param relativePeriods the RelativePeriods instance.
   */
  void deleteRelativePeriods(RelativePeriods relativePeriods);
}

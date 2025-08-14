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

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Kristian Nordal
 */
public interface PeriodService {
  /**
   * Adds a Period.
   *
   * @param period the Period to add.
   */
  void addPeriod(Period period);

  /**
   * Deletes a Period.
   *
   * @param period the Period to delete.
   */
  void deletePeriod(Period period);

  /**
   * Returns a Period.
   *
   * @param id the id of the Period to return.
   * @return the Period with the given id, or null if no match.
   */
  Period getPeriod(long id);

  /**
   * Gets the Period with the given ISO period identifier.
   *
   * @param isoPeriod the ISO period identifier.
   * @return the Period with the given ISO period identifier.
   */
  Period getPeriod(String isoPeriod);

  /**
   * Returns a Period.
   *
   * @param startDate the start date of the Period.
   * @param periodType the PeriodType of the Period
   * @return the Period matching the dates and periodtype, or null if no match.
   */
  Period getPeriodFromDates(Date startDate, PeriodType periodType);

  /**
   * Returns all persisted Periods.
   *
   * @return all persisted Periods.
   */
  List<Period> getAllPeriods();

  /**
   * Returns all Periods with start date after or equal the specified start date and end date before
   * or equal the specified end date.
   *
   * @param startDate the ultimate start date.
   * @param endDate the ultimate end date.
   * @return a list of all Periods with start date after or equal the specified start date and end
   *     date before or equal the specified end date, or an empty list if no Periods match.
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
   *     date before or equal the specified end date, or an empty list if no Periods match.
   */
  List<Period> getPeriodsBetweenDates(PeriodType periodType, Date startDate, Date endDate);

  /**
   * Returns Periods where at least one its days are between the given start date and end date.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @return Periods where at least one its days are between the given start date and end date.
   */
  List<Period> getIntersectingPeriods(Date startDate, Date endDate);

  /**
   * Returns Periods where at least one its days are between each of the Periods start date and end
   * date in the given collection.
   *
   * @param periods the collection of Periods.
   * @return a list of Periods.
   */
  List<Period> getIntersectionPeriods(Collection<Period> periods);

  /**
   * Returns all Periods from the given collection of Periods which are completely within the span
   * of the of the given Period.
   *
   * @param period the base Period.
   * @param periods the collection of Periods.
   * @return all Periods from the given collection of Periods which are completely within the span
   *     of the of the given Period.
   */
  List<Period> getInclusivePeriods(Period period, Collection<Period> periods);

  /**
   * Returns all Periods with a given PeriodType.
   *
   * @param periodType the PeriodType of the Periods to return.
   * @return all Periods with the given PeriodType, or an empty list if no Periods match.
   */
  List<Period> getPeriodsByPeriodType(PeriodType periodType);

  /**
   * Enforces that each Period in the given collection is loaded in the current session. Persists
   * the Period if it does not exist.
   *
   * @param periods the collection of Periods.
   * @return the list of Periods.
   */
  List<Period> reloadPeriods(Collection<Period> periods);

  /**
   * Returns a list of the given number of previous periods in ascending order. The given last
   * period appears last in the returned list.
   *
   * @param lastPeriod the last period.
   * @param previousPeriods the number of previous periods.
   * @return a list of {@link Period}.
   */
  List<Period> getPeriods(Period lastPeriod, int previousPeriods);

  /**
   * Checks if the given Period is associated with the current session. If not, replaces the Period
   * with a Period associated with the current session. Persists the Period if not already
   * persisted.
   *
   * @param period the Period to reload.
   * @return a Period.
   */
  Period reloadPeriod(Period period);

  Period reloadIsoPeriodInStatelessSession(String isoPeriod);

  /**
   * Retrieves the period with the given ISO period identifier. Reloads the period in the session if
   * found.
   *
   * @param isoPeriod the ISO period identifier.
   * @return a Period.
   */
  Period reloadIsoPeriod(String isoPeriod);

  /**
   * Retrieves the period with the given ISO period identifiers. Reloads the periods in the session
   * if found.
   *
   * @param isoPeriods the list of ISO period identifiers.
   * @return a list of Periods.
   */
  List<Period> reloadIsoPeriods(List<String> isoPeriods);

  /**
   * Returns how many days into period date is. If date is before period.startDate, returns 0. If
   * date is after period.endDate, return last day of period.
   *
   * @param period the period.
   * @param date the date.
   * @return the day in period.
   */
  int getDayInPeriod(Period period, Date date);

  // -------------------------------------------------------------------------
  // PeriodType
  // -------------------------------------------------------------------------

  /**
   * Returns a PeriodType with the given name.
   *
   * @param name the name of the PeriodType to return.
   * @return the PeriodType with the given name, or null if no match.
   */
  PeriodType getPeriodTypeByName(String name);

  /**
   * Returns a persisted PeriodType represented by the given Class.
   *
   * @param periodType the Class type of the PeriodType.
   * @return a PeriodType instance.
   */
  PeriodType getPeriodTypeByClass(Class<? extends PeriodType> periodType);

  /**
   * Checks if the given periodType is associated with the current session and loads it if not. Null
   * is returned if the period does not exist.
   *
   * @param periodType the Period to reload.
   * @return a Period.
   */
  PeriodType reloadPeriodType(PeriodType periodType);

  /**
   * Returns a PeriodType with the given {@link PeriodTypeEnum} value.
   *
   * @param periodType the {@link PeriodTypeEnum}.
   * @return the PeriodType with the given name, or null if no match.
   */
  default PeriodType getPeriodType(PeriodTypeEnum periodType) {
    return getPeriodTypeByName(periodType.getName());
  }
}

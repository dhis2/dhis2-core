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
package org.hisp.dhis.commons.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.comparator.DescendingPeriodComparator;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Lars Helge Overland
 */
public class GetPeriodsAction extends ActionPagingSupport<Period> {
  private static final String ALL = "ALL";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private PeriodService periodService;

  public void setPeriodService(PeriodService periodService) {
    this.periodService = periodService;
  }

  private I18nFormat format;

  public void setFormat(I18nFormat format) {
    this.format = format;
  }

  // -------------------------------------------------------------------------
  // Input & Output
  // -------------------------------------------------------------------------

  private String name;

  public void setName(String name) {
    this.name = name;
  }

  private List<Period> periods;

  public List<Period> getPeriods() {
    return periods;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    canReadType(Period.class);

    if (name == null || name.equals(ALL)) {
      List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();

      periods = new ArrayList<>();

      for (PeriodType type : periodTypes) {
        periods.addAll(periodService.getPeriodsByPeriodType(type));
      }
    } else {
      PeriodType periodType = periodService.getPeriodTypeByName(name);

      periods = new ArrayList<>(periodService.getPeriodsByPeriodType(periodType));
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    periods.forEach(instance -> canReadInstance(instance, currentUserDetails));

    for (Period period : periods) {
      period.setName(format.formatPeriod(period));
    }

    Collections.sort(periods, new DescendingPeriodComparator());

    if (usePaging) {
      this.paging = createPaging(periods.size());

      periods = periods.subList(paging.getStartPos(), paging.getEndPos());
    }

    return SUCCESS;
  }
}

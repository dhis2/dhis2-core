/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.webdomain;

import java.util.Date;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.util.DateUtils;

/**
 * EndDateTime represents an upper limit date and time used to filter results in search APIs.
 *
 * <p>EndDateTime accepts any date and time in ISO8601 format. If no time is defined, then the time
 * at the end of the day is used by default.
 *
 * <p>This behavior, combined with {@link StartDateTime}, allows to correctly implement an interval
 * search including start and end dates.
 */
@OpenApi.Description(
    """
  Use a valid ISO8601 date _`YYYY[-]MM[-]DD`_ or date-time _`YYYY[-]MM[-]DD'T'hh[:mm[:ss[.sss]]]`_ pattern.
  When the time part is omitted the end of the day is assumed.""")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EndDateTime {
  private final Date date;

  public static EndDateTime of(String date) {
    return new EndDateTime(DateUtils.parseDateEndOfTheDay(date));
  }

  public Date toDate() {
    if (date == null) {
      return null;
    }
    return new Date(date.getTime());
  }
}

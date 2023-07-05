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
package org.hisp.dhis.outlierdetection.util;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Lars Helge Overland
 */
public class OutlierDetectionUtils {
  /**
   * Returns an organisation unit 'path' "like" clause for the given list of {@link
   * OrganisationUnit}.
   *
   * @param query the list of {@link OrganisationUnit}.
   * @return an organisation unit 'path' "like" clause.
   */
  public static String getOrgUnitPathClause(List<OrganisationUnit> orgUnits) {
    String sql = "(";

    for (OrganisationUnit ou : orgUnits) {
      sql += "ou.\"path\" like '" + ou.getPath() + "%' or ";
    }

    return StringUtils.trim(TextUtils.removeLastOr(sql)) + ")";
  }

  /**
   * Returns a period data start date clause.
   *
   * @param dataStartDate the data start date.
   * @return a period data start date clause.
   */
  public static String getDataStartDateClause(Date dataStartDate) {
    return dataStartDate != null ? "and pe.startdate >= :data_start_date " : StringUtils.EMPTY;
  }

  /**
   * Returns a period data end date clause.
   *
   * @param dataStartDate the data start date.
   * @return a period data end date clause.
   */
  public static String getDataEndDateClause(Date dataStartDate) {
    return dataStartDate != null ? "and pe.enddate <= :data_end_date " : StringUtils.EMPTY;
  }
}

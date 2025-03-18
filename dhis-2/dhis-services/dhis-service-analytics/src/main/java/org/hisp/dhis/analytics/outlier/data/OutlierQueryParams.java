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
package org.hisp.dhis.analytics.outlier.data;

import static org.hisp.dhis.analytics.OutlierDetectionAlgorithm.Z_SCORE;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import org.hisp.dhis.analytics.OutlierDetectionAlgorithm;
import org.hisp.dhis.analytics.QueryKey;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.IdScheme;

/** Encapsulation of a web API request for outlier value detection. */
@Data
public class OutlierQueryParams {
  private Set<String> ds = new HashSet<>();

  private Set<String> dx = new HashSet<>();

  private Set<String> ou = new HashSet<>();

  private String pe;

  /**
   * This parameter selects the headers to be returned in the response. We use a LinkedHashSet
   * because the order matters.
   */
  private Set<String> headers = new LinkedHashSet<>();

  private Date startDate;

  private Date endDate;

  private OutlierDetectionAlgorithm algorithm = Z_SCORE;

  private Double threshold;

  private Date dataStartDate;

  private Date dataEndDate;

  private Date relativePeriodDate;

  private String orderBy;

  private SortOrder sortOrder;

  private Integer maxResults;

  private IdScheme outputIdScheme = IdScheme.UID;

  private boolean skipRounding;

  public boolean hasHeaders() {
    return headers != null && !headers.isEmpty();
  }

  public String queryKey() {
    QueryKey key = new QueryKey();

    key.add(dataStartDate);
    key.add(dataEndDate);
    key.add(startDate);
    key.add(endDate);
    key.add(relativePeriodDate);
    key.add(pe);
    key.add(maxResults);
    key.add(algorithm);
    key.add(threshold);
    key.add(orderBy);
    key.add(sortOrder);
    key.add(outputIdScheme);
    key.add(skipRounding);

    if (ds != null) {
      ds.forEach(e -> key.add("ds", "[" + e + "]"));
    }

    if (dx != null) {
      dx.forEach(e -> key.add("dx", "[" + e + "]"));
    }

    if (ou != null) {
      ou.forEach(e -> key.add("ou", "[" + e + "]"));
    }

    if (headers != null) {
      headers.forEach(e -> key.add("header", "[" + e + "]"));
    }

    return key.build();
  }
}

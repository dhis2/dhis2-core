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
package org.hisp.dhis.outlierdetection;

import static org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm.Z_SCORE;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/**
 * Encapsulation of a web API request for outlier value detection.
 *
 * @author Lars Helge Overland
 */
@Data
public class OutlierDetectionQuery {
  private Set<String> ds = new HashSet<>();

  private Set<String> dx = new HashSet<>();

  private Date startDate;

  private Date endDate;

  private Set<String> ou = new HashSet<>();
  ;

  private OutlierDetectionAlgorithm algorithm = Z_SCORE;

  private Double threshold;

  private Date dataStartDate;

  private Date dataEndDate;

  private Order orderBy;

  private Integer maxResults;

  /**
   * This parameter selects the headers to be returned as part of the response. The implementation
   * for this Set will be LinkedHashSet as the ordering is important.
   */
  private Set<String> headers = new HashSet<>();

  public boolean hasHeaders() {
    return headers != null && !headers.isEmpty();
  }
}

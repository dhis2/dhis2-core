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
package org.hisp.dhis.analytics.outlier;

import static org.hisp.dhis.analytics.common.ColumnHeader.ABSOLUTE_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.LOWER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.MEDIAN_ABS_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.STANDARD_DEVIATION;
import static org.hisp.dhis.analytics.common.ColumnHeader.UPPER_BOUNDARY;
import static org.hisp.dhis.analytics.common.ColumnHeader.ZSCORE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.ColumnHeader;

/**
 * Candidate on which to order an outlier detection result set.
 *
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public enum Order {
  Z_SCORE(ZSCORE.getItem(), "z_score"),
  MODIFIED_ZSCORE(ColumnHeader.MODIFIED_ZSCORE.getItem(), "z_score"),
  VALUE("value", "value"),
  MEDIAN(ColumnHeader.MEDIAN.getItem(), "middle_value"),
  MEAN(ColumnHeader.MEAN.getItem(), "middle_value"),
  STD_DEV(STANDARD_DEVIATION.getName(), "std_dev"),
  MEDIAN_ABS_DEV(MEDIAN_ABS_DEVIATION.getItem(), "middle_value_abs_dev"),
  MEAN_ABS_DEV(ABSOLUTE_DEVIATION.getItem(), "middle_value_abs_dev"),
  LOWER_BOUND(LOWER_BOUNDARY.getItem(), "lower_bound"),
  UPPER_BOUND(UPPER_BOUNDARY.getItem(), "upper_bound");

  @Getter private final String headerName;
  @Getter private final String columnName;
}

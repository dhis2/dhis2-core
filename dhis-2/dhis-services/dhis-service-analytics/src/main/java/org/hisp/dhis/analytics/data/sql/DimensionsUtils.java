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
package org.hisp.dhis.analytics.data.sql;

import static java.lang.String.join;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.SqlBuilder;

@RequiredArgsConstructor
public class DimensionsUtils {

  private final SqlBuilder sqlBuilder;

  /**
   * Generates a comma-delimited string with the dimension names of the given dimensions where each
   * dimension name is quoted. Dimensions which are considered fixed will be excluded.
   *
   * @param dimensions the collection of {@link DimensionalObject}.
   * @return a comma-delimited string of quoted dimension names.
   */
  public String getCommaDelimitedQuotedDimensionColumns(Collection<DimensionalObject> dimensions) {
    return join(",", getQuotedDimensionColumns(dimensions));
  }

  /**
   * Generates a list of the dimension names of the given dimensions where each dimension name is
   * quoted. Dimensions which are considered fixed will be excluded.
   *
   * @param dimensions the collection of {@link DimensionalObject}.
   * @return a list of quoted dimension names.
   */
  public List<String> getQuotedDimensionColumns(Collection<DimensionalObject> dimensions) {
    return dimensions.stream()
        .filter(d -> !d.isFixed())
        .map(DimensionalObject::getDimensionName)
        .map(sqlBuilder::quoteAx)
        .collect(Collectors.toList());
  }
}

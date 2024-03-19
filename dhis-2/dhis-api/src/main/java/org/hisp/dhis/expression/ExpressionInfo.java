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
package org.hisp.dhis.expression;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.DimensionalItemId;

/**
 * Information parsed from an expression
 *
 * <p>This is only information that is gathered from parsing the expression, and contains no
 * information from any other source such as the database (either data or metadata). In other words,
 * the same expression string will always result in the same information here.
 *
 * @author Jim Grace
 */
@Getter
@Setter
public class ExpressionInfo {
  /** The dimensional item ids found. */
  private Set<DimensionalItemId> itemIds = new HashSet<>();

  /** The sampled dimensional item ids found (for predictors). */
  private Set<DimensionalItemId> sampleItemIds = new HashSet<>();

  /** Ids of org unit groups that will need org unit group member counts. */
  private Set<String> orgUnitGroupCountIds = new HashSet<>();

  /** Ids of org unit groups found in orgUnits.groups function. */
  private Set<String> orgUnitGroupIds = new HashSet<>();

  /** Ids of data sets found in orgUnits.dataSet function. */
  private Set<String> orgUnitDataSetIds = new HashSet<>();

  /** Ids of programs found in orgUnits.program function. */
  private Set<String> orgUnitProgramIds = new HashSet<>();

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public Set<DimensionalItemId> getAllItemIds() {
    return Sets.union(itemIds, sampleItemIds);
  }
}

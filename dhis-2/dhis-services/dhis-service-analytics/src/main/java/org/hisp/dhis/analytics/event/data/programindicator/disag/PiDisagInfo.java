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
package org.hisp.dhis.analytics.event.data.programindicator.disag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.program.ProgramCategoryMapping;

/**
 * Contextual information needed when disaggregating a {@link
 * org.hisp.dhis.program.ProgramIndicator} during an analytics query
 *
 * @author Jim Grace
 */
@Builder
@Getter
public class PiDisagInfo {

  /** Category UIDs that are used as query dimensions */
  Set<String> dimensionCategories;

  /**
   * Additional category UIDs (that are not also dimensions) needed to assemble a
   * categoryOptionCombo and/or attributeOptionCombo. (This is a list rather than a set for
   * consistency in ordering the SQL columns, since it is traversed more than once.)
   */
  List<String> cocCategories;

  /** All category mappings that are being used by the current query */
  Map<String, ProgramCategoryMapping> categoryMappings;

  /** Map of ordered option UIDs to categoryOptionCombo UID */
  Map<String, String> cocResolver;

  /** Map of ordered option UIDs to attributeOptionCombo UID */
  Map<String, String> aocResolver;

  /**
   * Tests to see if a dimension is provided by program indicator disaggregation logic.
   *
   * @param dimension the dimension to test
   * @return true if a piDisag dimension, else false
   */
  public boolean isPiDisagDimension(String dimension) {
    return categoryMappings.containsKey(dimension);
  }
}

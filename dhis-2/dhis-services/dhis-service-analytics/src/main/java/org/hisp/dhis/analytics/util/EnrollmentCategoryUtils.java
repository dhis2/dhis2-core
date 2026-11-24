/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Program;

/**
 * Resolves the attribute categories of a {@link Program} that apply to its enrollments.
 *
 * <p>The attribute option combo of an enrollment belongs to {@link
 * Program#getEnrollmentCategoryCombo()}, not to {@link Program#getCategoryCombo()}, which applies
 * to events. The enrollment analytics table holds one column per data dimension category of the
 * enrollment category combo, so these are the only categories enrollment analytics can filter on.
 */
@UtilityClass
public class EnrollmentCategoryUtils {

  /**
   * Returns the data dimension categories of the enrollment category combo of the given program, or
   * an empty list if the program is null or its enrollment category combo is the default one.
   *
   * @param program the {@link Program}.
   * @return the list of {@link Category}.
   */
  public static List<Category> getEnrollmentCategories(Program program) {
    if (program == null || !program.hasNonDefaultEnrollmentCategoryCombo()) {
      return List.of();
    }

    return program.getEnrollmentCategoryCombo().getDataDimensionCategories();
  }

  /**
   * Returns the uids of the categories returned by {@link #getEnrollmentCategories(Program)}.
   *
   * @param program the {@link Program}.
   * @return the set of category uids.
   */
  public static Set<String> getEnrollmentCategoryUids(Program program) {
    return getEnrollmentCategories(program).stream()
        .map(IdentifiableObject::getUid)
        .collect(Collectors.toSet());
  }
}

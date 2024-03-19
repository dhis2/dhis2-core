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
package org.hisp.dhis.tracker.imports.programrule.executor;

import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.enrollment.AssignAttributeExecutor;
import org.hisp.dhis.tracker.imports.programrule.executor.event.AssignDataValueExecutor;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.tracker.imports.report.Warning;

/**
 * A {@link RuleActionExecutor} execute a rule action for an event or an enrollment. The execution
 * can produce a {@link ProgramRuleIssue} that will be converted into an {@link
 * org.hisp.dhis.tracker.imports.report.Error} or a {@link Warning} and presented to the client in
 * the {@link ValidationReport}.
 *
 * <p>{@link AssignAttributeExecutor} can also mutate the Bundle, as it can add or change the value
 * of an attribute. {@link AssignDataValueExecutor} can do the same for a data element.
 */
public interface RuleActionExecutor<T> {

  /**
   * Tests whether the given values are equal. If the given value type is numeric, the values are
   * converted to doubles before being checked for equality.
   *
   * @param value1 the first value.
   * @param value2 the second value.
   * @param valueType the value type.
   * @return true if the values are equal, false if not.
   */
  static boolean isEqual(String value1, String value2, ValueType valueType) {
    if (Objects.equals(value1, value2)) {
      return true;
    }

    if (valueType.isNumeric()) {
      return NumberUtils.isParsable(value1)
          && NumberUtils.isParsable(value2)
          && MathUtils.isEqual(Double.parseDouble(value1), Double.parseDouble(value2));
    }
    return false;
  }

  /**
   * A rule action can be associated with an attribute or a data element. When it is associated with
   * a data element we need to make sure the data element is part of the {@link ProgramStage} of the
   * event otherwise we do not need to execute the action.
   *
   * @return the dataElement Uid the rule action is associated with.
   */
  default String getDataElementUid() {
    return null;
  }

  Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, T entity);
}

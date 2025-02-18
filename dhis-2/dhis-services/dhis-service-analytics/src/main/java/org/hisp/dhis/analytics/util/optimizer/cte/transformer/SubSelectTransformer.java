/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.optimizer.cte.transformer;

import static org.hisp.dhis.analytics.util.optimizer.cte.StringUtils.preserveLettersAndNumbers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatcher;

public class SubSelectTransformer {
  @Getter private final Map<SubSelect, FoundSubSelect> extractedSubSelects = new LinkedHashMap<>();

  // The matchers you want to run (in order).
  private final List<SubselectMatcher> matchers;

  public SubSelectTransformer(List<SubselectMatcher> matchers) {
    this.matchers = matchers;
  }

  /**
   * Transforms a given SubSelect if it matches one of the known patterns. Otherwise, it returns the
   * original SubSelect unchanged.
   */
  public Expression transform(SubSelect subSelect) {
    // If it's null or missing a SelectBody, there's nothing to transform
    if (subSelect == null || subSelect.getSelectBody() == null) {
      return subSelect;
    }

    // Find the first matching pattern
    Optional<FoundSubSelect> matched = findMatchingPattern(subSelect);

    // If we find a match, store it and return a new Column referencing the alias/column
    if (matched.isPresent()) {
      FoundSubSelect found = matched.get();
      extractedSubSelects.put(subSelect, found);

      // Derive the alias for the column
      String alias = deriveAlias(found);
      return new Column(new Table(alias), found.columnReference());
    } else {
      // No match; return subSelect as-is
      return subSelect;
    }
  }

  /**
   * Finds the first matching pattern for a SubSelect. Matchers are evaluated in the order they were
   * defined.
   */
  private Optional<FoundSubSelect> findMatchingPattern(SubSelect subSelect) {
    return matchers.stream()
        .map(matcher -> matcher.match(subSelect))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  /**
   * Derives the alias for the column based on the FoundSubSelect 'name'. This replicates the logic
   * from ExpressionTransformer.visit(SubSelect).
   */
  private String deriveAlias(FoundSubSelect found) {
    return switch (found.name()) {
      case "last_sched" -> "ls";
      case "last_created" -> "lc";
      case "relationship_count", "relationship_count_agg" -> "rlc";
      default -> {
        // Handle "last_value_*" and "de_count_*"
        if (found.name().startsWith("last_value_")) {
          yield "lv_" + preserveLettersAndNumbers(found.columnReference());
        }
        if (found.name().startsWith("de_count_")) {
          yield "dec_" + preserveLettersAndNumbers(found.columnReference());
        }
        // Fallback to the subselect's name
        yield found.name();
      }
    };
  }
}

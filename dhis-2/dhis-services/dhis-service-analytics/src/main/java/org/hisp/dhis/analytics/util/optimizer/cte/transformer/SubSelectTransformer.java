package org.hisp.dhis.analytics.util.optimizer.cte.transformer;

import lombok.Getter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubSelectTransformer {
    @Getter
    private final Map<SubSelect, FoundSubSelect> extractedSubSelects = new LinkedHashMap<>();

    // The matchers you want to run (in order).
    private final List<SubselectMatcher> matchers;

    public SubSelectTransformer(List<SubselectMatcher> matchers) {
        this.matchers = matchers;
    }

    /**
     * Transforms a given SubSelect if it matches one of the known patterns.
     * Otherwise, it returns the original SubSelect unchanged.
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
     * Finds the first matching pattern for a SubSelect.
     * Matchers are evaluated in the order they were defined.
     */
    private Optional<FoundSubSelect> findMatchingPattern(SubSelect subSelect) {
        return matchers.stream()
                .map(matcher -> matcher.match(subSelect))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Derives the alias for the column based on the FoundSubSelect 'name'.
     * This replicates the logic from ExpressionTransformer.visit(SubSelect).
     */
    private String deriveAlias(FoundSubSelect found) {
        return switch (found.name()) {
            case "last_sched" -> "ls";
            case "last_created" -> "lc";
            case "relationship_count", "relationship_count_agg" -> "rlc";
            default -> {
                // Handle "last_value_*" and "de_count_*"
                if (found.name().startsWith("last_value_")) {
                    yield "lv_" + preserveLetterNumbers(found.columnReference());
                }
                if (found.name().startsWith("de_count_")) {
                    yield "dec_" + preserveLetterNumbers(found.columnReference());
                }
                // Fallback to the subselect's name
                yield found.name();
            }
        };
    }

    /**
     * Preserves only letters and digits from a string, removing punctuation.
     */
    private String preserveLetterNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}

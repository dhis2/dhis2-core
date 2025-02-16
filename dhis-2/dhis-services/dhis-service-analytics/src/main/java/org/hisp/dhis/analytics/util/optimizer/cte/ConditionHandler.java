package org.hisp.dhis.analytics.util.optimizer.cte;

import lombok.Getter;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

import java.util.function.Function;
import java.util.function.Predicate;

public class ConditionHandler {

    private final Predicate<String> condition;
    @Getter
    private final Function<CteInput, GeneratedCte> generator;

    public ConditionHandler(Predicate<String> condition, Function<CteInput, GeneratedCte> generator) {
        this.condition = condition;
        this.generator = generator;
    }

    public boolean matches(String name) {
        return condition.test(name);
    }
}

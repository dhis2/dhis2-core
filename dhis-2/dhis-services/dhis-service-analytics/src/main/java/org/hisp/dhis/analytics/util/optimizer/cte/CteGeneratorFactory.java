package org.hisp.dhis.analytics.util.optimizer.cte;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.dataElementCountCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastCreatedCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastEventValueCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastScheduledCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.relationshipCountCte;

public class CteGeneratorFactory {

    private final List<ConditionHandler> handlers;

    public CteGeneratorFactory() {
        this.handlers = new ArrayList<>();
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.add(new ConditionHandler(
                "last_sched"::equals,
                input -> new GeneratedCte(
                        input.found().name(),
                        lastScheduledCte(input.eventTable()),
                        "ls"
                )
        ));

        handlers.add(new ConditionHandler(
                "last_created"::equals,
                input -> new GeneratedCte(
                        input.found().name(),
                        lastCreatedCte(input.eventTable()),
                        "lc"
                )
        ));

        handlers.add(new ConditionHandler(
                name -> name.startsWith("relationship_count"),
                input -> {
                    boolean isAggregated = Boolean.parseBoolean(
                            input.found().metadata().getOrDefault("isAggregated", "false")
                    );
                    String relationshipTypeUid = input.found().metadata().get("relationshipTypeUid");
                    String newCteSql = relationshipCountCte(isAggregated, relationshipTypeUid);
                    return new GeneratedCte(
                            input.found().name(),
                            newCteSql,
                            "rlc",
                            ImmutablePair.of("trackedentity", "trackedentityid")
                    );
                }
        ));

        handlers.add(new ConditionHandler(
                name -> name.startsWith("last_value_"),
                input -> {
                    String newCteSql = lastEventValueCte(input.eventTable(), input.found().columnReference());
                    String alias = "lv_" + preserveLettersAndNumbers(input.found().columnReference());
                    return new GeneratedCte(input.found().name(), newCteSql, alias);
                }
        ));

        handlers.add(new ConditionHandler(
                name -> name.startsWith("de_count_"),
                input -> {
                    String newCteSql = dataElementCountCte(input.subSelect(), input.found().columnReference());
                    String alias = "dec_" + preserveLettersAndNumbers(input.found().columnReference());
                    return new GeneratedCte(input.found().name(), newCteSql, alias);
                }
        ));
    }

    public Function<CteInput, GeneratedCte> getGenerator(String cteName) {
        return handlers.stream()
                .filter(handler -> handler.matches(cteName))
                .findFirst()
                .map(ConditionHandler::getGenerator)
                .orElseThrow(() -> new IllegalArgumentException("No CTE generator found for: " + cteName));
    }

    /**
     * Removes all characters from the input string that are not letters or numbers.
     *
     * @param str the input string to be processed
     * @return a new string containing only letters and numbers from the input string
     */
    private String preserveLettersAndNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}


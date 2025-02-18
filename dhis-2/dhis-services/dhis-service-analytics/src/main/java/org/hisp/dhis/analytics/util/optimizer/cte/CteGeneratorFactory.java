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
package org.hisp.dhis.analytics.util.optimizer.cte;

import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.dataElementCountCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastCreatedCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastEventValueCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.lastScheduledCte;
import static org.hisp.dhis.analytics.util.optimizer.cte.SubqueryTransformer.relationshipCountCte;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

public class CteGeneratorFactory {

  private final List<ConditionHandler> handlers;

  public CteGeneratorFactory() {
    this.handlers = new ArrayList<>();
    registerHandlers();
  }

  private void registerHandlers() {
    handlers.add(
        new ConditionHandler(
            "last_sched"::equals,
            input ->
                new GeneratedCte(
                    input.found().name(), lastScheduledCte(input.eventTable()), "ls")));

    handlers.add(
        new ConditionHandler(
            "last_created"::equals,
            input ->
                new GeneratedCte(input.found().name(), lastCreatedCte(input.eventTable()), "lc")));

    handlers.add(
        new ConditionHandler(
            name -> name.startsWith("relationship_count"),
            input -> {
              boolean isAggregated =
                  Boolean.parseBoolean(
                      input.found().metadata().getOrDefault("isAggregated", "false"));
              String relationshipTypeUid = input.found().metadata().get("relationshipTypeUid");
              String newCteSql = relationshipCountCte(isAggregated, relationshipTypeUid);
              return new GeneratedCte(
                  input.found().name(),
                  newCteSql,
                  "rlc",
                  ImmutablePair.of("trackedentity", "trackedentityid"));
            }));

    handlers.add(
        new ConditionHandler(
            name -> name.startsWith("last_value_"),
            input -> {
              String newCteSql =
                  lastEventValueCte(input.eventTable(), input.found().columnReference());
              String alias = "lv_" + preserveLettersAndNumbers(input.found().columnReference());
              return new GeneratedCte(input.found().name(), newCteSql, alias);
            }));

    handlers.add(
        new ConditionHandler(
            name -> name.startsWith("de_count_"),
            input -> {
              String newCteSql =
                  dataElementCountCte(input.subSelect(), input.found().columnReference());
              String alias = "dec_" + preserveLettersAndNumbers(input.found().columnReference());
              return new GeneratedCte(input.found().name(), newCteSql, alias);
            }));
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

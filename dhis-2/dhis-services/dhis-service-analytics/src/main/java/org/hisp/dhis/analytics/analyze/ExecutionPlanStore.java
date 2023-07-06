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
package org.hisp.dhis.analytics.analyze;

import java.util.List;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.common.ExecutionPlan;

/**
 * Responsible for providing methods responsible for executing/explaining SQL statements, and making
 * them available for the consumers.
 */
public interface ExecutionPlanStore {
  /**
   * Executes and add the result planning of the given "sql". The resulting internal {@link
   * ExecutionPlan} will be stored with the associated "key".
   *
   * @param key the unique key associated with {@link ExecutionPlan} objects.
   * @param sql the statement to be executed/explained.
   */
  void addExecutionPlan(String key, String sql);

  /**
   * Executes and add the result planning of the given "sql". The resulting internal {@link
   * ExecutionPlan} objects will be stored with the associated "key".
   *
   * @param key the unique key associated with {@link ExecutionPlan} objects.
   * @param sqlQuery the statement to be executed/explained.
   */
  void addExecutionPlan(String key, SqlQuery sqlQuery);

  /**
   * Returns all available {@link ExecutionPlan} associated with the given "key".
   *
   * @param key the unique key associated with a {@link ExecutionPlan}.
   */
  List<ExecutionPlan> getExecutionPlans(String key);

  /**
   * Removes all {@link ExecutionPlan} objects associated with the given "key".
   *
   * @param key the unique key associated with {@link ExecutionPlan} objects.
   */
  void removeExecutionPlans(String key);
}

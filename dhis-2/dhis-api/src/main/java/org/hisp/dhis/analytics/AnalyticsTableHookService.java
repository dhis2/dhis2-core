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
package org.hisp.dhis.analytics;

import java.util.List;
import org.hisp.dhis.resourcetable.ResourceTableType;

/**
 * @author Lars Helge Overland
 */
public interface AnalyticsTableHookService {
  /**
   * Returns an {@link AnalyticsTableHook}.
   *
   * @param uid the identifier.
   * @return an {@link AnalyticsTableHook}.
   */
  AnalyticsTableHook getByUid(String uid);

  /**
   * Returns a list of {@link AnalyticsTableHook} with the given phase.
   *
   * @param phase the {@link AnalyticsTablePhase}.
   * @return a list of {@link AnalyticsTableHook}.
   */
  List<AnalyticsTableHook> getByPhase(AnalyticsTablePhase phase);

  /**
   * Returns a list of {@link AnalyticsTableHook} with the given phase and given resource table
   * type.
   *
   * @param phase the {@link AnalyticsTablePhase}.
   * @param resourceTableType the {@link ResourceTableType}.
   * @return a list of {@link AnalyticsTableHook}.
   */
  List<AnalyticsTableHook> getByPhaseAndResourceTableType(
      AnalyticsTablePhase phase, ResourceTableType resourceTableType);

  /**
   * Returns a list of {@link AnalyticsTableHook} with the given phase and given analytics table
   * type.
   *
   * @param phase the {@link AnalyticsTablePhase}.
   * @param analyticsTableType the {@link AnalyticsTableType}.
   * @return a list of {@link AnalyticsTableHook}.
   */
  List<AnalyticsTableHook> getByPhaseAndAnalyticsTableType(
      AnalyticsTablePhase phase, AnalyticsTableType analyticsTableType);

  /**
   * Executes the SQL commands for the given list of {@link AnalyticsTableHook}.
   *
   * @param hooks the list of analytics table hooks.
   */
  void executeAnalyticsTableSqlHooks(List<AnalyticsTableHook> hooks);
}

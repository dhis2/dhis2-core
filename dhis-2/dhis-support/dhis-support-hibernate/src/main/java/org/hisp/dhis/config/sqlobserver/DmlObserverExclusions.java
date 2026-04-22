/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.config.sqlobserver;

import java.util.Set;

/**
 * Tables excluded from DML observation. These are high-churn internal tables where audit/cache
 * invalidation events would be noise.
 */
public class DmlObserverExclusions {

  private DmlObserverExclusions() {}

  /**
   * Tables excluded from DML observation:
   *
   * <ul>
   *   <li>{@code audit} — is never observed in the system, only externaly
   *   <li>{@code flyway_schema_history} — schema migration bookkeeping, not application data
   *   <li>{@code jobconfiguration} — updated on every job execution (heartbeats), very high churn
   *   <li>{@code hibernate_sequence} — sequence generator, not meaningful for cache invalidation
   *   <li>{@code spring_session*} — session store, not application data
   *   <li>{@code icon} — static reference data, rarely changes
   *   <li>{@code messageconversation_*} — messaging join tables, high churn, not cached in API
   *   <li>{@code datavalue} — highest-volume table in DHIS2 (possibly millions of rows during
   *       imports)
   *   <li>{@code trackedentityattributevalue}, {@code eventdatavalue} — tracker data, very high
   *       volume
   *   <li>{@code programstageinstance}, {@code programinstance} — tracker event/enrollment tables
   * </ul>
   */
  private static final Set<String> EXCLUDED_TABLES =
      Set.of(
          "audit",
          "flyway_schema_history",
          "jobconfiguration",
          "hibernate_sequence",
          "spring_session",
          "spring_session_attributes",
          "icon",
          "messageconversation_messages",
          "messageconversation_usermessages",
          // High-volume data tables — excluding here avoids unnecessary SQL parsing during imports.
          "datavalue",
          "trackedentityattributevalue",
          "eventdatavalue",
          "programstageinstance",
          "programinstance");

  /** Returns true if the given table name should be excluded from DML observation. */
  public static boolean isExcluded(String tableName) {
    if (tableName == null) {
      return true;
    }
    return EXCLUDED_TABLES.contains(tableName.toLowerCase());
  }
}

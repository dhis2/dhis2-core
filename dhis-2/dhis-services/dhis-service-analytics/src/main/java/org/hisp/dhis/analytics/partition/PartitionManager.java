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
package org.hisp.dhis.analytics.partition;

import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;

/**
 * Manager for analytics table partitions.
 *
 * @author Lars Helge Overland
 */
public interface PartitionManager {
  /**
   * Returns a set of names of current event analytics partitions.
   *
   * @param tableType the type to get all existing table partitions for.
   */
  Set<String> getAnalyticsPartitions(AnalyticsTableType tableType);

  /**
   * Indicates whether the given analytics table exists.
   *
   * @param table the analytics table name.
   */
  boolean tableExists(String table);

  /**
   * Filters the set of integers for which a database partition table exists.
   *
   * @param partitions the partitions.
   * @param tableName the table name.
   */
  void filterNonExistingPartitions(Partitions partitions, String tableName);

  /**
   * Event handler for {@link ApplicationCacheClearedEvent}.
   *
   * @param event the {@link ApplicationCacheClearedEvent}.
   */
  void handleApplicationCachesCleared(ApplicationCacheClearedEvent event);
}

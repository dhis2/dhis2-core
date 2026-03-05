/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.sync;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.dxf2.sync.DataSynchronizationWithPaging.PagedDataSynchronisationContext;
import org.hisp.dhis.dxf2.sync.SystemInstance;

public final class TrackerSynchronizationContext extends PagedDataSynchronisationContext {

  private final Map<String, Set<String>> skipSyncDataElementsByProgramStage;

  private TrackerSynchronizationContext(
      Date skipChangedBefore,
      long objectsToSynchronize,
      SystemInstance instance,
      int pageSize,
      Map<String, Set<String>> skipSyncDataElementsByProgramStage) {

    super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
    this.skipSyncDataElementsByProgramStage = skipSyncDataElementsByProgramStage;
  }

  static TrackerSynchronizationContext emptyContext(Date skipChangedBefore, int pageSize) {
    return new TrackerSynchronizationContext(skipChangedBefore, 0, null, pageSize, Map.of());
  }

  static TrackerSynchronizationContext forTrackedEntities(
      Date skipChangedBefore, long objectsToSynchronize, SystemInstance instance, int pageSize) {

    return new TrackerSynchronizationContext(
        skipChangedBefore, objectsToSynchronize, instance, pageSize, Map.of());
  }

  static TrackerSynchronizationContext forEvents(
      Date skipChangedBefore,
      long objectsToSynchronize,
      SystemInstance instance,
      int pageSize,
      Map<String, Set<String>> skipSyncDataElementsByProgramStage) {

    return new TrackerSynchronizationContext(
        skipChangedBefore,
        objectsToSynchronize,
        instance,
        pageSize,
        skipSyncDataElementsByProgramStage);
  }

  boolean hasNoObjectsToSynchronize() {
    return getObjectsToSynchronize() == 0;
  }

  Map<String, Set<String>> getSkipSyncDataElementsByProgramStage() {
    return skipSyncDataElementsByProgramStage;
  }
}

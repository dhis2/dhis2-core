/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.trackedentity;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.program.Program;

/**
 * @author Abyot Asalefew Gizaw
 */
public interface TrackedEntityAttributeStore
    extends IdentifiableObjectStore<TrackedEntityAttribute> {
  String ID = TrackedEntityAttributeStore.class.getName();

  /**
   * Get attributes which are displayed in visit schedule
   *
   * @param displayOnVisitSchedule True/False value
   * @return List of attributes
   */
  List<TrackedEntityAttribute> getByDisplayOnVisitSchedule(boolean displayOnVisitSchedule);

  /**
   * Get attributes which are displayed in visit schedule
   *
   * @return List of attributes
   */
  List<TrackedEntityAttribute> getDisplayInListNoProgram();

  /**
   * Fetches all {@link TrackedEntityAttribute} linked to all {@link TrackedEntityType} present in
   * the system
   *
   * @return a Set of {@link TrackedEntityAttribute}
   */
  Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes();

  /**
   * Retrieve all attributes that are either configured as trigram indexable and at least one of the
   * operators `LIKE` or `EW` is not blocked.
   *
   * @return a Set of {@link TrackedEntityAttribute}
   */
  Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes();

  /**
   * Fetches all {@link TrackedEntityAttribute} UIDs of the given {@link Program}
   *
   * @return a Set of {@link TrackedEntityAttribute} UIDs
   */
  Set<String> getTrackedEntityAttributesInProgram(Program program);
}

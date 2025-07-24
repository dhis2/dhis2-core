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
import javax.annotation.Nonnull;
import org.hisp.dhis.program.Program;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
public interface TrackedEntityAttributeService {
  String ID = TrackedEntityAttributeService.class.getName();

  /**
   * The max length of a value. This is also naturally constrained by the database table, due to the
   * data type: varchar(1200).
   */
  int TEA_VALUE_MAX_LENGTH = 1200;

  /**
   * Adds an {@link TrackedEntityAttribute}
   *
   * @param attribute The to TrackedEntityAttribute add.
   * @return A generated unique id of the added {@link TrackedEntityAttribute} .
   */
  long addTrackedEntityAttribute(TrackedEntityAttribute attribute);

  /**
   * Deletes a {@link TrackedEntityAttribute}.
   *
   * @param attribute the TrackedEntityAttribute to delete.
   */
  void deleteTrackedEntityAttribute(TrackedEntityAttribute attribute);

  /**
   * Updates an {@link TrackedEntityAttribute}.
   *
   * @param attribute the TrackedEntityAttribute to update.
   */
  void updateTrackedEntityAttribute(TrackedEntityAttribute attribute);

  /** returns all programAttributes */
  List<TrackedEntityAttribute> getProgramTrackedEntityAttributes(List<Program> programs);

  /**
   * Returns a {@link TrackedEntityAttribute}.
   *
   * @param id the id of the TrackedEntityAttribute to return.
   * @return the TrackedEntityAttribute with the given id
   */
  TrackedEntityAttribute getTrackedEntityAttribute(long id);

  /**
   * Returns the {@link TrackedEntityAttribute} with the given UID.
   *
   * @param uid the UID.
   * @return the TrackedEntityAttribute with the given UID, or null if no match.
   */
  TrackedEntityAttribute getTrackedEntityAttribute(String uid);

  /**
   * Returns the {@link TrackedEntityAttribute}s with the given UIDs.
   *
   * @param uids list of UIDs.
   * @return all the TrackedEntityAttribute with the given UIDs.
   */
  List<TrackedEntityAttribute> getTrackedEntityAttributes(@Nonnull List<String> uids);

  /**
   * Returns the {@link TrackedEntityAttribute}s with the given UIDs.
   *
   * @param ids list of primary key ids.
   * @return all the TrackedEntityAttribute with the given ids.
   */
  List<TrackedEntityAttribute> getTrackedEntityAttributesById(List<Long> ids);

  /**
   * Returns a {@link TrackedEntityAttribute} with a given name.
   *
   * @param name the name of the TrackedEntityAttribute to return.
   * @return the TrackedEntityAttribute with the given name, or null if no match.
   */
  TrackedEntityAttribute getTrackedEntityAttributeByName(String name);

  /**
   * Returns all {@link TrackedEntityAttribute}
   *
   * @return a list of all TrackedEntityAttribute, or an empty List if there are no
   *     TrackedEntityAttributes.
   */
  List<TrackedEntityAttribute> getAllTrackedEntityAttributes();

  /**
   * Get the tracked entity attributes for given program i.e. program attributes to which the
   * current user must have data read access.
   */
  Set<TrackedEntityAttribute> getProgramAttributes(Program program);

  /**
   * Get the tracked entity attributes for given tracked entity type i.e. tracked entity type
   * attributes to which the current user must have data read access.
   */
  Set<TrackedEntityAttribute> getTrackedEntityTypeAttributes(TrackedEntityType trackedEntityType);

  Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes(
      List<Program> programs, List<TrackedEntityType> trackedEntityTypes);

  /**
   * Returns all {@link TrackedEntityAttribute} that are candidates for creating trigram indexes.
   *
   * @return a set of all TrackedEntityAttribute, or an empty List if there are no
   *     TrackedEntityAttributes that are indexable
   */
  Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes();

  /**
   * Returns all id's of {@link TrackedEntityAttribute} that are candidates for creating trigram
   * indexes.
   *
   * @return a set of all id's of TrackedEntityAttribute, or an empty List if there are no
   *     TrackedEntityAttributes that are indexable
   */
  Set<TrackedEntityAttribute> getAllTrigramIndexableAttributes();

  /**
   * Returns all {@link TrackedEntityAttribute}
   *
   * @return a List of all system wide uniqe TrackedEntityAttribute, or an empty List if there are
   *     no TrackedEntityAttributes.
   */
  List<TrackedEntityAttribute> getAllSystemWideUniqueTrackedEntityAttributes();

  /**
   * Get attributes which are displayed in visit schedule
   *
   * @param displayOnVisitSchedule True/False value
   * @return a list of attributes
   */
  List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
      boolean displayOnVisitSchedule);

  @Transactional(readOnly = true)
  List<TrackedEntityAttribute> getAllUniqueTrackedEntityAttributes();

  /**
   * Get all {@link TrackedEntityAttribute} linked to all {@link TrackedEntityType} present in the
   * system
   *
   * @return a Set of {@link TrackedEntityAttribute}
   */
  Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes();

  /**
   * Fetches all {@link TrackedEntityAttribute} UIDs of the given {@link Program}
   *
   * @return a Set of {@link TrackedEntityAttribute} UIDs
   */
  Set<String> getTrackedEntityAttributesInProgram(@Nonnull Program program);
}

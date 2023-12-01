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
package org.hisp.dhis.dxf2.importsummary;

import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * A "append only" set of {@link ImportConflict}s.
 *
 * @author Jan Bernitt
 */
public interface ImportConflicts {

  /**
   * @return read-only access to the set of conflicts (in order of occurrence)
   */
  Iterable<ImportConflict> getConflicts();

  /**
   * Adds a new conflict to this set of conflicts
   *
   * @param conflict the added conflict
   */
  void addConflict(ImportConflict conflict);

  /**
   * Adds a new conflict to this set of conflicts
   *
   * @param object reference or ID of the object causing the conflict
   * @param message a description of the conflict
   */
  default void addConflict(String object, String message) {
    addConflict(new ImportConflict(object, message));
  }

  /**
   * @return A textual description of all {@link ImportConflict}s in this set
   */
  String getConflictsDescription();

  /**
   * @return Number of grouped conflicts in the set. This can be less than the number of conflicts
   *     added using {@link #addConflict(String, String)} since duplicates are eliminated
   */
  int getConflictCount();

  /**
   * @return The total number of occurred conflicts (no grouping) which is similar to the number of
   *     conflicts added using {@link #addConflict(ImportConflict)}.
   */
  int getTotalConflictOccurrenceCount();

  /**
   * Count number of conflicts occurred for a particular error type.
   *
   * @param errorCode error code to count
   * @return number of total occurred conflicts with the provided {@link ErrorCode}
   */
  default int getConflictOccurrenceCount(ErrorCode errorCode) {
    return StreamSupport.stream(getConflicts().spliterator(), false)
        .filter(c -> c.getErrorCode() == errorCode)
        .mapToInt(ImportConflict::getOccurrenceCount)
        .sum();
  }

  /**
   * Tests if a {@link ImportConflict} with certain qualities exists in this set.
   *
   * @param test the test to perform
   * @return true if it exist, otherwise false
   */
  default boolean hasConflict(Predicate<ImportConflict> test) {
    return StreamSupport.stream(getConflicts().spliterator(), false).anyMatch(test);
  }

  /**
   * @return true, if there are any conflicts in this set, else false
   */
  default boolean hasConflicts() {
    return getConflictCount() > 0;
  }
}

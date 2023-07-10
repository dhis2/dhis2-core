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
package org.hisp.dhis.deduplication;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.trackedentity.TrackedEntity;

public interface PotentialDuplicateStore extends IdentifiableObjectStore<PotentialDuplicate> {
  int getCountPotentialDuplicates(PotentialDuplicateCriteria query);

  List<PotentialDuplicate> getPotentialDuplicates(PotentialDuplicateCriteria query);

  boolean exists(PotentialDuplicate potentialDuplicate) throws PotentialDuplicateConflictException;

  /**
   * Moves the tracked entity attribute values from the "duplicate" tei into the "original" tei.
   * Only the trackedEntityAttributes specified in the argument are considered. If a corresponding
   * trackedEntityAttribute value already exists in the old tei, they are overwritten. If no
   * trackedEntityAttributeValue exists in the old tei, then a new TEAV with the value as in the
   * duplicate is created and the old teav is deleted.
   *
   * @param original The original TEI
   * @param duplicate The duplicate TEI
   * @param trackedEntityAttributes The teas that has to be considered for moving from duplicate to
   *     original
   */
  void moveTrackedEntityAttributeValues(
      TrackedEntity original, TrackedEntity duplicate, List<String> trackedEntityAttributes);

  void moveRelationships(
      TrackedEntity originalUid, TrackedEntity duplicateUid, List<String> relationships);

  void moveEnrollments(TrackedEntity original, TrackedEntity duplicate, List<String> enrollments);

  void removeTrackedEntity(TrackedEntity trackedEntity);

  void auditMerge(DeduplicationMergeParams params);
}

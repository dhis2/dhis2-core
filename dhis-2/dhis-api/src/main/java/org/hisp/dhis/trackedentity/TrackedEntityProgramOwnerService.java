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
package org.hisp.dhis.trackedentity;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * @author Ameen Mohamed
 */
public interface TrackedEntityProgramOwnerService {
  String ID = TrackedEntityProgramOwnerService.class.getName();

  /**
   * Assign an orgUnit as the owner for a tracked entity instance for the given program. If another
   * owner already exist then this method would fail.
   *
   * @param teUid The Uid of the tracked entity instance
   * @param programUid The program Uid
   * @param orgUnitUid The organisation units Uid
   */
  void createTrackedEntityProgramOwner(String teUid, String programUid, String orgUnitUid);

  /**
   * Update the owner ou for a tracked entity instance for the given program. If no owner previously
   * exist, then this method will fail.
   *
   * @param teUid The tracked entity instance Uid
   * @param programUid The program Uid
   * @param orgUnitUid The organisation Unit Uid
   */
  void updateTrackedEntityProgramOwner(String teUid, String programUid, String orgUnitUid);

  /**
   * Assign an orgUnit as the owner for a tracked entity instance for the given program. If another
   * owner already exist then this method would fail.
   *
   * @param teId The Id of the tracked entity instance
   * @param programId The program Id
   * @param orgUnitId The organisation units Id
   */
  void createTrackedEntityProgramOwner(long teId, long programId, long orgUnitId);

  /**
   * Update the owner ou for a tracked entity instance for the given program. If no owner previously
   * exist, then this method will fail.
   *
   * @param teId The tracked entity instance Id
   * @param programId The program Id
   * @param orgUnitId The organisation Unit Id
   */
  void updateTrackedEntityProgramOwner(long teId, long programId, long orgUnitId);

  /**
   * Get the program owner details for a tracked entity instance.
   *
   * @param teId The tracked entity instance Id
   * @param programId The program Id
   * @return The TrackedEntityProgramOwner object
   */
  TrackedEntityProgramOwner getTrackedEntityProgramOwner(long teId, long programId);

  /**
   * Get the program owner details for a tracked entity instance.
   *
   * @param teUid The tracked entity instance Uid
   * @param programUid The program Uid
   * @return The TrackedEntityProgramOwner object
   */
  TrackedEntityProgramOwner getTrackedEntityProgramOwner(String teUid, String programUid);

  /**
   * Assign an orgUnit as the owner for a tracked entity instance for the given program. If another
   * owner already exist then it would be overwritten.
   *
   * @param teUid
   * @param programUid
   * @param orgUnitUid
   */
  void createOrUpdateTrackedEntityProgramOwner(String teUid, String programUid, String orgUnitUid);

  /**
   * Assign an orgUnit as the owner for a tracked entity instance for the given program. If another
   * owner already exist then it would be overwritten.
   *
   * @param teUid
   * @param programUid
   * @param orgUnitUid
   */
  void createOrUpdateTrackedEntityProgramOwner(long teUid, long programUid, long orgUnitUid);

  /**
   * Assign an orgUnit as the owner for a tracked entity instance for the given program. If another
   * owner already exist then it would be overwritten.
   *
   * @param entityInstance
   * @param program
   * @param ou
   */
  void createOrUpdateTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou);

  /**
   * Update the owner ou for a tracked entity instance for the given program. If no owner previously
   * exist, then this method will fail.
   *
   * @param entityInstance
   * @param program
   * @param ou
   */
  void updateTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou);

  /**
   * Create a new program owner ou for a tracked entity instance. If an owner previously exist, then
   * this method will fail.
   *
   * @param entityInstance
   * @param program
   * @param ou
   */
  void createTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou);
}

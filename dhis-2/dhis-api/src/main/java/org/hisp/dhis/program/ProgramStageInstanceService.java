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
package org.hisp.dhis.program;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew
 */
public interface ProgramStageInstanceService {
  String ID = ProgramStageInstanceService.class.getName();

  /**
   * Adds a {@link ProgramStageInstance}
   *
   * @param programStageInstance The ProgramStageInstance to add.
   * @return A generated unique id of the added {@link ProgramStageInstance}.
   */
  long addProgramStageInstance(ProgramStageInstance programStageInstance);

  /**
   * Adds a {@link ProgramStageInstance}
   *
   * @param programStageInstance The ProgramStageInstance to add.
   * @param user the current user.
   * @return A generated unique id of the added {@link ProgramStageInstance}.
   */
  long addProgramStageInstance(ProgramStageInstance programStageInstance, User user);

  /** Soft deletes a {@link ProgramStageInstance}. */
  void deleteProgramStageInstance(ProgramStageInstance programStageInstance);

  /**
   * Updates a {@link ProgramStageInstance}.
   *
   * @param programStageInstance the ProgramStageInstance to update.
   */
  void updateProgramStageInstance(ProgramStageInstance programStageInstance);

  /**
   * Updates a {@link ProgramStageInstance}.
   *
   * @param programStageInstance the ProgramStageInstance to update.
   * @param user the current user.
   */
  void updateProgramStageInstance(ProgramStageInstance programStageInstance, User user);

  /**
   * Updates a last sync timestamp on specified ProgramStageInstances
   *
   * @param programStageInstanceUIDs UIDs of ProgramStageInstances where the lastSynchronized flag
   *     should be updated
   * @param lastSynchronized The date of last successful sync
   */
  void updateProgramStageInstancesSyncTimestamp(
      List<String> programStageInstanceUIDs, Date lastSynchronized);

  /**
   * Checks whether a {@link ProgramStageInstance} with the given identifier exists. Doesn't take
   * into account the deleted values.
   *
   * @param uid the identifier.
   */
  boolean programStageInstanceExists(String uid);

  /**
   * Checks whether a {@link ProgramStageInstance} with the given identifier exists. Takes into
   * accound also the deleted values.
   *
   * @param uid the identifier.
   */
  boolean programStageInstanceExistsIncludingDeleted(String uid);

  /**
   * Returns UIDs of existing ProgramStageInstances (including deleted) from the provided UIDs
   *
   * @param uids PSI UIDs to check
   * @return Set containing UIDs of existing PSIs (including deleted)
   */
  List<String> getProgramStageInstanceUidsIncludingDeleted(List<String> uids);

  /**
   * Returns a {@link ProgramStageInstance}.
   *
   * @param id the id of the ProgramStageInstance to return.
   * @return the ProgramStageInstance with the given id.
   */
  ProgramStageInstance getProgramStageInstance(long id);

  /**
   * Returns the {@link ProgramStageInstance} with the given UID.
   *
   * @param uid the UID.
   * @return the ProgramStageInstance with the given UID, or null if no match.
   */
  ProgramStageInstance getProgramStageInstance(String uid);

  /**
   * Gets the number of ProgramStageInstances added since the given number of days.
   *
   * @param days number of days.
   * @return the number of ProgramStageInstances.
   */
  long getProgramStageInstanceCount(int days);

  /**
   * Creates a program stage instance.
   *
   * @param programInstance the ProgramInstance.
   * @param programStage the ProgramStage.
   * @param enrollmentDate the enrollment date.
   * @param incidentDate date of the incident.
   * @param organisationUnit the OrganisationUnit where the event took place.
   * @return ProgramStageInstance the ProgramStageInstance which was created.
   */
  ProgramStageInstance createProgramStageInstance(
      ProgramInstance programInstance,
      ProgramStage programStage,
      Date enrollmentDate,
      Date incidentDate,
      OrganisationUnit organisationUnit);

  /**
   * Validates EventDataValues, handles files for File EventDataValues and creates audit logs for
   * the upcoming create/save changes. DOES PERSIST the changes to the PSI object.
   *
   * @param programStageInstance the ProgramStageInstance that EventDataValues belong to
   * @param dataElementEventDataValueMap the map of DataElements and related EventDataValues to
   *     update
   */
  void saveEventDataValuesAndSaveProgramStageInstance(
      ProgramStageInstance programStageInstance,
      Map<DataElement, EventDataValue> dataElementEventDataValueMap);
}

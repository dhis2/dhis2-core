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

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService")
public class DefaultTrackedEntityProgramOwnerService implements TrackedEntityProgramOwnerService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final TrackedEntityService trackedEntityService;

  private final ProgramService programService;

  private final OrganisationUnitService orgUnitService;

  private final CurrentUserService currentUserService;

  private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

  @Override
  @Transactional
  public void createTrackedEntityProgramOwner(String teUid, String programUid, String orgUnitUid) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teUid);
    if (entityInstance == null) {
      return;
    }
    Program program = programService.getProgram(programUid);
    if (program == null) {
      return;
    }
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitUid);
    if (ou == null) {
      return;
    }
    trackedEntityProgramOwnerStore.save(
        buildTrackedEntityProgramOwner(entityInstance, program, ou));
  }

  @Override
  @Transactional
  public void createTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou) {
    if (entityInstance == null || program == null || ou == null) {
      return;
    }
    trackedEntityProgramOwnerStore.save(
        buildTrackedEntityProgramOwner(entityInstance, program, ou));
  }

  private TrackedEntityProgramOwner buildTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou) {
    TrackedEntityProgramOwner teProgramOwner =
        new TrackedEntityProgramOwner(entityInstance, program, ou);
    teProgramOwner.updateDates();
    User user = currentUserService.getCurrentUser();
    if (user != null) {
      teProgramOwner.setCreatedBy(user.getUsername());
    }
    return teProgramOwner;
  }

  @Override
  @Transactional
  public void createOrUpdateTrackedEntityProgramOwner(
      String teUid, String programUid, String orgUnitUid) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teUid);
    Program program = programService.getProgram(programUid);
    if (entityInstance == null) {
      return;
    }
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
            entityInstance.getId(), program.getId());
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitUid);
    if (ou == null) {
      return;
    }

    if (teProgramOwner == null) {
      trackedEntityProgramOwnerStore.save(
          buildTrackedEntityProgramOwner(entityInstance, program, ou));
    } else {
      teProgramOwner = updateTrackedEntityProgramOwner(teProgramOwner, ou);
      trackedEntityProgramOwnerStore.update(teProgramOwner);
    }
  }

  @Override
  @Transactional
  public void createOrUpdateTrackedEntityProgramOwner(
      long teUid, long programUid, long orgUnitUid) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teUid);
    Program program = programService.getProgram(programUid);
    if (entityInstance == null) {
      return;
    }
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
            entityInstance.getId(), program.getId());
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitUid);
    if (ou == null) {
      return;
    }

    if (teProgramOwner == null) {
      trackedEntityProgramOwnerStore.save(
          buildTrackedEntityProgramOwner(entityInstance, program, ou));
    } else {
      teProgramOwner = updateTrackedEntityProgramOwner(teProgramOwner, ou);
      trackedEntityProgramOwnerStore.update(teProgramOwner);
    }
  }

  @Override
  @Transactional
  public void createOrUpdateTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou) {
    if (entityInstance == null || program == null || ou == null) {
      return;
    }
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
            entityInstance.getId(), program.getId());
    if (teProgramOwner == null) {
      trackedEntityProgramOwnerStore.save(
          buildTrackedEntityProgramOwner(entityInstance, program, ou));
    } else {
      teProgramOwner = updateTrackedEntityProgramOwner(teProgramOwner, ou);
      trackedEntityProgramOwnerStore.update(teProgramOwner);
    }
  }

  @Override
  @Transactional
  public void updateTrackedEntityProgramOwner(
      TrackedEntity entityInstance, Program program, OrganisationUnit ou) {
    if (entityInstance == null || program == null || ou == null) {
      return;
    }
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
            entityInstance.getId(), program.getId());
    if (teProgramOwner == null) {
      return;
    }
    teProgramOwner = updateTrackedEntityProgramOwner(teProgramOwner, ou);
    trackedEntityProgramOwnerStore.update(teProgramOwner);
  }

  private TrackedEntityProgramOwner updateTrackedEntityProgramOwner(
      TrackedEntityProgramOwner teProgramOwner, OrganisationUnit ou) {
    teProgramOwner.setOrganisationUnit(ou);
    teProgramOwner.updateDates();
    User user = currentUserService.getCurrentUser();
    if (user != null) {
      teProgramOwner.setCreatedBy(user.getUsername());
    }
    return teProgramOwner;
  }

  @Override
  @Transactional
  public void updateTrackedEntityProgramOwner(String teUid, String programUid, String orgUnitUid) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teUid);
    if (entityInstance == null) {
      return;
    }
    Program program = programService.getProgram(programUid);
    if (program == null) {
      return;
    }

    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
            entityInstance.getId(), program.getId());
    if (teProgramOwner == null) {
      return;
    }
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitUid);
    if (ou == null) {
      return;
    }
    teProgramOwner = updateTrackedEntityProgramOwner(teProgramOwner, ou);
    trackedEntityProgramOwnerStore.update(teProgramOwner);
  }

  @Override
  @Transactional
  public void createTrackedEntityProgramOwner(long teId, long programId, long orgUnitId) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teId);
    if (entityInstance == null) {
      return;
    }
    Program program = programService.getProgram(programId);
    if (program == null) {
      return;
    }
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitId);
    if (ou == null) {
      return;
    }
    trackedEntityProgramOwnerStore.save(
        buildTrackedEntityProgramOwner(entityInstance, program, ou));
  }

  @Override
  @Transactional
  public void updateTrackedEntityProgramOwner(long teId, long programId, long orgUnitId) {
    TrackedEntityProgramOwner teProgramOwner =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(teId, programId);
    if (teProgramOwner == null) {
      return;
    }
    OrganisationUnit ou = orgUnitService.getOrganisationUnit(orgUnitId);
    if (ou == null) {
      return;
    }
    trackedEntityProgramOwnerStore.update(updateTrackedEntityProgramOwner(teProgramOwner, ou));
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityProgramOwner getTrackedEntityProgramOwner(long teId, long programId) {
    return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(teId, programId);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityProgramOwner getTrackedEntityProgramOwner(String teUid, String programUid) {
    TrackedEntity entityInstance = trackedEntityService.getTrackedEntity(teUid);
    Program program = programService.getProgram(programUid);
    if (entityInstance == null || program == null) {
      return null;
    }
    return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner(
        entityInstance.getId(), program.getId());
  }
}

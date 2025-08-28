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
package org.hisp.dhis.merge.orgunit.handler;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merge handler for tracker entities.
 *
 * @author Lars Helge Overland
 */
@Component
@RequiredArgsConstructor
public class TrackerOrgUnitMergeHandler {
  private final EntityManager entityManager;

  @Transactional
  public void mergeProgramMessages(OrgUnitMergeRequest request) {
    migrate(
        "update ProgramMessage pm "
            + "set pm.recipients.organisationUnit = :target "
            + "where pm.recipients.organisationUnit.id in (:sources)",
        request);
  }

  @Transactional
  public void mergeSingleEvents(OrgUnitMergeRequest request) {
    migrate(
        "update SingleEvent ev "
            + "set ev.organisationUnit = :target "
            + "where ev.organisationUnit.id in (:sources)",
        request);
  }

  @Transactional
  public void mergeEnrollments(OrgUnitMergeRequest request) {
    migrate(
        "update TrackerEvent ev "
            + "set ev.organisationUnit = :target "
            + "where ev.organisationUnit.id in (:sources)",
        request);

    migrate(
        "update Enrollment en "
            + "set en.organisationUnit = :target "
            + "where en.organisationUnit.id in (:sources)",
        request);
  }

  @Transactional
  public void mergeTrackedEntities(OrgUnitMergeRequest request) {
    migrate(
        "update ProgramOwnershipHistory poh "
            + "set poh.organisationUnit = :target "
            + "where poh.organisationUnit.id in (:sources)",
        request);

    migrate(
        "update TrackedEntityProgramOwner tpo "
            + "set tpo.organisationUnit = :target "
            + "where tpo.organisationUnit.id in (:sources)",
        request);

    migrate(
        "update TrackedEntity te "
            + "set te.organisationUnit = :target "
            + "where te.organisationUnit.id in (:sources)",
        request);
  }

  private void migrate(String hql, OrgUnitMergeRequest request) {
    entityManager
        .createQuery(hql)
        .setParameter("target", request.getTarget())
        .setParameter("sources", IdentifiableObjectUtils.getIdentifiers(request.getSources()))
        .executeUpdate();
  }
}

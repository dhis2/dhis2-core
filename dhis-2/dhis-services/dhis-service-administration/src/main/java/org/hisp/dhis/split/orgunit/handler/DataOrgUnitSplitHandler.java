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
package org.hisp.dhis.split.orgunit.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DataOrgUnitSplitHandler {
  private static final String PARAM_ORG_UNIT = "organisationUnit";

  private static final String PARAM_SOURCE = "source";

  private final SessionFactory sessionFactory;

  @Transactional
  public void splitData(OrgUnitSplitRequest request) {
    migrate(request, "DataValueAudit", PARAM_ORG_UNIT);
    migrate(request, "DataValue", PARAM_SOURCE);
    migrate(request, "DataApprovalAudit", PARAM_ORG_UNIT);
    migrate(request, "DataApproval", PARAM_ORG_UNIT);
    migrate(request, "LockException", PARAM_ORG_UNIT);
    migrate(request, "ValidationResult", PARAM_ORG_UNIT);
    migrate(request, "MinMaxDataElement", PARAM_SOURCE);
    migrate(request, "Interpretation", PARAM_ORG_UNIT);

    migrate(request, "ProgramMessage", "recipients." + PARAM_ORG_UNIT);
    migrate(request, "ProgramStageInstance", PARAM_ORG_UNIT);
    migrate(request, "ProgramInstance", PARAM_ORG_UNIT);
    migrate(request, "ProgramOwnershipHistory", PARAM_ORG_UNIT);
    migrate(request, "TrackedEntityProgramOwner", PARAM_ORG_UNIT);
    migrate(request, "TrackedEntityInstance", PARAM_ORG_UNIT);
  }

  private void migrate(OrgUnitSplitRequest request, String entity, String property) {
    String hql =
        String.format(
            "update %s e set e.%s = :target where e.%s = :source", entity, property, property);

    log.debug("Update data HQL: '{}'", hql);

    sessionFactory
        .getCurrentSession()
        .createQuery(hql)
        .setParameter("source", request.getSource())
        .setParameter("target", request.getPrimaryTarget())
        .executeUpdate();
  }
}

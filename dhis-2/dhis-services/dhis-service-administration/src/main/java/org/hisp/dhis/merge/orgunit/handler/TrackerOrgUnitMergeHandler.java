/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.merge.orgunit.handler;

import javax.transaction.Transactional;

import lombok.AllArgsConstructor;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class TrackerOrgUnitMergeHandler
{
    private SessionFactory sessionFactory;

    public void mergeProgramMessages( OrgUnitMergeRequest request )
    {
        migrate( "update ProgramMessage pm " +
            "set pm.recipients.organisationUnit = :target " +
            "where pm.recipients.organisationUnit.id in (:sources)", request );
    }

    public void mergeProgramInstances( OrgUnitMergeRequest request )
    {
        migrate( "update ProgramStageInstance psi " +
            "set psi.organisationUnit = :target " +
            "where psi.organisationUnit.id in (:sources)", request );

        migrate( "update ProgramInstance pi " +
            "set pi.organisationUnit = :target " +
            "where pi.organisationUnit.id in (:sources)", request );
    }

    public void mergeTrackedEntityInstances( OrgUnitMergeRequest request )
    {
        migrate( "update ProgramOwnershipHistory poh " +
            "set poh.organisationUnit = :target " +
            "where poh.organisationUnit.id in (:sources)", request );

        migrate( "update TrackedEntityProgramOwner tpo " +
            "set tpo.organisationUnit = :target " +
            "where tpo.organisationUnit.id in (:sources)", request );

        migrate( "update TrackedEntityInstance tei " +
            "set tei.organisationUnit = :target " +
            "where tei.organisationUnit.id in (:sources)", request );
    }

    private void migrate( String hql, OrgUnitMergeRequest request )
    {
        getQuery( hql )
            .setParameter( "target", request.getTarget() )
            .setParameterList( "sources", IdentifiableObjectUtils.getIdentifiers( request.getSources() ) )
            .executeUpdate();
    }

    private Query<?> getQuery( String hql )
    {
        return sessionFactory
            .getCurrentSession()
            .createQuery( hql );
    }
}

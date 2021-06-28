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
package org.hisp.dhis.split.orgunit.handler;

import javax.transaction.Transactional;

import lombok.AllArgsConstructor;

import org.hibernate.SessionFactory;
import org.hisp.dhis.split.orgunit.OrgUnitSplitRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class DataOrgUnitSplitHandler
{
    private final SessionFactory sessionFactory;

    @Transactional
    public void splitData( OrgUnitSplitRequest request )
    {
        migrate( request, "DataValueAudit", "organisationUnit" );
        migrate( request, "DataValue", "source" );
        migrate( request, "DataApprovalAudit", "organisationUnit" );
        migrate( request, "DataApproval", "organisationUnit" );
        migrate( request, "LockException", "organisationUnit" );
        migrate( request, "ValidationResult", "organisationUnit" );
        migrate( request, "MinMaxDataElement", "source" );
        migrate( request, "Interpretation", "organisationUnit" );

        migrate( request, "ProgramMessage", "recipients.organisationUnit" );
        migrate( request, "ProgramStageInstance", "organisationUnit" );
        migrate( request, "ProgramInstance", "organisationUnit" );
        migrate( request, "ProgramOwnershipHistory", "organisationUnit" );
        migrate( request, "TrackedEntityProgramOwner", "organisationUnit" );
        migrate( request, "TrackedEntityInstance", "organisationUnit" );
    }

    private void migrate( OrgUnitSplitRequest request, String entity, String property )
    {
        String hql = String.format(
            "update %s e set e.%s = :target where e.%s = :source",
            entity, property, property );

        sessionFactory.getCurrentSession().createQuery( hql )
            .setParameter( "source", request.getSource() )
            .setParameter( "target", request.getPrimaryTarget() )
            .executeUpdate();
    }
}

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
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.validation.ValidationResultService;
import org.hisp.dhis.validation.ValidationResultsDeletionRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class DataOrgUnitMergeHandler
{
    private SessionFactory sessionFactory;

    private DataSetService dataSetService;

    private ValidationResultService validationResultService;

    private MinMaxDataElementService minMaxDataElementService;

    public void mergeInterpretations( OrgUnitMergeRequest request )
    {
        migrate( "update Interpretation i " +
            "set i.organisationUnit = :target " +
            "where i.organisationUnit.id in (:sources)", request );
    }

    public void mergeLockExceptions( OrgUnitMergeRequest request )
    {
        request.getSources().forEach( ou -> dataSetService.deleteLockExceptions( ou ) );
    }

    public void mergeValidationResults( OrgUnitMergeRequest request )
    {
        ValidationResultsDeletionRequest deletionRequest = new ValidationResultsDeletionRequest();
        deletionRequest.setOu( IdentifiableObjectUtils.getUids( request.getSources() ) );

        validationResultService.deleteValidationResults( deletionRequest );
    }

    public void mergeMinMaxDataElements( OrgUnitMergeRequest request )
    {
        request.getSources().forEach( ou -> minMaxDataElementService.removeMinMaxDataElements( ou ) );
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

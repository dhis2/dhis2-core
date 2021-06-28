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
package org.hisp.dhis.split.orgunit;

import java.util.Set;

import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.split.orgunit.handler.AnalyticalObjectOrgUnitSplitHandler;
import org.hisp.dhis.split.orgunit.handler.DataOrgUnitSplitHandler;
import org.hisp.dhis.split.orgunit.handler.MetadataOrgUnitSplitHandler;
import org.hisp.dhis.split.orgunit.handler.TrackerOrgUnitSplitHandler;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * Main class for org unit split.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
public class DefaultOrgUnitSplitService
    implements OrgUnitSplitService
{
    private final OrgUnitSplitValidator validator;

    private final IdentifiableObjectManager idObjectManager;

    private final ImmutableList<OrgUnitSplitHandler> handlers;

    public DefaultOrgUnitSplitService( OrgUnitSplitValidator validator,
        IdentifiableObjectManager idObjectManager,
        MetadataOrgUnitSplitHandler metadataHandler,
        AnalyticalObjectOrgUnitSplitHandler analyticalObjectHandler,
        DataOrgUnitSplitHandler dataHandler,
        TrackerOrgUnitSplitHandler trackerHandler )
    {
        this.validator = validator;
        this.idObjectManager = idObjectManager;
        this.handlers = getSplitHandlers( metadataHandler,
            analyticalObjectHandler, dataHandler, trackerHandler );
    }

    @Override
    @Transactional
    public void split( OrgUnitSplitRequest request )
    {
        log.info( "Org unit split request: {}", request );

        validator.validate( request );

        handlers.forEach( handler -> handler.split( request ) );

        // Persistence framework will inspect and update associated objects

        for ( OrganisationUnit target : request.getTargets() )
        {
            idObjectManager.update( target );
        }

        handleDeleteSource( request );

        log.info( "Org unit split operation done: {}", request );
    }

    @Override
    public OrgUnitSplitRequest getFromQuery( OrgUnitSplitQuery query )
    {
        OrganisationUnit source = idObjectManager.get( OrganisationUnit.class, query.getSource() );

        Set<OrganisationUnit> targets = Sets.newHashSet(
            idObjectManager.getByUid( OrganisationUnit.class, query.getTargets() ) );

        return new OrgUnitSplitRequest.Builder()
            .withSource( source )
            .addTargets( targets )
            .withDeleteSource( query.getDeleteSource() )
            .build();
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private ImmutableList<OrgUnitSplitHandler> getSplitHandlers(
        MetadataOrgUnitSplitHandler metadataHandler,
        AnalyticalObjectOrgUnitSplitHandler analyticalObjectHandler,
        DataOrgUnitSplitHandler dataHandler,
        TrackerOrgUnitSplitHandler trackerHandler )
    {
        return ImmutableList.<OrgUnitSplitHandler> builder()
            .build();
    }

    /**
     * Handles deletion of the source {@link OrganisationUnit}.
     *
     * @param request the {@link OrgUnitSplitRequest}.
     */
    private void handleDeleteSource( OrgUnitSplitRequest request )
    {
        if ( request.isDeleteSource() )
        {
            idObjectManager.delete( request.getSource() );
        }
    }
}

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
package org.hisp.dhis.tracker.validation;

import java.util.Optional;

import lombok.Data;

import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;

// TODO is this class really needed? what is the purpose of this class and why aren't the two caches moved to preheat?
/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
public class TrackerImportValidationContext
{
    private TrackerBundle bundle;

    public TrackerImportValidationContext( TrackerBundle bundle )
    {
        // Create a copy of the bundle
        this.bundle = bundle;
    }

    public TrackerImportStrategy getStrategy( TrackerDto dto )
    {
        return bundle.getResolvedStrategyMap().get( dto.getTrackerType() ).get( dto.getUid() );
    }

    public OrganisationUnit getOrganisationUnit( String id )
    {
        return bundle.getPreheat().get( OrganisationUnit.class, id );
    }

    public TrackedEntityInstance getTrackedEntityInstance( String id )
    {
        return bundle.getPreheat().getTrackedEntity( bundle.getIdentifier(), id );
    }

    public TrackedEntityAttribute getTrackedEntityAttribute( String id )
    {
        return bundle.getPreheat().get( TrackedEntityAttribute.class, id );
    }

    public TrackedEntityType getTrackedEntityType( String id )
    {
        return bundle.getPreheat().get( TrackedEntityType.class, id );
    }

    public Program getProgram( String id )
    {
        return bundle.getPreheat().get( Program.class, id );
    }

    public ProgramInstance getProgramInstance( String id )
    {
        return bundle.getPreheat().getEnrollment( bundle.getIdentifier(), id );
    }

    public ProgramStage getProgramStage( String id )
    {
        return bundle.getPreheat().get( ProgramStage.class, id );
    }

    public ProgramStageInstance getProgramStageInstance( String event )
    {
        return bundle.getPreheat().getEvent( bundle.getIdentifier(), event );
    }

    // remove
    public FileResource getFileResource( String id )
    {
        return bundle.getPreheat().get( FileResource.class, id );
    }

    public Optional<ReferenceTrackerEntity> getReference( String uid )
    {
        return bundle.getPreheat().getReference( uid );
    }

}

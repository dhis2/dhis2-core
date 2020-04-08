package org.hisp.dhis.tracker.preheat;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class PreheatHelper
{
    private PreheatHelper()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static OrganisationUnit getOrganisationUnit( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), OrganisationUnit.class, id );
    }

    public static TrackedEntityInstance getTei( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().getTrackedEntity( bundle.getIdentifier(), id );
    }

    public static TrackedEntityAttribute getTrackedEntityAttribute( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), TrackedEntityAttribute.class, id );
    }

    public static TrackedEntityType getTrackedEntityType( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), TrackedEntityType.class, id );
    }

    public static Program getProgram( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), Program.class, id );
    }

    public static ProgramInstance getProgramInstance( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().getEnrollment( bundle.getIdentifier(), id );
    }

    public static ProgramStage getProgramStage( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), ProgramStage.class, id );
    }

    public static ProgramStageInstance getProgramStageInstance( TrackerBundle bundle, String event )
    {
        return bundle.getPreheat().getEvent( bundle.getIdentifier(), event );
    }

    public static CategoryOptionCombo getCategoryOptionCombo( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), CategoryOptionCombo.class, id );
    }

    public static CategoryOption getCategoryOption( TrackerBundle bundle, String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), CategoryOption.class, id );
    }
}
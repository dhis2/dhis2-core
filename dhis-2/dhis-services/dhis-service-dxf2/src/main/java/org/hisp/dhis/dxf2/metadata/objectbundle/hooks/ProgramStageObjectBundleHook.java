package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProgramStageObjectBundleHook
        extends AbstractObjectBundleHook
{
    private final ProgramStageService programStageService;

    private final ProgramStageSectionService programStageSectionService;

    private final AclService aclService;

    public ProgramStageObjectBundleHook( ProgramStageService programStageService, ProgramStageSectionService programStageSectionService, AclService aclService )
    {
        checkNotNull( aclService );
        checkNotNull( programStageService );
        checkNotNull( programStageSectionService );
        this.aclService = aclService;
        this.programStageSectionService = programStageSectionService;
        this.programStageService = programStageService;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( object == null || !object.getClass().isAssignableFrom( ProgramStage.class ) )
        {
            return new ArrayList<>();
        }

        ProgramStage programStage = ( ProgramStage ) object;

        List<ErrorReport> errors = new ArrayList<>();

        errors.addAll( validateProgramStageDataElementsAcl( programStage, bundle ) );

        return errors;
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T object, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( object ) )
        {
            return;
        }

        ProgramStage programStage = ( ProgramStage ) object;

        updateProgramStageSections( programStage );
    }

    private void updateProgramStageSections( ProgramStage programStage )
    {
        if ( programStage.getProgramStageSections().isEmpty() )
        {
            return;
        }

        programStage.getProgramStageSections().forEach( pss -> {
            if ( Objects.isNull( pss.getProgramStage() ) )
            {
                pss.setProgramStage( programStage );
            }

            programStageSectionService.saveProgramStageSection( pss );
        });

        programStageService.saveProgramStage( programStage );
    }

    private List<ErrorReport> validateProgramStageDataElementsAcl( ProgramStage programStage, ObjectBundle bundle )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( programStage.getProgramStageDataElements().isEmpty() )
        {
            return errors;
        }

        PreheatIdentifier identifier = bundle.getPreheatIdentifier();

        programStage.getProgramStageDataElements().forEach( de -> {

            DataElement dataElement = bundle.getPreheat().get( identifier, de.getDataElement() );

            if ( dataElement == null || !aclService.canRead( bundle.getUser(), de ) )
            {
                errors.add( new ErrorReport( DataElement.class, ErrorCode.E3012, identifier.getIdentifiersWithName( bundle.getUser() ),
                    identifier.getIdentifiersWithName( de ) ) );
            }
        } );

        return errors;
    }
}

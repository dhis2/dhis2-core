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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
public class ProgramStageObjectBundleHook
    extends AbstractObjectBundleHook
{
    private final AclService aclService;

    private final ProgramStageSectionService programStageSectionService;

    public ProgramStageObjectBundleHook( AclService aclService, ProgramStageSectionService programStageSectionService )
    {
        checkNotNull( aclService );
        this.aclService = aclService;
        this.programStageSectionService = programStageSectionService;
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( object == null || !object.getClass().isAssignableFrom( ProgramStage.class ) )
        {
            return new ArrayList<>();
        }

        ProgramStage programStage = (ProgramStage) object;

        List<ErrorReport> errors = new ArrayList<>();

        if ( programStage.getNextScheduleDate() != null )
        {
            DataElement nextScheduleDate = bundle.getPreheat().get( bundle.getPreheatIdentifier(), DataElement.class,
                programStage.getNextScheduleDate().getUid() );

            if ( !programStage.getDataElements().contains( programStage.getNextScheduleDate() )
                || nextScheduleDate == null || !nextScheduleDate.getValueType().equals( ValueType.DATE ) )
            {
                errors.add( new ErrorReport( ProgramStage.class, ErrorCode.E6001, programStage.getUid(),
                    programStage.getNextScheduleDate().getUid() ) );
            }
        }

        errors.addAll( validateProgramStageDataElementsAcl( programStage, bundle ) );

        if ( programStage.getProgram() == null && !checkProgramReference( programStage.getUid(), bundle ) )
        {
            errors.add( new ErrorReport( ProgramStage.class, ErrorCode.E4053, programStage.getUid() ) );
        }

        return errors;
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T object, ObjectBundle bundle )
    {
        if ( !ProgramStage.class.isInstance( object ) )
        {
            return;
        }

        ProgramStage programStage = (ProgramStage) object;

        Session session = sessionFactory.getCurrentSession();

        updateProgramStageSections( session, programStage );
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( object == null || !object.getClass().isAssignableFrom( ProgramStage.class ) )
            return;

        deleteRemovedSection( (ProgramStage) persistedObject, (ProgramStage) object );
    }

    private void deleteRemovedSection( ProgramStage persistedProgramStage, ProgramStage importProgramStage )
    {
        List<String> importIds = importProgramStage.getProgramStageSections().stream()
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        List<ProgramStageSection> programStageSectionsToDelete = persistedProgramStage.getProgramStageSections()
            .stream()
            .filter( section -> !importIds.contains( section.getUid() ) )
            .peek( programStageSectionService::deleteProgramStageSection )
            .collect( Collectors.toList() );

        persistedProgramStage.getProgramStageSections()
            .removeAll( programStageSectionsToDelete );
    }

    private void updateProgramStageSections( Session session, ProgramStage programStage )
    {
        if ( programStage.getProgramStageSections().isEmpty() )
        {
            return;
        }

        programStage.getProgramStageSections().stream().forEach( pss -> {
            if ( pss.getProgramStage() == null )
            {
                pss.setProgramStage( programStage );
            }
        } );

        session.update( programStage );
    }

    private List<ErrorReport> validateProgramStageDataElementsAcl( ProgramStage programStage, ObjectBundle bundle )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( programStage.getDataElements().isEmpty() )
        {
            return errors;
        }

        PreheatIdentifier identifier = bundle.getPreheatIdentifier();

        programStage.getDataElements().forEach( de -> {

            DataElement dataElement = bundle.getPreheat().get( identifier, de );

            if ( dataElement == null || !aclService.canRead( bundle.getUser(), de ) )
            {
                errors.add( new ErrorReport( DataElement.class, ErrorCode.E3012,
                    identifier.getIdentifiersWithName( bundle.getUser() ),
                    identifier.getIdentifiersWithName( de ) ) );
            }
        } );

        return errors;
    }

    /**
     * Check if current ProgramStage has reference from a Program in same
     * payload.
     */
    private boolean checkProgramReference( String programStageId, ObjectBundle objectBundle )
    {
        List<IdentifiableObject> persistedObjects = objectBundle.getObjects( Program.class, true );
        List<IdentifiableObject> nonPersistedObjects = objectBundle.getObjects( Program.class, false );

        Iterable<IdentifiableObject> programs = Iterables.concat( persistedObjects, nonPersistedObjects );

        for ( IdentifiableObject program : programs )
        {
            if ( ((Program) program).getProgramStages().stream()
                .anyMatch( ps -> ps.getUid().equals( programStageId ) ) )
            {
                return true;
            }
        }

        return false;
    }
}
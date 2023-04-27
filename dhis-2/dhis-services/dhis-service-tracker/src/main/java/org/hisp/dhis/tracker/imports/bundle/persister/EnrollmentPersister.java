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
package org.hisp.dhis.tracker.imports.bundle.persister;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import org.hibernate.Session;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.converter.TrackerSideEffectConverterService;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentPersister extends AbstractTrackerPersister<Enrollment, ProgramInstance>
{
    private final TrackerConverterService<Enrollment, ProgramInstance> enrollmentConverter;

    private final TrackedEntityCommentService trackedEntityCommentService;

    private final TrackerSideEffectConverterService sideEffectConverterService;

    private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    public EnrollmentPersister( ReservedValueService reservedValueService,
        TrackerConverterService<Enrollment, ProgramInstance> enrollmentConverter,
        TrackedEntityCommentService trackedEntityCommentService,
        TrackerSideEffectConverterService sideEffectConverterService,
        TrackedEntityProgramOwnerService trackedEntityProgramOwnerService,
        TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService )
    {
        super( reservedValueService, trackedEntityAttributeValueAuditService );

        this.enrollmentConverter = enrollmentConverter;
        this.trackedEntityCommentService = trackedEntityCommentService;
        this.sideEffectConverterService = sideEffectConverterService;
        this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
    }

    @Override
    protected void updateAttributes( Session session, TrackerPreheat preheat,
        Enrollment enrollment, ProgramInstance programInstance )
    {
        handleTrackedEntityAttributeValues( session, preheat, enrollment.getAttributes(),
            preheat.getTrackedEntity( programInstance.getEntityInstance().getUid() ) );
    }

    @Override
    protected void updateDataValues( Session session, TrackerPreheat preheat,
        Enrollment enrollment, ProgramInstance programInstance )
    {
        // DO NOTHING - TEI HAVE NO DATA VALUES
    }

    @Override
    protected void persistComments( TrackerPreheat preheat, ProgramInstance programInstance )
    {
        if ( !programInstance.getComments().isEmpty() )
        {
            for ( TrackedEntityComment comment : programInstance.getComments() )
            {
                if ( Objects.isNull( preheat.getNote( comment.getUid() ) ) )
                {
                    this.trackedEntityCommentService.addTrackedEntityComment( comment );
                }
            }
        }
    }

    @Override
    protected void updatePreheat( TrackerPreheat preheat, ProgramInstance programInstance )
    {
        preheat.putEnrollments( Collections.singletonList( programInstance ) );
        preheat.addProgramOwner( programInstance.getEntityInstance().getUid(), programInstance.getProgram().getUid(),
            programInstance.getOrganisationUnit() );
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, String uid )
    {
        return preheat.getEnrollment( uid ) == null;
    }

    @Override
    protected TrackerSideEffectDataBundle handleSideEffects( TrackerBundle bundle, ProgramInstance programInstance )
    {
        return TrackerSideEffectDataBundle.builder()
            .klass( ProgramInstance.class )
            .enrollmentRuleEffects(
                sideEffectConverterService.toTrackerSideEffects( bundle.getEnrollmentRuleEffects() ) )
            .eventRuleEffects( new HashMap<>() )
            .object( programInstance.getUid() )
            .importStrategy( bundle.getImportStrategy() )
            .accessedBy( bundle.getUsername() )
            .programInstance( programInstance )
            .program( programInstance.getProgram() )
            .build();
    }

    @Override
    protected ProgramInstance convert( TrackerBundle bundle, Enrollment enrollment )
    {
        return enrollmentConverter.from( bundle.getPreheat(), enrollment );
    }

    @Override
    protected TrackerType getType()
    {
        return TrackerType.ENROLLMENT;
    }

    @Override
    protected void persistOwnership( TrackerPreheat preheat, ProgramInstance entity )
    {
        if ( isNew( preheat, entity.getUid() ) )
        {
            if ( preheat.getProgramOwner().get( entity.getEntityInstance().getUid() ) == null
                || preheat.getProgramOwner().get( entity.getEntityInstance().getUid() )
                    .get( entity.getProgram().getUid() ) == null )
            {
                trackedEntityProgramOwnerService.createTrackedEntityProgramOwner( entity.getEntityInstance(),
                    entity.getProgram(), entity.getOrganisationUnit() );
            }
        }
    }

    @Override
    protected String getUpdatedTrackedEntity( ProgramInstance entity )
    {
        return entity.getEntityInstance().getUid();
    }
}

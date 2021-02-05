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
package org.hisp.dhis.tracker.bundle.persister;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleHook;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerSideEffectConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
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

    public EnrollmentPersister( List<TrackerBundleHook> bundleHooks, ReservedValueService reservedValueService,
        TrackerConverterService<Enrollment, ProgramInstance> enrollmentConverter,
        TrackedEntityCommentService trackedEntityCommentService,
        TrackerSideEffectConverterService sideEffectConverterService )
    {
        super( bundleHooks, reservedValueService );

        this.enrollmentConverter = enrollmentConverter;
        this.trackedEntityCommentService = trackedEntityCommentService;
        this.sideEffectConverterService = sideEffectConverterService;
    }

    @Override
    protected void updateAttributes( Session session, TrackerPreheat preheat,
        Enrollment enrollment, ProgramInstance programInstance )
    {
        handleTrackedEntityAttributeValues( session, preheat, enrollment.getAttributes(),
            programInstance.getEntityInstance() );
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
        preheat.putEnrollments( TrackerIdScheme.UID, Collections.singletonList( programInstance ) );
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, String uid )
    {
        return preheat.getEnrollment( TrackerIdScheme.UID, uid ) == null;
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
            .build();
    }

    @Override
    protected ProgramInstance convert( TrackerBundle bundle, Enrollment enrollment )
    {
        Date now = new Date();
        ProgramInstance programInstance = enrollmentConverter.from( bundle.getPreheat(), enrollment );
        programInstance.setLastUpdated( now );
        programInstance.setLastUpdatedBy( bundle.getUser() );
        return programInstance;
    }

    @Override
    protected TrackerType getType()
    {
        return TrackerType.ENROLLMENT;
    }

    @Override
    protected void runPostCreateHooks( TrackerBundle bundle )
    {
        bundle.getEnrollments()
            .forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Enrollment.class, o, bundle ) ) );
    }

    @Override
    protected void runPreCreateHooks( TrackerBundle bundle )
    {
        bundle.getEnrollments()
            .forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Enrollment.class, o, bundle ) ) );
    }
}

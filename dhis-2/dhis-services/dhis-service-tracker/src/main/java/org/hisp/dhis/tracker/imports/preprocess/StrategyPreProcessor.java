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
package org.hisp.dhis.tracker.imports.preprocess;

import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the correct strategy for every
 * tracker object.
 *
 * @author Enrico Colasante
 */
@Component
public class StrategyPreProcessor
    implements BundlePreProcessor
{
    @Override
    public void process( TrackerBundle bundle )
    {
        preProcessTrackedEntities( bundle );
        preProcessEnrollments( bundle );
        preProcessEvents( bundle );
        preProcessRelationships( bundle );
    }

    public void preProcessTrackedEntities( TrackerBundle bundle )
    {
        for ( TrackedEntity tei : bundle.getTrackedEntities() )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();

            TrackedEntityInstance existingTei = bundle.getPreheat().getTrackedEntity(
                tei.getTrackedEntity() );

            if ( importStrategy.isCreateAndUpdate() )
            {
                if ( existingTei == null )
                {
                    bundle.setStrategy( tei, TrackerImportStrategy.CREATE );
                }
                else
                {
                    bundle.setStrategy( tei, TrackerImportStrategy.UPDATE );
                }
            }
            else
            {
                bundle.setStrategy( tei, importStrategy );
            }
        }
    }

    public void preProcessEnrollments( TrackerBundle bundle )
    {
        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();

            ProgramInstance existingPI = bundle.getPreheat().getEnrollment(
                enrollment.getEnrollment() );

            if ( importStrategy.isCreateAndUpdate() )
            {
                if ( existingPI == null )
                {
                    bundle.setStrategy( enrollment, TrackerImportStrategy.CREATE );
                }
                else
                {
                    bundle.setStrategy( enrollment, TrackerImportStrategy.UPDATE );
                }
            }
            else
            {
                bundle.setStrategy( enrollment, importStrategy );
            }
        }
    }

    public void preProcessEvents( TrackerBundle bundle )
    {
        for ( org.hisp.dhis.tracker.imports.domain.Event event : bundle.getEvents() )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();

            Event existingEvent = bundle.getPreheat().getEvent( event.getEvent() );

            if ( importStrategy.isCreateAndUpdate() )
            {
                if ( existingEvent == null )
                {
                    bundle.setStrategy( event, TrackerImportStrategy.CREATE );
                }
                else
                {
                    bundle.setStrategy( event, TrackerImportStrategy.UPDATE );
                }
            }
            else
            {
                bundle.setStrategy( event, importStrategy );
            }
        }
    }

    public void preProcessRelationships( TrackerBundle bundle )
    {
        for ( Relationship relationship : bundle.getRelationships() )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();
            org.hisp.dhis.relationship.Relationship existingRelationship = bundle.getPreheat()
                .getRelationship( relationship.getUid() );

            if ( importStrategy.isCreateAndUpdate() )
            {
                if ( existingRelationship == null )
                {
                    bundle.setStrategy( relationship, TrackerImportStrategy.CREATE );
                }
                else
                {
                    bundle.setStrategy( relationship, TrackerImportStrategy.UPDATE );
                }
            }
            else
            {
                bundle.setStrategy( relationship, importStrategy );
            }

        }
    }
}

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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1002;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1063;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1114;

import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ExistenceValidator
    implements Validator<TrackedEntity>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        TrackerImportStrategy importStrategy = bundle.getStrategy( trackedEntity );

        TrackedEntityInstance existingTe = bundle.getPreheat().getTrackedEntity( trackedEntity.getTrackedEntity() );

        // If the tracked entity is soft-deleted no operation is allowed
        if ( existingTe != null && existingTe.isDeleted() )
        {
            reporter.addError( trackedEntity, E1114, trackedEntity.getTrackedEntity() );
            return;
        }

        if ( existingTe != null && importStrategy.isCreate() )
        {
            reporter.addError( trackedEntity, E1002, trackedEntity.getTrackedEntity() );
        }
        else if ( existingTe == null && importStrategy.isUpdateOrDelete() )
        {
            reporter.addError( trackedEntity, E1063, trackedEntity.getTrackedEntity() );
        }
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return true;
    }

}

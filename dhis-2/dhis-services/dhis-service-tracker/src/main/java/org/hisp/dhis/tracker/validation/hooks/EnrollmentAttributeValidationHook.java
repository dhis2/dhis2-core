package org.hisp.dhis.tracker.validation.hooks;

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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentAttributeValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 104;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            reporter.increment( enrollment );

            Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, enrollment.getTrackedEntityInstance() );

            // NOTE: maybe this should qualify as a hard break, on the prev hook (required properties).
            if ( program == null || trackedEntityInstance == null )
            {
                continue;
            }

            validateEnrollmentAttributes( reporter, program, enrollment, trackedEntityInstance, bundle );
        }

        return reporter.getReportList();
    }

    protected void validateEnrollmentAttributes( ValidationErrorReporter errorReporter,
        Program program,
        Enrollment enrollment,
        TrackedEntityInstance trackedEntityInstance,
        TrackerBundle bundle )
    {
        Map<String, String> attributeValueMap = Maps.newHashMap();

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );

            TrackedEntityAttribute teAttribute = PreheatHelper
                .getTrackedEntityAttribute( bundle, attribute.getAttribute() );
            if ( teAttribute == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1017 ) );
                continue;
            }

            validateAttrValueType( errorReporter, attribute, teAttribute );
        }

        Map<TrackedEntityAttribute, Boolean> mandatoryMap = Maps.newHashMap();
        for ( ProgramTrackedEntityAttribute programTrackedEntityAttribute : program.getProgramAttributes() )
        {
            mandatoryMap.put( programTrackedEntityAttribute.getAttribute(),
                programTrackedEntityAttribute.isMandatory() );
        }

        // ignore attributes which do not belong to this program
        trackedEntityInstance.getTrackedEntityAttributeValues().stream().
            filter( value -> mandatoryMap.containsKey( value.getAttribute() ) ).
            forEach( value -> attributeValueMap.put( value.getAttribute().getUid(), value.getValue() ) );

        for ( TrackedEntityAttribute trackedEntityAttribute : mandatoryMap.keySet() )
        {
            Boolean mandatory = mandatoryMap.get( trackedEntityAttribute );

            boolean hasMissingAttribute = mandatory &&
                !bundle.getUser().isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() )
                // NOTE This seams a bit out of place???
                && !attributeValueMap.containsKey( trackedEntityAttribute.getUid() );

            if ( hasMissingAttribute )
            {
                // Missing mandatory attribute: `{0}`.
                errorReporter.addError( newReport( TrackerErrorCode.E1018 )
                    .addArg( trackedEntityAttribute ) );
            }

            // NOTE: This is "THE" potential performance killer...
            // "Error validating attribute, not unique; Error `{0}`"
            validateAttributeUniqueness( errorReporter,
                attributeValueMap.get( trackedEntityAttribute.getUid() ),
                trackedEntityAttribute,
                trackedEntityInstance.getUid(),
                trackedEntityInstance.getOrganisationUnit() );

            attributeValueMap.remove( trackedEntityAttribute.getUid() );
        }

        if ( !attributeValueMap.isEmpty() )
        {
            // Only program attributes is allowed for enrollment; Non valid attributes: `{0}`.
            errorReporter.addError( newReport( TrackerErrorCode.E1019 )
                .addArg( Joiner.on( "," ).withKeyValueSeparator( "=" ).join( attributeValueMap ) ) );
        }
    }

}

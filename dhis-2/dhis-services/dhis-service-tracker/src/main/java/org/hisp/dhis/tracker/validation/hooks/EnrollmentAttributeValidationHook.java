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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1017;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1018;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1019;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1075;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;

import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentAttributeValidationHook extends AttributeValidationHook
{

    public EnrollmentAttributeValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

        Map<String, String> attributeValueMap = Maps.newHashMap();

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            validateRequiredProperties( reporter, attribute );

            if ( attribute.getAttribute() == null || attribute.getValue() == null ||
                context.getTrackedEntityAttribute( attribute.getAttribute() ) == null )
            {
                continue;
            }

            attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );

            TrackedEntityAttribute teAttribute = context.getTrackedEntityAttribute( attribute.getAttribute() );

            validateAttrValueType( reporter, attribute, teAttribute );

            validateAttributeUniqueness( reporter,
                attribute.getValue(),
                teAttribute,
                tei,
                tei.getOrganisationUnit() );
        }

        Program program = context.getProgram( enrollment.getProgram() );
        validateMandatoryAttributes( reporter, program, attributeValueMap );
    }

    protected void validateRequiredProperties( ValidationErrorReporter reporter, Attribute attribute )
    {
        addErrorIfNull( attribute.getAttribute(), reporter, E1075, attribute );
        addErrorIfNull( attribute.getValue(), reporter, E1076, attribute );

        if ( attribute.getAttribute() != null )
        {
            TrackedEntityAttribute teAttribute = reporter.getValidationContext()
                .getTrackedEntityAttribute( attribute.getAttribute() );

            addErrorIfNull( teAttribute, reporter, E1017, attribute );
        }
    }

    private void validateMandatoryAttributes( ValidationErrorReporter reporter,
        Program program, Map<String, String> attributeValueMap )
    {
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        // Map having as key program attribute uid and mandatory flag as value
        Map<String, Boolean> programAttributesMap = program.getProgramAttributes().stream()
            .collect( Collectors.toMap(
                programTrackedEntityAttribute -> programTrackedEntityAttribute.getAttribute().getUid(),
                ProgramTrackedEntityAttribute::isMandatory ) );

        // attributeValueMap (attributes from enrollments) must contain all
        // programAttributeMap entries which are mandatory
        programAttributesMap.entrySet()
            .stream()
            // filter on mandatory flag
            .filter( Map.Entry::getValue )
            .map( Map.Entry::getKey )
            .forEach( mandatoryAttributeUid -> addErrorIf( () -> !attributeValueMap.containsKey( mandatoryAttributeUid ),
                reporter, E1018, mandatoryAttributeUid ) );

        attributeValueMap.keySet()
            .forEach( enrollmentAttribute -> addErrorIf( () -> !programAttributesMap.containsKey( enrollmentAttribute ),
                reporter, E1019, enrollmentAttribute + "=" + attributeValueMap.get( enrollmentAttribute ) ) );
    }
}
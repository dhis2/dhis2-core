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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1006;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1018;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1019;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1075;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1076;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateOptionSet;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.imports.validation.service.attribute.TrackedAttributeValidationService;
import org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component( "org.hisp.dhis.tracker.imports.validation.validator.enrollment.AttributeValidator" )
class AttributeValidator extends org.hisp.dhis.tracker.imports.validation.validator.AttributeValidator
    implements Validator<Enrollment>
{

    public AttributeValidator( TrackedAttributeValidationService teAttrService,
        DhisConfigurationProvider dhisConfigurationProvider )
    {
        super( teAttrService, dhisConfigurationProvider );
    }

    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        TrackerPreheat preheat = bundle.getPreheat();
        Program program = preheat.getProgram( enrollment.getProgram() );
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        TrackedEntityInstance tei = bundle.getPreheat().getTrackedEntity( enrollment.getTrackedEntity() );

        OrganisationUnit orgUnit = preheat
            .getOrganisationUnit( getOrgUnitUidFromTei( bundle, enrollment.getTrackedEntity() ) );

        Map<MetadataIdentifier, String> attributeValueMap = Maps.newHashMap();

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            validateRequiredProperties( reporter, preheat, enrollment, attribute, program );

            TrackedEntityAttribute teAttribute = bundle.getPreheat()
                .getTrackedEntityAttribute( attribute.getAttribute() );

            if ( attribute.getAttribute().isNotBlank() && attribute.getValue() != null && teAttribute != null )
            {

                attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );
                validateAttributeValue( reporter, enrollment, teAttribute, attribute.getValue() );
                validateAttrValueType( reporter, bundle.getPreheat(), enrollment, attribute, teAttribute );
                validateOptionSet( reporter, enrollment, teAttribute,
                    attribute.getValue() );

                validateAttributeUniqueness( reporter,
                    preheat,
                    enrollment,
                    attribute.getValue(),
                    teAttribute,
                    tei,
                    orgUnit );
            }
        }

        validateMandatoryAttributes( reporter, bundle, program, attributeValueMap, enrollment );
    }

    protected void validateRequiredProperties( Reporter reporter, TrackerPreheat preheat,
        Enrollment enrollment,
        Attribute attribute, Program program )
    {
        if ( attribute.getAttribute().isBlank() )
        {
            reporter.addError( enrollment, E1075, attribute );
            return;
        }

        Optional<ProgramTrackedEntityAttribute> optionalTrackedAttr = program.getProgramAttributes().stream()
            .filter( pa -> attribute.getAttribute().isEqualTo( pa.getAttribute() ) && pa.isMandatory() )
            .findFirst();

        if ( optionalTrackedAttr.isPresent() )
        {
            reporter.addErrorIfNull( attribute.getValue(), enrollment, E1076,
                TrackedEntityAttribute.class.getSimpleName(), attribute.getAttribute() );
        }

        TrackedEntityAttribute teAttribute = preheat.getTrackedEntityAttribute( attribute.getAttribute() );
        reporter.addErrorIfNull( teAttribute, enrollment, E1006, attribute.getAttribute() );
    }

    private void validateMandatoryAttributes( Reporter reporter, TrackerBundle bundle,
        Program program, Map<MetadataIdentifier, String> enrollmentNonEmptyAttributes, Enrollment enrollment )
    {
        // Build a data structures of attributes eligible for mandatory
        // validations:
        // 1 - attributes from enrollments whose value is not empty or null
        // 2 - attributes already existing in TEI (from preheat)

        // 1 - attributes from enrollment whose value is non-empty

        // 2 - attributes from existing TEI (if any) from preheat
        Set<MetadataIdentifier> teiAttributes = buildTeiAttributes( bundle, enrollment.getTrackedEntity() );

        // merged ids of eligible attributes to validate
        Set<MetadataIdentifier> mergedAttributes = Streams
            .concat( enrollmentNonEmptyAttributes.keySet().stream(), teiAttributes.stream() )
            .collect( Collectors.toSet() );

        // Map having as key program attribute and mandatory flag as value
        TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
        Map<MetadataIdentifier, Boolean> programAttributesMap = program.getProgramAttributes().stream()
            .collect( Collectors.toMap(
                programTrackedEntityAttribute -> idSchemes
                    .toMetadataIdentifier( programTrackedEntityAttribute.getAttribute() ),
                ProgramTrackedEntityAttribute::isMandatory ) );

        // Merged attributes must contain each mandatory program attribute.
        programAttributesMap.entrySet()
            .stream()
            .filter( Map.Entry::getValue ) // <--- filter on mandatory flag
            .map( Map.Entry::getKey )
            .forEach( mandatoryProgramAttribute -> reporter.addErrorIf(
                () -> !mergedAttributes.contains( mandatoryProgramAttribute ),
                enrollment, E1018, mandatoryProgramAttribute, program.getUid(), enrollment.getEnrollment() ) );

        // enrollment must not contain any attribute which is not defined in
        // program
        enrollmentNonEmptyAttributes
            .forEach(
                ( attrId, attrVal ) -> reporter.addErrorIf( () -> !programAttributesMap.containsKey( attrId ),
                    enrollment, E1019, attrId.getIdentifierOrAttributeValue() + "=" + attrVal ) );
    }

    private Set<MetadataIdentifier> buildTeiAttributes( TrackerBundle bundle,
        String trackedEntityInstanceUid )
    {
        TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
        return Optional.of( bundle )
            .map( TrackerBundle::getPreheat )
            .map( trackerPreheat -> trackerPreheat.getTrackedEntity( trackedEntityInstanceUid ) )
            .map( TrackedEntityInstance::getTrackedEntityAttributeValues )
            .orElse( Collections.emptySet() )
            .stream()
            .map( TrackedEntityAttributeValue::getAttribute )
            .map( idSchemes::toMetadataIdentifier )
            .collect( Collectors.toSet() );
    }

    private MetadataIdentifier getOrgUnitUidFromTei( TrackerBundle bundle, String teiUid )
    {
        return bundle.findTrackedEntityByUid( teiUid )
            .map( TrackedEntity::getOrgUnit )
            .orElse( null );
    }
}
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
package org.hisp.dhis.tracker.validation.hooks;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1006;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1090;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook extends AttributeValidationHook
{
    public TrackedEntityAttributeValidationHook( TrackedAttributeValidationService teAttrService,
        DhisConfigurationProvider dhisConfigurationProvider )
    {
        super( teAttrService, dhisConfigurationProvider );
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        TrackedEntityType trackedEntityType = bundle.getPreheat()
            .getTrackedEntityType( trackedEntity.getTrackedEntityType() );

        TrackedEntityInstance tei = bundle.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
        OrganisationUnit organisationUnit = bundle.getPreheat()
            .getOrganisationUnit( trackedEntity.getOrgUnit() );

        validateMandatoryAttributes( reporter, bundle, trackedEntity, trackedEntityType );
        validateAttributes( reporter, bundle, trackedEntity, tei, organisationUnit, trackedEntityType );
    }

    private void validateMandatoryAttributes( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity,
        TrackedEntityType trackedEntityType )
    {
        if ( trackedEntityType != null )
        {
            Set<MetadataIdentifier> trackedEntityAttributes = trackedEntity.getAttributes()
                .stream()
                .map( Attribute::getAttribute )
                .collect( Collectors.toSet() );

            TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
            trackedEntityType.getTrackedEntityTypeAttributes()
                .stream()
                .filter( trackedEntityTypeAttribute -> Boolean.TRUE.equals( trackedEntityTypeAttribute.isMandatory() ) )
                .map( TrackedEntityTypeAttribute::getTrackedEntityAttribute )
                .map( idSchemes::toMetadataIdentifier )
                .filter( mandatoryAttribute -> !trackedEntityAttributes.contains( mandatoryAttribute ) )
                .forEach(
                    attribute -> reporter.addError( trackedEntity, E1090, attribute, trackedEntityType.getUid(),
                        trackedEntity.getTrackedEntity() ) );
        }
    }

    protected void validateAttributes( ValidationErrorReporter reporter,
        TrackerBundle bundle, TrackedEntity trackedEntity, TrackedEntityInstance tei, OrganisationUnit orgUnit,
        TrackedEntityType trackedEntityType )
    {
        checkNotNull( trackedEntity, TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        TrackerPreheat preheat = bundle.getPreheat();
        Map<MetadataIdentifier, TrackedEntityAttributeValue> valueMap = new HashMap<>();
        if ( tei != null )
        {
            TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
            valueMap = tei.getTrackedEntityAttributeValues()
                .stream()
                .collect( Collectors.toMap( v -> idSchemes.toMetadataIdentifier( v.getAttribute() ), v -> v ) );
        }

        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            TrackedEntityAttribute tea = preheat.getTrackedEntityAttribute( attribute.getAttribute() );

            if ( tea == null )
            {
                reporter.addError( trackedEntity, E1006, attribute.getAttribute() );
                continue;
            }

            if ( attribute.getValue() == null )
            {
                Optional<TrackedEntityTypeAttribute> optionalTea = Optional.of( trackedEntityType )
                    .map( tet -> tet.getTrackedEntityTypeAttributes().stream() )
                    .flatMap( tetAtts -> tetAtts.filter(
                        teaAtt -> attribute.getAttribute().isEqualTo( teaAtt.getTrackedEntityAttribute() )
                            && teaAtt.isMandatory() != null && teaAtt.isMandatory() )
                        .findFirst() );

                if ( optionalTea.isPresent() )
                    reporter.addError( trackedEntity, E1076, TrackedEntityAttribute.class.getSimpleName(),
                        attribute.getAttribute() );

                continue;
            }

            validateAttributeValue( reporter, trackedEntity, tea, attribute.getValue() );
            validateAttrValueType( reporter, preheat, trackedEntity, attribute, tea );
            validateOptionSet( reporter, trackedEntity, tea, attribute.getValue() );

            validateAttributeUniqueness( reporter, preheat, trackedEntity, attribute.getValue(), tea, tei, orgUnit );

            validateFileNotAlreadyAssigned( reporter, bundle, trackedEntity, attribute, valueMap );
        }
    }

    protected void validateFileNotAlreadyAssigned( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity te,
        Attribute attr, Map<MetadataIdentifier, TrackedEntityAttributeValue> valueMap )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );

        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();
        if ( !attrIsFile )
        {
            return;
        }

        TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( attr.getAttribute() );

        // Todo: how can this be possible? is this acceptable?
        if ( trackedEntityAttributeValue != null &&
            !trackedEntityAttributeValue.getAttribute().getValueType().isFile() )
        {
            return;
        }

        FileResource fileResource = bundle.getPreheat().get( FileResource.class, attr.getValue() );

        reporter.addErrorIfNull( fileResource, te, E1084, attr.getValue() );

        if ( bundle.getStrategy( te ).isCreate() )
        {
            reporter.addErrorIf( () -> fileResource != null && fileResource.isAssigned(), te, E1009, attr.getValue() );
        }

        if ( bundle.getStrategy( te ).isUpdate() )
        {
            reporter.addErrorIf(
                () -> fileResource != null && fileResource.getFileResourceOwner() != null
                    && !fileResource.getFileResourceOwner().equals( te.getUid() ),
                te, E1009, attr.getValue() );
        }
    }
}

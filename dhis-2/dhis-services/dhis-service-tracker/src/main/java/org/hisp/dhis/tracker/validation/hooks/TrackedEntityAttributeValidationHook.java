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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 2;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( bundle.getImportStrategy().isDelete() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( TrackedEntity trackedEntity : bundle.getTrackedEntities() )
        {
            reporter.increment( trackedEntity );

            TrackedEntityInstance tei = PreheatHelper
                .getTrackedEntityInstance( bundle, trackedEntity.getTrackedEntity() );

            OrganisationUnit orgUnit = getOrganisationUnit( bundle, trackedEntity );

            validateAttributes( reporter, bundle, trackedEntity, tei, orgUnit );
        }

        return reporter.getReportList();
    }

    protected void validateAttributes( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity te, TrackedEntityInstance tei, OrganisationUnit orgUnit )
    {

        // For looking up existing tei attr. ie. if it is an update. Could/should this be done in the preheater instead?
        Map<String, TrackedEntityAttributeValue> valueMap = getTeiAttributeValueMap(
            trackedEntityAttributeValueService.getTrackedEntityAttributeValues( tei ) );

        for ( Attribute attribute : te.getAttributes() )
        {
            if ( StringUtils.isEmpty( attribute.getValue() ) )
            {
                continue;
            }

            TrackedEntityAttribute trackedEntityAttribute = PreheatHelper
                .getTrackedEntityAttribute( bundle, attribute.getAttribute() );
            if ( trackedEntityAttribute == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1006 )
                    .addArg( attribute.getAttribute() ) );
                continue;
            }

            TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( trackedEntityAttribute.getUid() );

            validateTextPattern( errorReporter, attribute, trackedEntityAttribute, trackedEntityAttributeValue );

            validateAttrValueType( errorReporter, attribute, trackedEntityAttribute );

            validateAttrUnique( errorReporter,
                attribute.getValue(),
                trackedEntityAttribute,
                te.getTrackedEntity(),
                orgUnit );

            validateFileNotAlreadyAssigned( errorReporter, attribute, tei );
        }
    }
}

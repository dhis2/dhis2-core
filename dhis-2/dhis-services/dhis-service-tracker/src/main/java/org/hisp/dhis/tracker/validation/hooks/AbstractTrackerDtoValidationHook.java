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

import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.DATE_STRING_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.GEOMETRY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerDtoValidationHook
    implements TrackerValidationHook
{
    public static final String IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD = "Implementing class fail to override this method!";

    @Autowired
    protected TrackedEntityAttributeService teAttrService;

    private final TrackerImportStrategy strategy;

    private final boolean removeOnError;

    private final Class<?> dtoTypeClass;

    public AbstractTrackerDtoValidationHook()
    {
        this.removeOnError = true;
        this.dtoTypeClass = null;
        this.strategy = null;
    }

    public <T extends TrackerDto> AbstractTrackerDtoValidationHook( Class<T> dtoClass, TrackerImportStrategy strategy )
    {
        this.removeOnError = false;
        this.dtoTypeClass = dtoClass;
        this.strategy = strategy;
    }

    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerImportValidationContext context )
    {
        TrackerBundle bundle = context.getBundle();

        ValidationErrorReporter reporter = new ValidationErrorReporter( context, this.getClass() );

        if ( this.strategy != null )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();

            if ( importStrategy.isDelete() && !this.strategy.isDelete() )
            {
                return reporter.getReportList();
            }
        }

        if ( dtoTypeClass == null || dtoTypeClass.equals( TrackedEntity.class ) )
        {
            validateTrackerDTOs( reporter, ( o, r ) -> validateTrackedEntity( r, o ),
                bundle.getTrackedEntities() );
        }

        if ( dtoTypeClass == null || dtoTypeClass.equals( Enrollment.class ) )
        {
            validateTrackerDTOs( reporter, ( o, r ) -> validateEnrollment( r, o ), bundle.getEnrollments() );
        }

        if ( dtoTypeClass == null || dtoTypeClass.equals( Event.class ) )
        {
            validateTrackerDTOs( reporter, ( o, r ) -> validateEvent( r, o ), bundle.getEvents() );
        }

        return reporter.getReportList();
    }

    public <T extends TrackerDto> void validateTrackerDTOs( ValidationErrorReporter reporter,
        ValidationFunction<T> function, List<T> dtoInstances )
    {
        Iterator<T> iterator = dtoInstances.iterator();

        while ( iterator.hasNext() )
        {
            T dto = iterator.next();

            // Fork the report in order to be thread-safe so we can support multi-threaded validation in future.
            // Iterator needs to be changed to split variant also...
            ValidationErrorReporter reportFork = reporter.fork( dto );

            function.validateTrackerDto( dto, reportFork );

            if ( this.removeOnError && reportFork.hasErrors() )
            {
                iterator.remove();
            }

            reporter.merge( reportFork );
        }
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        Objects.requireNonNull( attr, ATTRIBUTE_CANT_BE_NULL );
        Objects.requireNonNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        String error = teAttrService.validateValueType( teAttr, attr.getValue() );
        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( error ) );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstanceUid,
        OrganisationUnit organisationUnit )
    {
        Objects.requireNonNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
        {
            return;
        }

        String error = teAttrService.validateAttributeUniquenessWithinScope(
            trackedEntityAttribute,
            value,
            trackedEntityInstanceUid,
            organisationUnit );

        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1064 )
                .addArg( error ) );
        }
    }

    protected void validateGeometry( ValidationErrorReporter errorReporter, Geometry geometry, FeatureType featureType )
    {
        Objects.requireNonNull( geometry, GEOMETRY_CANT_BE_NULL );

        if ( featureType == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1074 ) );
            return;
        }

        FeatureType typeFromName = FeatureType.getTypeFromName( geometry.getGeometryType() );

        if ( FeatureType.NONE == featureType || featureType != typeFromName )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1012 )
                .addArg( featureType.name() ) );
        }
    }

//    protected OrganisationUnit getOrganisationUnit( TrackerBundle bundle, TrackedEntity te )
//    {
//        Objects.requireNonNull( bundle, TRACKER_BUNDLE_CANT_BE_NULL );
//        Objects.requireNonNull( te, TRACKED_ENTITY_CANT_BE_NULL );
//
//        TrackedEntityInstance trackedEntityInstance = PreheatHelper
//            .getTei( bundle, te.getTrackedEntity() );
//
//        OrganisationUnit organisationUnit =
//            trackedEntityInstance != null ? trackedEntityInstance.getOrganisationUnit() : null;
//
//        return bundle.getImportStrategy().isCreateOrCreateAndUpdate()
//            ? PreheatHelper.getOrganisationUnit( bundle, te.getOrgUnit() )
//            : organisationUnit;
//    }
//
//    protected TrackedEntityType getTrackedEntityType( TrackerBundle bundle, TrackedEntity te )
//    {
//        Objects.requireNonNull( bundle, TRACKER_BUNDLE_CANT_BE_NULL );
//        Objects.requireNonNull( te, TRACKED_ENTITY_CANT_BE_NULL );
//
//        TrackedEntityInstance trackedEntityInstance = PreheatHelper
//            .getTei( bundle, te.getTrackedEntity() );
//
//        TrackedEntityType trackedEntityType =
//            trackedEntityInstance != null ? trackedEntityInstance.getTrackedEntityType() : null;
//
//        return bundle.getImportStrategy().isCreateOrCreateAndUpdate()
//            ? PreheatHelper.getTrackedEntityType( bundle, te.getTrackedEntityType() )
//            : trackedEntityType;
//    }

    public boolean isNotValidDateString( String dateString )
    {
        Objects.requireNonNull( dateString, DATE_STRING_CANT_BE_NULL );

        return !DateUtils.dateIsValid( dateString );
    }

    public boolean isValidDateStringAndNotNull( String dateString )
    {
        return dateString != null && DateUtils.dateIsValid( dateString );
    }
}

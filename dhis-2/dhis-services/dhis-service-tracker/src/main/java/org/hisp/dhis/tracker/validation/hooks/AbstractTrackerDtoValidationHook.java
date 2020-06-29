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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.DATE_STRING_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.GEOMETRY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerDtoValidationHook
    implements TrackerValidationHook
{
    private int order = Ordered.LOWEST_PRECEDENCE;

    @Override
    public int getOrder()
    {
        return order;
    }

    public void setOrder( int order )
    {
        this.order = order;
    }

    public static final String IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD = "Implementing class fail to override this method!";

    @Autowired
    protected TrackedEntityAttributeService teAttrService;

    @Autowired
    private TrackedEntityCommentService commentService;

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
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        String error;

        // We need to do try/catch here since validateValueType() since validateValueType can cast IllegalArgumentException e.g.
        // on at org.joda.time.format.DateTimeFormatter.parseDateTime(DateTimeFormatter.java:945)
        try
        {
            error = teAttrService.validateValueType( teAttr, attr.getValue() );
        }
        catch ( Exception e )
        {
            error = e.getMessage();
        }

        if ( error != null )
        {
            ValueType valueType = teAttr.getValueType();
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( valueType.toString() )
                .addArg( error ) );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstanceUid,
        OrganisationUnit organisationUnit )
    {
        checkNotNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
            return;

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
        checkNotNull( geometry, GEOMETRY_CANT_BE_NULL );

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

    protected void validateNotes( ValidationErrorReporter reporter, TrackerImportStrategy strategy, List<Note> notes )
    {
        for ( Note note : notes )
        {
            boolean validUid = CodeGenerator.isValidUid( note.getNote() );
            if ( !validUid )
            {
                reporter.addError( newReport( TrackerErrorCode.E1118 )
                    .addArg( note.toString() ) );
            }

            if ( strategy.isCreate() )
            {
                //TODO: This looks like a potential performance killer, existence check on every note...
                //TODO: Note persistence not impl. yet.
                boolean alreadyExists = commentService.trackedEntityCommentExists( note.getNote() );
                if ( alreadyExists )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1120 )
                        .addArg( note.toString() ) );
                }
            }

            boolean emptyValue = StringUtils.isEmpty( note.getValue() );
            if ( emptyValue )
            {
                reporter.addError( newReport( TrackerErrorCode.E1119 )
                    .addArg( note.toString() ) );
            }

            Date stored = null;
            Exception error = null;
            try
            {
                stored = DateUtils.parseDate( note.getStoredAt() );
            }
            catch ( Exception e )
            {
                error = e;
            }
            if ( stored == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1121 )
                    .addArg( note.toString() )
                    .addArg( error != null ? error.getMessage() : "" )
                );
            }
        }
    }

    public boolean isNotValidDateString( String dateString )
    {
        checkNotNull( dateString, DATE_STRING_CANT_BE_NULL );

        return !DateUtils.dateIsValid( dateString );
    }

    public boolean isValidDateStringAndNotNull( String dateString )
    {
        return dateString != null && DateUtils.dateIsValid( dateString );
    }
}

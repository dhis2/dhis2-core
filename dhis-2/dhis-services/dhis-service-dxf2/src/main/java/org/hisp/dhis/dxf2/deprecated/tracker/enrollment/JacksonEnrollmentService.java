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
package org.hisp.dhis.dxf2.deprecated.tracker.enrollment;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.relationship.RelationshipService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.events.enrollment.EnrollmentService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
@Transactional
public class JacksonEnrollmentService extends AbstractEnrollmentService
{
    public JacksonEnrollmentService(
        EnrollmentService enrollmentService,
        EventService programStageInstanceService,
        ProgramService programService,
        TrackedEntityInstanceService trackedEntityInstanceService,
        TrackerOwnershipManager trackerOwnershipAccessManager,
        RelationshipService relationshipService,
        TrackedEntityService teiService,
        TrackedEntityAttributeService trackedEntityAttributeService,
        TrackedEntityAttributeValueService trackedEntityAttributeValueService,
        CurrentUserService currentUserService,
        TrackedEntityCommentService commentService,
        IdentifiableObjectManager manager,
        I18nManager i18nManager,
        UserService userService,
        DbmsManager dbmsManager,
        org.hisp.dhis.dxf2.deprecated.tracker.event.EventService eventService,
        TrackerAccessManager trackerAccessManager,
        SchemaService schemaService,
        QueryService queryService,
        Notifier notifier,
        ApplicationEventPublisher eventPublisher,
        ObjectMapper jsonMapper,
        @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper )
    {
        checkNotNull( enrollmentService );
        checkNotNull( programStageInstanceService );
        checkNotNull( programService );
        checkNotNull( trackedEntityInstanceService );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( relationshipService );
        checkNotNull( teiService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( trackedEntityAttributeValueService );
        checkNotNull( currentUserService );
        checkNotNull( commentService );
        checkNotNull( manager );
        checkNotNull( i18nManager );
        checkNotNull( userService );
        checkNotNull( dbmsManager );
        checkNotNull( eventService );
        checkNotNull( trackerAccessManager );
        checkNotNull( schemaService );
        checkNotNull( queryService );
        checkNotNull( notifier );
        checkNotNull( eventPublisher );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );

        this.enrollmentService = enrollmentService;
        this.programStageInstanceService = programStageInstanceService;
        this.programService = programService;
        this.trackedEntityInstanceService = trackedEntityInstanceService;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.relationshipService = relationshipService;
        this.teiService = teiService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.trackedEntityAttributeValueService = trackedEntityAttributeValueService;
        this.currentUserService = currentUserService;
        this.commentService = commentService;
        this.manager = manager;
        this.i18nManager = i18nManager;
        this.userService = userService;
        this.dbmsManager = dbmsManager;
        this.eventService = eventService;
        this.trackerAccessManager = trackerAccessManager;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.notifier = notifier;
        this.eventPublisher = eventPublisher;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }
    // -------------------------------------------------------------------------
    // EnrollmentService Impl
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) xmlMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( String input, Class<?> clazz )
        throws IOException
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) jsonMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( String input, Class<?> clazz )
        throws IOException
    {
        return (T) jsonMapper.readValue( input, clazz );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public List<Enrollment> getEnrollmentsJson( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseJsonEnrollments( input );
    }

    @Override
    public List<Enrollment> getEnrollmentsXml( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseXmlEnrollments( input );
    }

    @Override
    public ImportSummaries addEnrollmentsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Enrollment> enrollments = parseJsonEnrollments( input );

        return addEnrollmentList( enrollments, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addEnrollmentsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Enrollment> enrollments = parseXmlEnrollments( input );

        return addEnrollmentList( enrollments, updateImportOptions( importOptions ) );
    }

    private List<Enrollment> parseJsonEnrollments( String input )
        throws IOException
    {
        List<Enrollment> enrollments = new ArrayList<>();

        JsonNode root = jsonMapper.readTree( input );

        if ( root.get( "enrollments" ) != null )
        {
            Enrollments fromJson = fromJson( input, Enrollments.class );
            enrollments.addAll( fromJson.getEnrollments() );
        }
        else
        {
            Enrollment fromJson = fromJson( input, Enrollment.class );
            enrollments.add( fromJson );
        }

        return enrollments;
    }

    private List<Enrollment> parseXmlEnrollments( String input )
        throws IOException
    {
        List<Enrollment> enrollments = new ArrayList<>();

        try
        {
            Enrollments fromXml = fromXml( input, Enrollments.class );
            enrollments.addAll( fromXml.getEnrollments() );
        }
        catch ( JsonMappingException ex )
        {
            Enrollment fromXml = fromXml( input, Enrollment.class );
            enrollments.add( fromXml );
        }

        return enrollments;
    }

    @Override
    public ImportSummaries addEnrollmentList( List<Enrollment> enrollments, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );

        List<Enrollment> create = new ArrayList<>();
        List<Enrollment> update = new ArrayList<>();
        List<Enrollment> delete = new ArrayList<>();

        if ( importOptions.getImportStrategy().isCreate() )
        {
            create.addAll( enrollments );
        }
        else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
        {
            sortCreatesAndUpdates( enrollments, create, update );
        }
        else if ( importOptions.getImportStrategy().isUpdate() )
        {
            update.addAll( enrollments );
        }
        else if ( importOptions.getImportStrategy().isDelete() )
        {
            delete.addAll( enrollments );
        }
        else if ( importOptions.getImportStrategy().isSync() )
        {
            for ( Enrollment enrollment : enrollments )
            {
                if ( enrollment.isDeleted() )
                {
                    delete.add( enrollment );
                }
                else
                {
                    sortCreatesAndUpdates( enrollment, create, update );
                }
            }
        }

        importSummaries.addImportSummaries( addEnrollments( create, importOptions, null, true ) );
        importSummaries.addImportSummaries( updateEnrollments( update, importOptions, true ) );
        importSummaries.addImportSummaries( deleteEnrollments( delete, importOptions, true ) );

        if ( ImportReportMode.ERRORS == importOptions.getReportMode() )
        {
            importSummaries.getImportSummaries().removeIf( is -> !is.hasConflicts() );
        }

        return importSummaries;
    }

    private void sortCreatesAndUpdates( List<Enrollment> enrollments, List<Enrollment> create, List<Enrollment> update )
    {
        List<String> ids = enrollments.stream().map( Enrollment::getEnrollment ).collect( Collectors.toList() );
        List<String> existingUids = enrollmentService.getEnrollmentsUidsIncludingDeleted( ids );

        for ( Enrollment enrollment : enrollments )
        {
            if ( StringUtils.isEmpty( enrollment.getEnrollment() )
                || !existingUids.contains( enrollment.getEnrollment() ) )
            {
                create.add( enrollment );
            }
            else
            {
                update.add( enrollment );
            }
        }
    }

    private void sortCreatesAndUpdates( Enrollment enrollment, List<Enrollment> create, List<Enrollment> update )
    {
        if ( StringUtils.isEmpty( enrollment.getEnrollment() ) )
        {
            create.add( enrollment );
        }
        else
        {
            if ( !enrollmentService.enrollmentExists( enrollment.getEnrollment() ) )
            {
                create.add( enrollment );
            }
            else
            {
                update.add( enrollment );
            }
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary updateEnrollmentJson( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        Enrollment enrollment = fromJson( inputStream, Enrollment.class );
        enrollment.setEnrollment( id );

        return updateEnrollment( enrollment, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummary updateEnrollmentForNoteJson( String id, InputStream inputStream )
        throws IOException
    {
        Enrollment enrollment = fromJson( inputStream, Enrollment.class );
        enrollment.setEnrollment( id );

        return updateEnrollmentForNote( enrollment );
    }

    @Override
    public ImportSummary updateEnrollmentXml( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        Enrollment enrollment = fromXml( inputStream, Enrollment.class );
        enrollment.setEnrollment( id );

        return updateEnrollment( enrollment, updateImportOptions( importOptions ) );
    }
}

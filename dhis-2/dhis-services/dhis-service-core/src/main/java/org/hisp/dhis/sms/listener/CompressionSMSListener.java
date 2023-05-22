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
package org.hisp.dhis.sms.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.SmsSubmissionReader;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmissionHeader;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Slf4j
@Transactional
public abstract class CompressionSMSListener
    extends
    BaseSMSListener
{
    protected abstract SmsResponse postProcess( IncomingSms sms, SmsSubmission submission )
        throws SMSProcessingException;

    protected abstract boolean handlesType( SubmissionType type );

    protected final UserService userService;

    protected final TrackedEntityTypeService trackedEntityTypeService;

    protected final TrackedEntityAttributeService trackedEntityAttributeService;

    protected final ProgramService programService;

    protected final OrganisationUnitService organisationUnitService;

    protected final CategoryService categoryService;

    protected final DataElementService dataElementService;

    protected final EventService eventService;

    protected final IdentifiableObjectManager identifiableObjectManager;

    public CompressionSMSListener( IncomingSmsService incomingSmsService, MessageSender smsSender,
        UserService userService, TrackedEntityTypeService trackedEntityTypeService,
        TrackedEntityAttributeService trackedEntityAttributeService, ProgramService programService,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        DataElementService dataElementService, EventService eventService,
        IdentifiableObjectManager identifiableObjectManager )
    {
        super( incomingSmsService, smsSender );

        checkNotNull( userService );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( programService );
        checkNotNull( organisationUnitService );
        checkNotNull( categoryService );
        checkNotNull( dataElementService );
        checkNotNull( eventService );

        this.userService = userService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.programService = programService;
        this.organisationUnitService = organisationUnitService;
        this.categoryService = categoryService;
        this.dataElementService = dataElementService;
        this.eventService = eventService;
        this.identifiableObjectManager = identifiableObjectManager;
    }

    @Override
    public boolean accept( IncomingSms sms )
    {
        if ( sms == null || !SmsUtils.isBase64( sms ) )
        {
            return false;
        }

        SmsSubmissionHeader header = getHeader( sms );
        if ( header == null )
        {
            // If the header is null we simply accept any listener
            // and handle the error in receive() below
            return true;
        }
        return handlesType( header.getType() );
    }

    @Override
    public void receive( IncomingSms sms )
    {
        SmsSubmissionReader reader = new SmsSubmissionReader();
        SmsSubmissionHeader header = getHeader( sms );
        if ( header == null )
        {
            // Error with the header, we have no message ID, use -1
            sendSMSResponse( SmsResponse.HEADER_ERROR, sms, -1 );
            return;
        }

        SmsMetadata meta = getMetadata( header.getLastSyncDate() );
        SmsSubmission subm = null;
        try
        {
            subm = reader.readSubmission( SmsUtils.getBytes( sms ), meta );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            sendSMSResponse( SmsResponse.READ_ERROR, sms, header.getSubmissionId() );
            return;
        }

        // TODO: Can be removed - debugging line to check Sms submissions
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        log.info( String.format( "New received Sms submission decoded as: %s", gson.toJson( subm ) ) );

        SmsResponse resp = null;
        try
        {
            checkUser( subm );
            resp = postProcess( sms, subm );
        }
        catch ( SMSProcessingException e )
        {
            log.error( e.getMessage() );
            sendSMSResponse( e.getResp(), sms, header.getSubmissionId() );
            return;
        }

        log.info( String.format( "Sms Response: ", resp.toString() ) );
        sendSMSResponse( resp, sms, header.getSubmissionId() );
    }

    private void checkUser( SmsSubmission subm )
    {
        Uid userid = subm.getUserId();
        User user = userService.getUser( userid.getUid() );

        if ( user == null )
        {
            throw new SMSProcessingException( SmsResponse.INVALID_USER.set( userid ) );
        }
    }

    private SmsSubmissionHeader getHeader( IncomingSms sms )
    {
        byte[] smsBytes = SmsUtils.getBytes( sms );
        SmsSubmissionReader reader = new SmsSubmissionReader();
        try
        {
            return reader.readHeader( smsBytes );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            log.error( e.getMessage() );
            return null;
        }
    }

    private SmsMetadata getMetadata( Date lastSyncDate )
    {
        SmsMetadata meta = new SmsMetadata();
        meta.dataElements = getTypeUidsBefore( DataElement.class, lastSyncDate );
        meta.categoryOptionCombos = getTypeUidsBefore( CategoryOptionCombo.class, lastSyncDate );
        meta.users = getTypeUidsBefore( User.class, lastSyncDate );
        meta.trackedEntityTypes = getTypeUidsBefore( TrackedEntityType.class, lastSyncDate );
        meta.trackedEntityAttributes = getTypeUidsBefore( TrackedEntityAttribute.class, lastSyncDate );
        meta.programs = getTypeUidsBefore( Program.class, lastSyncDate );
        meta.organisationUnits = getTypeUidsBefore( OrganisationUnit.class, lastSyncDate );
        meta.programStages = getTypeUidsBefore( ProgramStage.class, lastSyncDate );
        meta.relationshipTypes = getTypeUidsBefore( RelationshipType.class, lastSyncDate );
        meta.dataSets = getTypeUidsBefore( DataSet.class, lastSyncDate );

        return meta;
    }

    private List<SmsMetadata.Id> getTypeUidsBefore( Class<? extends IdentifiableObject> klass, Date lastSyncDate )
    {
        return identifiableObjectManager.getUidsCreatedBefore( klass, lastSyncDate ).stream()
            .map( o -> new SmsMetadata.Id( o ) ).collect( Collectors.toList() );
    }

    protected List<Object> saveNewEvent( String eventUid, OrganisationUnit orgUnit, ProgramStage programStage,
        Enrollment enrollment, IncomingSms sms, CategoryOptionCombo aoc, User user, List<SmsDataValue> values,
        SmsEventStatus eventStatus, Date eventDate, Date dueDate, GeoPoint coordinates )
    {
        ArrayList<Object> errorUids = new ArrayList<>();
        Event event;
        // If we aren't given a Uid for the event, it will be auto-generated

        if ( eventService.eventExists( eventUid ) )
        {
            event = eventService.getEvent( eventUid );
        }
        else
        {
            event = new Event();
            event.setUid( eventUid );
        }

        event.setOrganisationUnit( orgUnit );
        event.setProgramStage( programStage );
        event.setEnrollment( enrollment );
        event.setExecutionDate( eventDate );
        event.setDueDate( dueDate );
        event.setAttributeOptionCombo( aoc );
        event.setStoredBy( user.getUsername() );

        UserInfoSnapshot currentUserInfo = UserInfoSnapshot.from( user );

        event.setCreatedByUserInfo( currentUserInfo );
        event.setLastUpdatedByUserInfo( currentUserInfo );

        event.setStatus( getCoreEventStatus( eventStatus ) );
        event.setGeometry( convertGeoPointToGeometry( coordinates ) );

        if ( eventStatus.equals( SmsEventStatus.COMPLETED ) )
        {
            event.setCompletedBy( user.getUsername() );
            event.setCompletedDate( new Date() );
        }

        Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
        if ( values != null )
        {
            for ( SmsDataValue dv : values )
            {
                Uid deid = dv.getDataElement();
                String val = dv.getValue();

                DataElement de = dataElementService.getDataElement( deid.getUid() );

                // TODO: Is this the correct way of handling errors here?
                if ( de == null )
                {
                    log.warn( String
                        .format( "Given data element [%s] could not be found. Continuing with submission...", deid ) );
                    errorUids.add( deid );

                    continue;
                }
                else if ( val == null || StringUtils.isEmpty( val ) )
                {
                    log.warn( String
                        .format( "Value for atttribute [%s] is null or empty. Continuing with submission...", deid ) );
                    continue;
                }

                EventDataValue eventDataValue = new EventDataValue( deid.getUid(), dv.getValue(), currentUserInfo );
                eventDataValue.setAutoFields();
                dataElementsAndEventDataValues.put( de, eventDataValue );
            }
        }

        eventService.saveEventDataValuesAndSaveEvent( event,
            dataElementsAndEventDataValues );

        return errorUids;
    }

    private EventStatus getCoreEventStatus( SmsEventStatus eventStatus )
    {
        switch ( eventStatus )
        {
            case ACTIVE:
                return EventStatus.ACTIVE;
            case COMPLETED:
                return EventStatus.COMPLETED;
            case VISITED:
                return EventStatus.VISITED;
            case SCHEDULE:
                return EventStatus.SCHEDULE;
            case OVERDUE:
                return EventStatus.OVERDUE;
            case SKIPPED:
                return EventStatus.SKIPPED;
            default:
                return null;
        }
    }

    protected ProgramStatus getCoreProgramStatus( SmsEnrollmentStatus enrollmentStatus )
    {
        switch ( enrollmentStatus )
        {
            case ACTIVE:
                return ProgramStatus.ACTIVE;
            case COMPLETED:
                return ProgramStatus.COMPLETED;
            case CANCELLED:
                return ProgramStatus.CANCELLED;
            default:
                return null;
        }
    }

    protected Geometry convertGeoPointToGeometry( GeoPoint coordinates )
    {
        if ( coordinates == null )
        {
            return null;
        }

        GeometryFactory gf = new GeometryFactory();
        Coordinate co = new Coordinate(
            coordinates.getLongitude(), coordinates.getLatitude() );

        return gf.createPoint( co );
    }
}
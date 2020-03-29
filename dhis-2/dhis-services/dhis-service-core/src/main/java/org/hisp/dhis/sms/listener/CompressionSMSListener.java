package org.hisp.dhis.sms.listener;

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

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSConsts.SMSEnrollmentStatus;
import org.hisp.dhis.smscompression.SMSConsts.SMSEventStatus;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.SMSResponse;
import org.hisp.dhis.smscompression.SMSSubmissionReader;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSMetadata;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmissionHeader;
import org.hisp.dhis.smscompression.models.UID;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import static com.google.common.base.Preconditions.checkNotNull;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public abstract class CompressionSMSListener
    extends
    BaseSMSListener
{
    protected abstract SMSResponse postProcess( IncomingSms sms, SMSSubmission submission )
        throws SMSProcessingException;

    protected abstract boolean handlesType( SubmissionType type );

    protected final UserService userService;

    protected final TrackedEntityTypeService trackedEntityTypeService;

    protected final TrackedEntityAttributeService trackedEntityAttributeService;

    protected final ProgramService programService;

    protected final OrganisationUnitService organisationUnitService;

    protected final CategoryService categoryService;

    protected final DataElementService dataElementService;

    protected final ProgramStageInstanceService programStageInstanceService;

    protected final IdentifiableObjectManager identifiableObjectManager;

    public CompressionSMSListener( IncomingSmsService incomingSmsService, MessageSender smsSender,
        UserService userService, TrackedEntityTypeService trackedEntityTypeService,
        TrackedEntityAttributeService trackedEntityAttributeService, ProgramService programService,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        DataElementService dataElementService, ProgramStageInstanceService programStageInstanceService,
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
        checkNotNull( programStageInstanceService );

        this.userService = userService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.programService = programService;
        this.organisationUnitService = organisationUnitService;
        this.categoryService = categoryService;
        this.dataElementService = dataElementService;
        this.programStageInstanceService = programStageInstanceService;
        this.identifiableObjectManager = identifiableObjectManager;
    }

    @Override
    public boolean accept( IncomingSms sms )
    {
        if ( sms == null || !SmsUtils.isBase64( sms ) )
        {
            return false;
        }

        SMSSubmissionHeader header = getHeader( sms );
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
        SMSSubmissionReader reader = new SMSSubmissionReader();
        SMSSubmissionHeader header = getHeader( sms );
        if ( header == null )
        {
            // Error with the header, we have no message ID, use -1
            sendSMSResponse( SMSResponse.HEADER_ERROR, sms, -1 );
            return;
        }

        SMSMetadata meta = getMetadata( header.getLastSyncDate() );
        SMSSubmission subm = null;
        try
        {
            subm = reader.readSubmission( SmsUtils.getBytes( sms ), meta );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            sendSMSResponse( SMSResponse.READ_ERROR, sms, header.getSubmissionID() );
            return;
        }

        // TODO: Can be removed - debugging line to check SMS submissions
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        log.info( String.format( "New received SMS submission decoded as: %s", gson.toJson( subm ) ) );

        SMSResponse resp = null;
        try
        {
            checkUser( subm );
            resp = postProcess( sms, subm );
        }
        catch ( SMSProcessingException e )
        {
            log.error( e.getMessage() );
            sendSMSResponse( e.getResp(), sms, header.getSubmissionID() );
            return;
        }

        log.info( String.format( "SMS Response: ", resp.toString() ) );
        sendSMSResponse( resp, sms, header.getSubmissionID() );
    }

    private void checkUser( SMSSubmission subm )
    {
        UID userid = subm.getUserID();
        User user = userService.getUser( userid.uid );

        if ( user == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_USER.set( userid ) );
        }
    }

    private SMSSubmissionHeader getHeader( IncomingSms sms )
    {
        byte[] smsBytes = SmsUtils.getBytes( sms );
        SMSSubmissionReader reader = new SMSSubmissionReader();
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

    private SMSMetadata getMetadata( Date lastSyncDate )
    {
        SMSMetadata meta = new SMSMetadata();
        meta.dataElements = getTypeUidsBefore( DataElement.class, lastSyncDate );
        meta.categoryOptionCombos = getTypeUidsBefore( CategoryOptionCombo.class, lastSyncDate );
        meta.users = getTypeUidsBefore( User.class, lastSyncDate );
        meta.trackedEntityTypes = getTypeUidsBefore( TrackedEntityType.class, lastSyncDate );
        meta.trackedEntityAttributes = getTypeUidsBefore( TrackedEntityAttribute.class, lastSyncDate );
        meta.programs = getTypeUidsBefore( Program.class, lastSyncDate );
        meta.organisationUnits = getTypeUidsBefore( OrganisationUnit.class, lastSyncDate );

        return meta;
    }

    private List<SMSMetadata.ID> getTypeUidsBefore( Class<? extends IdentifiableObject> klass, Date lastSyncDate )
    {
        return identifiableObjectManager.getUidsCreatedBefore( klass, lastSyncDate ).stream()
            .map( o -> new SMSMetadata.ID( o ) ).collect( Collectors.toList() );
    }

    protected List<Object> saveNewEvent( String eventUid, OrganisationUnit orgUnit, ProgramStage programStage,
        ProgramInstance programInstance, IncomingSms sms, CategoryOptionCombo aoc, User user, List<SMSDataValue> values,
        SMSEventStatus eventStatus, Date eventDate, Date dueDate, GeoPoint coordinates )
    {
        ArrayList<Object> errorUIDs = new ArrayList<>();
        ProgramStageInstance programStageInstance;
        // If we aren't given a UID for the event, it will be auto-generated
        if ( programStageInstanceService.programStageInstanceExists( eventUid ) )
        {
            programStageInstance = programStageInstanceService.getProgramStageInstance( eventUid );
        }
        else
        {
            programStageInstance = new ProgramStageInstance();
            programStageInstance.setUid( eventUid );
        }

        programStageInstance.setOrganisationUnit( orgUnit );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setExecutionDate( eventDate );
        programStageInstance.setDueDate( dueDate );
        programStageInstance.setAttributeOptionCombo( aoc );
        programStageInstance.setStoredBy( user.getUsername() );
        programStageInstance.setStatus( getCoreEventStatus( eventStatus ) );
        programStageInstance.setGeometry( convertGeoPointToGeometry( coordinates ) );

        if ( eventStatus.equals( SMSEventStatus.COMPLETED ) )
        {
            programStageInstance.setCompletedBy( user.getUsername() );
            programStageInstance.setCompletedDate( new Date() );
        }

        Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
        if ( values != null )
        {
            for ( SMSDataValue dv : values )
            {
                UID deid = dv.getDataElement();
                String val = dv.getValue();

                DataElement de = dataElementService.getDataElement( deid.uid );
                // TODO: Is this the correct way of handling errors here?
                if ( de == null )
                {
                    log.warn( String
                        .format( "Given data element [%s] could not be found. Continuing with submission...", deid ) );
                    errorUIDs.add( deid );
                    continue;
                }
                else if ( val == null || StringUtils.isEmpty( val ) )
                {
                    log.warn( String
                        .format( "Value for atttribute [%s] is null or empty. Continuing with submission...", deid ) );
                    continue;
                }

                EventDataValue eventDataValue = new EventDataValue( deid.uid, dv.getValue(), user.getUsername() );
                eventDataValue.setAutoFields();
                dataElementsAndEventDataValues.put( de, eventDataValue );
            }
        }

        programStageInstanceService.saveEventDataValuesAndSaveProgramStageInstance( programStageInstance,
            dataElementsAndEventDataValues );

        return errorUIDs;
    }

    private EventStatus getCoreEventStatus( SMSEventStatus eventStatus )
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

    protected ProgramStatus getCoreProgramStatus( SMSEnrollmentStatus enrollmentStatus )
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
        com.vividsolutions.jts.geom.Coordinate co = new com.vividsolutions.jts.geom.Coordinate(
            coordinates.getLongitude(), coordinates.getLatitude() );

        return gf.createPoint( co );
    }
}
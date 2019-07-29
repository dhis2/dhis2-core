package org.hisp.dhis.sms.listener;

import static com.google.common.base.Preconditions.checkNotNull;

/*
* Copyright (c) 2004-2018, University of Oslo
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
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
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSConsts.SMSEventStatus;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.SMSResponse;
import org.hisp.dhis.smscompression.SMSSubmissionReader;
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
import org.jfree.util.Log;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Transactional
public abstract class NewSMSListener
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

    public NewSMSListener( IncomingSmsService incomingSmsService, MessageSender smsSender, UserService userService,
        TrackedEntityTypeService trackedEntityTypeService, TrackedEntityAttributeService trackedEntityAttributeService,
        ProgramService programService, OrganisationUnitService organisationUnitService, CategoryService categoryService,
        DataElementService dataElementService, ProgramStageInstanceService programStageInstanceService )
    {
        super( incomingSmsService, smsSender );

        checkNotNull( userService );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( programService );
        checkNotNull( organisationUnitService );
        checkNotNull( categoryService );
        checkNotNull( dataElementService );

        this.userService = userService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.programService = programService;
        this.organisationUnitService = organisationUnitService;
        this.categoryService = categoryService;
        this.dataElementService = dataElementService;
        this.programStageInstanceService = programStageInstanceService;
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
            Log.error( e.getMessage() );
            sendSMSResponse( SMSResponse.READ_ERROR, sms, header.getSubmissionID() );
            return;
        }

        // TODO: Can be removed - debugging line to check SMS submissions
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Log.info( "New received SMS submission decoded as: " + gson.toJson( subm ) );

        SMSResponse resp = null;
        try
        {
            checkUser( subm );
            resp = postProcess( sms, subm );
        }
        catch ( SMSProcessingException e )
        {
            Log.error( e.getMessage() );
            sendSMSResponse( e.getResp(), sms, header.getSubmissionID() );
            return;
        }

        Log.info( "SMS Response: " + resp.toString() );
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
            Log.error( e.getMessage() );
            return null;
        }
    }

    private SMSMetadata getMetadata( Date lastSyncDate )
    {
        SMSMetadata meta = new SMSMetadata();
        meta.dataElements = getAllDataElements( lastSyncDate );
        meta.categoryOptionCombos = getAllCatOptionCombos( lastSyncDate );
        meta.users = getAllUserIds( lastSyncDate );
        meta.trackedEntityTypes = getAllTrackedEntityTypeIds( lastSyncDate );
        meta.trackedEntityAttributes = getAllTrackedEntityAttributeIds( lastSyncDate );
        meta.programs = getAllProgramIds( lastSyncDate );
        meta.organisationUnits = getAllOrgUnitIds( lastSyncDate );

        return meta;
    }

    private List<SMSMetadata.ID> getAllUserIds( Date lastSyncDate )
    {
        List<User> users = userService.getAllUsers();

        return users.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllTrackedEntityTypeIds( Date lastSyncDate )
    {
        List<TrackedEntityType> teTypes = trackedEntityTypeService.getAllTrackedEntityType();

        return teTypes.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllTrackedEntityAttributeIds( Date lastSyncDate )
    {
        List<TrackedEntityAttribute> teiAttributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();

        return teiAttributes.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllProgramIds( Date lastSyncDate )
    {
        List<Program> programs = programService.getAllPrograms();

        return programs.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllOrgUnitIds( Date lastSyncDate )
    {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();

        return orgUnits.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllDataElements( Date lastSyncDate )
    {
        List<DataElement> dataElements = dataElementService.getAllDataElements();

        return dataElements.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private List<SMSMetadata.ID> getAllCatOptionCombos( Date lastSyncDate )
    {
        List<CategoryOptionCombo> catOptionCombos = categoryService.getAllCategoryOptionCombos();

        return catOptionCombos.stream().map( o -> getIdFromMetadata( o, lastSyncDate ) ).filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private SMSMetadata.ID getIdFromMetadata( IdentifiableObject obj, Date lastSyncDate )
    {
        if ( obj.getCreated().after( lastSyncDate ) )
        {
            return null;
        }
        else
        {
            SMSMetadata.ID id = new SMSMetadata.ID( obj.getUid() );
            return id;
        }
    }

    protected List<Object> saveNewEvent( String eventUid, OrganisationUnit orgUnit, ProgramStage programStage,
        ProgramInstance programInstance, IncomingSms sms, CategoryOptionCombo aoc, User user, List<SMSDataValue> values,
        SMSEventStatus eventStatus )
    {

        ArrayList<Object> errorUIDs = new ArrayList<>();
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        // If we aren't given a UID for the event, it will be auto-generated
        if ( eventUid != null )
            programStageInstance.setUid( eventUid );
        programStageInstance.setOrganisationUnit( orgUnit );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setExecutionDate( sms.getSentDate() );
        programStageInstance.setDueDate( sms.getSentDate() );
        programStageInstance.setAttributeOptionCombo( aoc );
        programStageInstance.setStoredBy( user.getUsername() );
        programStageInstance.setStatus( getCoreEventStatus( eventStatus ) );

        if ( eventStatus.equals( SMSEventStatus.COMPLETED ) )
        {
            programStageInstance.setCompletedBy( user.getUsername() );
            programStageInstance.setCompletedDate( new Date() );
        }

        Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
        for ( SMSDataValue dv : values )
        {
            UID deid = dv.getDataElement();
            String val = dv.getValue();

            DataElement de = dataElementService.getDataElement( deid.uid );
            // TODO: Is this the correct way of handling errors here?
            if ( de == null )
            {
                Log.warn( "Given data element [" + deid + "] could not be found. Continuing with submission..." );
                errorUIDs.add( deid );
                continue;
            }
            else if ( val == null || StringUtils.isEmpty( val ) )
            {
                Log.warn( "Value for atttribute [" + deid + "] is null or empty. Continuing with submission..." );
                continue;
            }

            EventDataValue eventDataValue = new EventDataValue( deid.uid, dv.getValue(), user.getUsername() );
            eventDataValue.setAutoFields();
            dataElementsAndEventDataValues.put( de, eventDataValue );
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
}
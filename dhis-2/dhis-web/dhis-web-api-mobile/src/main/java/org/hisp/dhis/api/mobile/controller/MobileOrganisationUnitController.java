package org.hisp.dhis.api.mobile.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.api.mobile.ActivityReportingService;
import org.hisp.dhis.api.mobile.FacilityReportingService;
import org.hisp.dhis.api.mobile.IProgramService;
import org.hisp.dhis.api.mobile.NotAllowedException;
import org.hisp.dhis.api.mobile.model.ActivityValue;
import org.hisp.dhis.api.mobile.model.Contact;
import org.hisp.dhis.api.mobile.model.Conversation;
import org.hisp.dhis.api.mobile.model.DataSetList;
import org.hisp.dhis.api.mobile.model.DataSetValue;
import org.hisp.dhis.api.mobile.model.DataSetValueList;
import org.hisp.dhis.api.mobile.model.DataStreamSerializable;
import org.hisp.dhis.api.mobile.model.Interpretation;
import org.hisp.dhis.api.mobile.model.InterpretationComment;
import org.hisp.dhis.api.mobile.model.LWUITmodel.LostEvent;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Notification;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Patient;
import org.hisp.dhis.api.mobile.model.LWUITmodel.PatientIdentifierAndAttribute;
import org.hisp.dhis.api.mobile.model.LWUITmodel.PatientList;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Program;
import org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance;
import org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship;
import org.hisp.dhis.api.mobile.model.Message;
import org.hisp.dhis.api.mobile.model.MobileModel;
import org.hisp.dhis.api.mobile.model.ModelList;
import org.hisp.dhis.api.mobile.model.Recipient;
import org.hisp.dhis.api.mobile.model.SMSCode;
import org.hisp.dhis.api.mobile.model.SMSCommand;
import org.hisp.dhis.i18n.I18nLocaleService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping( value = "/mobile" )
public class MobileOrganisationUnitController
    extends AbstractMobileController
{
    private static final String ACTIVITY_REPORT_UPLOADED = "activity_report_uploaded";

    private static final String DATASET_REPORT_UPLOADED = "dataset_report_uploaded";

    @Autowired
    private ActivityReportingService activityReportingService;

    @Autowired
    private IProgramService programService;

    @Autowired
    private FacilityReportingService facilityReportingService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private I18nLocaleService localeService;

    @Autowired
    private SMSCommandService smsCommandService;

    private Integer patientId;

    public Integer getPatientId()
    {
        return patientId;
    }

    public void setPatientId( Integer patientId )
    {
        this.patientId = patientId;
    }

    // For client version 2.8 and lower
    @RequestMapping( method = RequestMethod.GET, value = "orgUnits/{id}/all" )
    @ResponseBody
    public MobileModel getAllDataForOrgUnit2_8( @PathVariable int id, @RequestHeader( "accept-language" ) String locale )
    {
        MobileModel mobileModel = new MobileModel();
        mobileModel.setClientVersion( DataStreamSerializable.TWO_POINT_EIGHT );
        OrganisationUnit unit = getUnit( id );
        mobileModel.setActivityPlan( activityReportingService.getCurrentActivityPlan( unit, locale ) );
        mobileModel.setPrograms( programService.getPrograms( unit, locale ) );
        mobileModel.setDatasets( facilityReportingService.getMobileDataSetsForUnit( unit, locale ) );
        mobileModel.setServerCurrentDate( new Date() );
        mobileModel.setLocales( getLocalStrings( localeService.getAllLocales() ) );
        return mobileModel;
    }

    @RequestMapping( method = RequestMethod.POST, value = "orgUnits/{id}/updateDataSets" )
    @ResponseBody
    public DataSetList checkUpdatedDataSet2_8( @PathVariable int id, @RequestBody DataSetList dataSetList,
        @RequestHeader( "accept-language" ) String locale )
    {
        DataSetList returnList = facilityReportingService.getUpdatedDataSet( dataSetList, getUnit( id ), locale );
        returnList.setClientVersion( DataStreamSerializable.TWO_POINT_EIGHT );
        return returnList;
    }

    /**
     * Save a facility report for unit
     * 
     * @param dataSetValue - the report to save
     * @throws NotAllowedException if the {@link DataSetValue} is invalid
     */
    @RequestMapping( method = RequestMethod.POST, value = "orgUnits/{id}/dataSets" )
    @ResponseBody
    public String saveDataSetValues2_8( @PathVariable int id, @RequestBody DataSetValue dataSetValue )
        throws NotAllowedException
    {
        facilityReportingService.saveDataSetValues( getUnit( id ), dataSetValue );
        return DATASET_REPORT_UPLOADED;
    }

    /**
     * Save activity report for unit
     * 
     * @param activityValue - the report to save
     * @throws NotAllowedException if the {@link ActivityValue activity value}
     *         is invalid
     */
    @RequestMapping( method = RequestMethod.POST, value = "orgUnits/{id}/activities" )
    @ResponseBody
    public String saveActivityReport2_8( @PathVariable int id, @RequestBody ActivityValue activityValue )
        throws NotAllowedException
    {
        // FIXME set the last argument to 0 to fix compilation error
        activityReportingService.saveActivityReport( getUnit( id ), activityValue, 0 );
        return ACTIVITY_REPORT_UPLOADED;
    }

    @RequestMapping( method = RequestMethod.POST, value = "orgUnits/{id}/activitiyplan" )
    @ResponseBody
    public MobileModel updatePrograms2_8( @PathVariable int id, @RequestHeader( "accept-language" ) String locale,
        @RequestBody ModelList programsFromClient )
    {
        MobileModel model = new MobileModel();
        model.setClientVersion( DataStreamSerializable.TWO_POINT_EIGHT );
        model.setPrograms( programService.updateProgram( programsFromClient, locale, getUnit( id ) ) );
        model.setActivityPlan( activityReportingService.getCurrentActivityPlan( getUnit( id ), locale ) );
        model.setServerCurrentDate( new Date() );
        return model;
    }

    @RequestMapping( method = RequestMethod.GET, value = "orgUnits/{id}/changeLanguageDataSet" )
    @ResponseBody
    public DataSetList changeLanguageDataSet2_8( @PathVariable int id, @RequestHeader( "accept-language" ) String locale )
    {
        return facilityReportingService.getDataSetsForLocale( getUnit( id ), locale );
    }

    // For client version 2.9 and higher

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/all" )
    @ResponseBody
    public MobileModel getAllDataForOrgUnit( @PathVariable String clientVersion, @PathVariable int id,
        @RequestHeader( "accept-language" ) String locale )
    {
        MobileModel mobileModel = new MobileModel();
        mobileModel.setClientVersion( clientVersion );
        OrganisationUnit unit = getUnit( id );
        // mobileModel.setActivityPlan(
        // activityReportingService.getCurrentActivityPlan( unit, locale ) );
        // mobileModel.setPrograms( programService.getPrograms( unit, locale )
        // );
        mobileModel.setDatasets( facilityReportingService.getMobileDataSetsForUnit( unit, locale ) );
        mobileModel.setServerCurrentDate( new Date() );
        mobileModel.setLocales( getLocalStrings( localeService.getAllLocales() ) );
        mobileModel.setSmsCommands( this.getMobileSMSCommands( smsCommandService.getJ2MESMSCommands() ) );
        return mobileModel;
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/updateDataSets" )
    @ResponseBody
    public DataSetList checkUpdatedDataSet( @PathVariable String clientVersion, @PathVariable int id,
        @RequestBody DataSetList dataSetList, @RequestHeader( "accept-language" ) String locale )
    {
        DataSetList returnList = facilityReportingService.getUpdatedDataSet( dataSetList, getUnit( id ), locale );
        returnList.setClientVersion( clientVersion );
        return returnList;
    }

    /**
     * Save a facility report for unit
     * 
     * @param dataSetValue - the report to save
     * @throws NotAllowedException if the {@link DataSetValue} is invalid
     */

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/dataSets" )
    @ResponseBody
    public String saveDataSetValues( @PathVariable int id, @RequestBody DataSetValue dataSetValue )
        throws NotAllowedException
    {
        facilityReportingService.saveDataSetValues( getUnit( id ), dataSetValue );
        return DATASET_REPORT_UPLOADED;
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/dataSetValue" )
    @ResponseBody
    public DataSetValueList getDataSetValues( @PathVariable int id, @RequestBody DataSetList dataSetList )
        throws NotAllowedException
    {
        return facilityReportingService.getDataSetValues( getUnit( id ), dataSetList );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/activitiyplan" )
    @ResponseBody
    public MobileModel updatePrograms( @PathVariable String clientVersion, @PathVariable int id,
        @RequestHeader( "accept-language" ) String locale, @RequestBody ModelList programsFromClient )
    {
        MobileModel model = new MobileModel();
        model.setClientVersion( clientVersion );
        model.setPrograms( programService.updateProgram( programsFromClient, locale, getUnit( id ) ) );
        model.setActivityPlan( activityReportingService.getCurrentActivityPlan( getUnit( id ), locale ) );
        model.setServerCurrentDate( new Date() );
        return model;
    }

    /**
     * Save a facility report for unit
     * 
     * @param dataSetValue - the report to save
     * @throws NotAllowedException if the {@link DataSetValue} is invalid
     */

    // @RequestMapping( method = RequestMethod.POST, value =
    // "{clientVersion}/orgUnits/{id}/dataSets" )
    // @ResponseBody
    // public String saveDataSetValues( @PathVariable int id, @RequestBody
    // DataSetValue dataSetValue )
    // throws NotAllowedException
    // {
    // facilityReportingService.saveDataSetValues( getUnit( id ), dataSetValue
    // );
    // return DATASET_REPORT_UPLOADED;
    // }

    /**
     * Save activity report for unit
     * 
     * @param activityValue - the report to save
     * @throws NotAllowedException if the {@link ActivityValue activity value}
     *         is invalid
     */
    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/activities" )
    @ResponseBody
    public String saveActivityReport( @PathVariable int id, @RequestBody ActivityValue activityValue )
        throws NotAllowedException
    {
        // FIXME set the last argument to 0 to fix compilation error
        activityReportingService.saveActivityReport( getUnit( id ), activityValue, 0 );
        return ACTIVITY_REPORT_UPLOADED;
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/changeLanguageDataSet" )
    @ResponseBody
    public DataSetList changeLanguageDataSet( @PathVariable int id, @RequestHeader( "accept-language" ) String locale )
    {
        return facilityReportingService.getDataSetsForLocale( getUnit( id ), locale );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/updateContactForMobile" )
    @ResponseBody
    public Contact updateContactForMobile()
    {
        return facilityReportingService.updateContactForMobile();
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/all" )
    @ResponseBody
    public org.hisp.dhis.api.mobile.model.LWUITmodel.MobileModel getAllDataForOrgUnitLWUIT(
        @PathVariable String clientVersion, @PathVariable int id )
    {
        org.hisp.dhis.api.mobile.model.LWUITmodel.MobileModel mobileModel = new org.hisp.dhis.api.mobile.model.LWUITmodel.MobileModel();
        mobileModel.setClientVersion( clientVersion );
        OrganisationUnit unit = getUnit( id );
        mobileModel.setPrograms( programService.getProgramsLWUIT( unit ) );
        mobileModel.setServerCurrentDate( new Date() );
        mobileModel.setRelationshipTypes( programService.getAllRelationshipTypes() );
        
        return mobileModel;
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/updateContactForMobile" )
    @ResponseBody
    public Contact updateContactForMobileLWUIT()
    {
        return facilityReportingService.updateContactForMobile();
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findPatient" )
    @ResponseBody
    public Patient findPatientByName( @PathVariable int id, @RequestHeader( "patientId" ) String patientId )
        throws NotAllowedException
    {
        return activityReportingService.findPatient( patientId );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findPatients" )
    @ResponseBody
    public PatientList findPatientsById( @PathVariable int id, @RequestHeader( "patientIds" ) String patientIds )
        throws NotAllowedException
    {
        return activityReportingService.findPatients( patientIds );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findPatientInAdvanced/{programId}" )
    @ResponseBody
    public String findPatientInAdvanced( @PathVariable int programId, @PathVariable int id,
        @RequestHeader( "name" ) String keyword )
        throws NotAllowedException
    {
        return activityReportingService.findPatientInAdvanced( keyword, id, programId );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/uploadProgramStage/{patientId}" )
    @ResponseBody
    public String saveProgramStage( @PathVariable int patientId, @PathVariable int id,
        @RequestBody ProgramStage programStage )
        throws NotAllowedException
    {
        return activityReportingService.saveProgramStage( programStage, patientId, id );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/completeProgramInstance" )
    @ResponseBody
    public String completeProgramInstance( @PathVariable int id, @RequestBody ProgramInstance programInstance )
        throws NotAllowedException
    {
        return activityReportingService.completeProgramInstance( programInstance.getId() );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/uploadSingleEventWithoutRegistration" )
    @ResponseBody
    public String saveSingleEventWithoutRegistration( @PathVariable int id, @RequestBody ProgramStage programStage )
        throws NotAllowedException
    {
        return activityReportingService.saveSingleEventWithoutRegistration( programStage, id );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/enrollProgram" )
    @ResponseBody
    public Patient enrollProgram( @PathVariable int id, @RequestHeader( "enrollInfo" ) String enrollInfo )
        throws NotAllowedException
    {
        return activityReportingService.enrollProgram( enrollInfo, null, new Date() );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/addRelationship" )
    @ResponseBody
    public Patient addRelationship( @PathVariable int id, @RequestBody Relationship enrollmentRelationship )
        throws NotAllowedException
    {
        return activityReportingService.addRelationship( enrollmentRelationship, id );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/downloadAnonymousProgram" )
    @ResponseBody
    public Program getAnonymousProgram( @PathVariable int id, @RequestHeader( "programType" ) String programType )
        throws NotAllowedException
    {
        return activityReportingService.getAllProgramByOrgUnit( id, programType );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findProgram" )
    @ResponseBody
    public Program findProgram( @PathVariable int id, @RequestHeader( "info" ) String programInfo )
        throws NotAllowedException
    {
        return activityReportingService.findProgram( programInfo );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findLostToFollowUp" )
    @ResponseBody
    public String findLostToFollowUp( @PathVariable int id, @RequestHeader( "searchEventInfos" ) String searchEventInfos )
        throws NotAllowedException
    {
        return activityReportingService.findLostToFollowUp( id, searchEventInfos );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/handleLostToFollowUp" )
    @ResponseBody
    public Notification handleLostToFollowUp( @PathVariable int id, @RequestBody LostEvent lostEvent )
        throws NotAllowedException
    {
        return activityReportingService.handleLostToFollowUp( lostEvent );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/generateRepeatableEvent" )
    @ResponseBody
    public Patient generateRepeatableEvent( @PathVariable int id, @RequestHeader( "eventInfo" ) String eventInfo )
        throws NotAllowedException
    {
        return activityReportingService.generateRepeatableEvent( id, eventInfo );
    }

    // Supportive methods

    private Collection<String> getLocalStrings( Collection<Locale> locales )
    {
        if ( locales == null || locales.isEmpty() )
        {
            return null;
        }
        Collection<String> localeStrings = new ArrayList<>();

        for ( Locale locale : locales )
        {
            localeStrings.add( locale.getLanguage() + "-" + locale.getCountry() );
        }
        return localeStrings;
    }

    private List<SMSCommand> getMobileSMSCommands( Collection<org.hisp.dhis.sms.command.SMSCommand> normalSMSCommands )
    {
        List<SMSCommand> smsCommands = new ArrayList<>();
        for ( org.hisp.dhis.sms.command.SMSCommand normalSMSCommand : normalSMSCommands )
        {
            SMSCommand mobileSMSCommand = new SMSCommand();
            List<SMSCode> smsCodes = new ArrayList<>();

            mobileSMSCommand.setName( normalSMSCommand.getName() );
            mobileSMSCommand.setCodeSeparator( normalSMSCommand.getCodeSeparator() );
            mobileSMSCommand.setDataSetId( normalSMSCommand.getDataset().getId() );
            mobileSMSCommand.setSeparator( normalSMSCommand.getSeparator() );

            for ( org.hisp.dhis.sms.command.code.SMSCode normalSMSCode : normalSMSCommand.getCodes() )
            {
                SMSCode smsCode = new SMSCode();

                smsCode.setCode( normalSMSCode.getCode() );
                smsCode.setDataElementId( normalSMSCode.getDataElement().getId() );
                smsCode.setOptionId( normalSMSCode.getOptionId() );
                smsCodes.add( smsCode );
            }
            mobileSMSCommand.setSmsCodes( smsCodes );
            smsCommands.add( mobileSMSCommand );
        }
        return smsCommands;
    }

    private OrganisationUnit getUnit( int id )
    {
        return organisationUnitService.getOrganisationUnit( id );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/registerPerson" )
    @ResponseBody
    public Patient savePatient( @PathVariable int id, @RequestBody Patient patient,
        @RequestHeader( "programid" ) String programId )
        throws NotAllowedException
    {

        if ( patient.getId() == 0 )
        {
            return activityReportingService.savePatient( patient, id, programId );
        }
        else
        {
            return activityReportingService.updatePatient( patient, id, programId );
        }
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/getVariesInfo" )
    @ResponseBody
    public PatientIdentifierAndAttribute getVariesInfo( @PathVariable String clientVersion, @PathVariable int id,
        @RequestHeader( "accept-language" ) String locale, @RequestHeader( "programid" ) String programId )
    {
        PatientIdentifierAndAttribute patientIdentifierAndAttribute = new PatientIdentifierAndAttribute();
        patientIdentifierAndAttribute.setClientVersion( clientVersion );
        patientIdentifierAndAttribute.setPatientAttributes( activityReportingService
            .getPatientAttributesForMobile( programId ) );

        return patientIdentifierAndAttribute;
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/sendFeedback" )
    @ResponseBody
    public String sendFeedback( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.sendFeedback( message );

    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/findUser" )
    @ResponseBody
    public Recipient findUser( String clientVersion, @PathVariable int id, @RequestHeader( "name" ) String keyword )
        throws NotAllowedException
    {
        Recipient recipient = new Recipient();
        recipient.setUsers( activityReportingService.findUser( keyword ) );
        return recipient;
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findVisitSchedule/{programId}" )
    @ResponseBody
    public String findVisitSchedule( @PathVariable int programId, @PathVariable int id,
        @RequestHeader( "details" ) String info )
        throws NotAllowedException
    {
        return activityReportingService.findVisitSchedule( id, programId, info );
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/sendMessage" )
    @ResponseBody
    public String sendMessage( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.sendMessage( message );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/downloadMessageConversation" )
    @ResponseBody
    public Conversation downloadConversation( String clientVersion )
        throws NotAllowedException
    {

        Conversation conversation = new Conversation();
        conversation.setClientVersion( clientVersion );
        conversation.setConversations( activityReportingService.downloadMessageConversation() );

        return conversation;

    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/getMessage" )
    @ResponseBody
    public Conversation getMessage( String clientVersion, @PathVariable int id,
        @RequestHeader( "id" ) String conversationId )
        throws NotAllowedException
    {

        Conversation conversation = new Conversation();
        conversation.setMessages( activityReportingService.getMessage( conversationId ) );

        return conversation;

    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/orgUnits/{id}/replyMessage" )
    @ResponseBody
    public String replyMessage( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.replyMessage( message );

    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/sendFeedback" )
    @ResponseBody
    public String sendFeedbackTracker( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.sendFeedback( message );

    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/findUser" )
    @ResponseBody
    public Recipient findUserTracker( String clientVersion, @PathVariable int id,
        @RequestHeader( "name" ) String keyword )
        throws NotAllowedException
    {
        Recipient recipient = new Recipient();
        recipient.setUsers( activityReportingService.findUser( keyword ) );
        return recipient;
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/sendMessage" )
    @ResponseBody
    public String sendMessageTracker( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.sendMessage( message );
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/downloadMessageConversation" )
    @ResponseBody
    public Conversation downloadConversationTracker( String clientVersion )
        throws NotAllowedException
    {

        Conversation conversation = new Conversation();
        conversation.setClientVersion( clientVersion );
        conversation.setConversations( activityReportingService.downloadMessageConversation() );

        return conversation;

    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/LWUIT/orgUnits/{id}/getMessage" )
    @ResponseBody
    public Conversation getMessageTracker( String clientVersion, @PathVariable int id,
        @RequestHeader( "id" ) String conversationId )
        throws NotAllowedException
    {

        Conversation conversation = new Conversation();
        conversation.setMessages( activityReportingService.getMessage( conversationId ) );

        return conversation;

    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/replyMessage" )
    @ResponseBody
    public String replyMessageTracker( @PathVariable int id, @RequestBody Message message )
        throws NotAllowedException
    {
        return activityReportingService.replyMessage( message );

    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/downloadInterpretation" )
    @ResponseBody
    public Interpretation downloadInterpretation( String clientVersion, @PathVariable int id,
        @RequestHeader( "uId" ) String uId )
        throws NotAllowedException
    {
        Interpretation interpretation = activityReportingService.getInterpretation( uId );
        return interpretation;
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/postInterpretation" )
    @ResponseBody
    public Interpretation postInterpretation( String clientVersion, @PathVariable int id,
        @RequestHeader( "data" ) String data )
        throws NotAllowedException
    {
        Interpretation interpretation = new Interpretation();
        interpretation.setText( activityReportingService.postInterpretation( data ) );
        return interpretation;
    }

    @RequestMapping( method = RequestMethod.GET, value = "{clientVersion}/orgUnits/{id}/postComment" )
    @ResponseBody
    public InterpretationComment postInterpretationComment( String clientVersion, @PathVariable int id,
        @RequestHeader( "data" ) String data )
        throws NotAllowedException
    {
        InterpretationComment message = new InterpretationComment();
        message.setText( activityReportingService.postInterpretationComment( data ) );
        return message;
    }

    @RequestMapping( method = RequestMethod.POST, value = "{clientVersion}/LWUIT/orgUnits/{id}/registerRelative" )
    @ResponseBody
    public Patient registerRelative( @PathVariable int id, @RequestBody Patient patient,
        @RequestHeader( "programid" ) String programId )
        throws NotAllowedException
    {
        return activityReportingService.registerRelative( patient, id, programId );
    }

}

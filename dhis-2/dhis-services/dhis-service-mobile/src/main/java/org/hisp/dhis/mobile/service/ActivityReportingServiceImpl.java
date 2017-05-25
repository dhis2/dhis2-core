package org.hisp.dhis.mobile.service;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.api.mobile.NotAllowedException;
import org.hisp.dhis.api.mobile.model.Activity;
import org.hisp.dhis.api.mobile.model.ActivityPlan;
import org.hisp.dhis.api.mobile.model.ActivityValue;
import org.hisp.dhis.api.mobile.model.Beneficiary;
import org.hisp.dhis.api.mobile.model.DataValue;
import org.hisp.dhis.api.mobile.model.Interpretation;
import org.hisp.dhis.api.mobile.model.InterpretationComment;
import org.hisp.dhis.api.mobile.model.LWUITmodel.LostEvent;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Notification;
import org.hisp.dhis.api.mobile.model.LWUITmodel.Patient;
import org.hisp.dhis.api.mobile.model.LWUITmodel.PatientList;
import org.hisp.dhis.api.mobile.model.OptionSet;
import org.hisp.dhis.api.mobile.model.PatientAttribute;
import org.hisp.dhis.api.mobile.model.Task;
import org.hisp.dhis.api.mobile.model.comparator.ActivityComparator;
import org.hisp.dhis.api.mobile.model.comparator.TrackedEntityAttributeValueSortOrderComparator;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.*;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.comparator.ProgramStageInstanceVisitDateComparator;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityReportingServiceImpl
    implements ActivityReportingService
{
    private static final String PROGRAM_STAGE_UPLOADED = "program_stage_uploaded";

    private static final String PROGRAM_STAGE_SECTION_UPLOADED = "program_stage_section_uploaded";

    private static final String SINGLE_EVENT_UPLOADED = "single_event_uploaded";

    private static final String SINGLE_EVENT_WITHOUT_REGISTRATION_UPLOADED = "single_event_without_registration_uploaded";

    private static final String PROGRAM_COMPLETED = "program_completed";

    private static final String FEEDBACK_SENT = "feedback_sent";

    private static final String MESSAGE_SENT = "message_sent";

    private static final String INTERPRETATION_SENT = "interpretation_sent";

    private static final String COMMENT_SENT = "comment_sent";

    private ActivityComparator activityComparator = new ActivityComparator();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStageInstanceService programStageInstanceService;

    private TrackedEntityInstanceService entityInstanceService;

    private TrackedEntityAttributeValueService attValueService;

    private TrackedEntityDataValueService dataValueService;

    private ProgramStageSectionService programStageSectionService;

    private ProgramInstanceService programInstanceService;

    private RelationshipService relationshipService;

    private RelationshipTypeService relationshipTypeService;

    private DataElementService dataElementService;

    private ProgramService programService;

    private ProgramStageService programStageService;

    private CurrentUserService currentUserService;

    private MessageService messageService;

    private MessageSender smsSender;

    private TrackedEntityAttributeService attributeService;

    private TrackedEntityService trackedEntityService;

    private I18nManager i18nManager;

    private UserService userService;

    private InterpretationService interpretationService;

    private ChartService chartService;

    private Integer patientId;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    @Required
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Required
    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    public void setSmsSender( MessageSender smsSender )
    {
        this.smsSender = smsSender;
    }

    public void setAttributeService( TrackedEntityAttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    public void setProgramStageInstanceService( ProgramStageInstanceService programStageInstanceService )
    {
        this.programStageInstanceService = programStageInstanceService;
    }

    @Required
    public void setAttValueService( TrackedEntityAttributeValueService attValueService )
    {
        this.attValueService = attValueService;
    }

    public void setGroupByAttribute( TrackedEntityAttribute groupByAttribute )
    {
        this.groupByAttribute = groupByAttribute;
    }

    @Required
    public void setEntityInstanceService( TrackedEntityInstanceService entityInstanceService )
    {
        this.entityInstanceService = entityInstanceService;
    }

    @Required
    public void setProgramInstanceService( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

    @Required
    public void setRelationshipService( RelationshipService relationshipService )
    {
        this.relationshipService = relationshipService;
    }

    @Required
    public void setProgramStageSectionService( ProgramStageSectionService programStageSectionService )
    {
        this.programStageSectionService = programStageSectionService;
    }

    @Required
    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    @Required
    public void setDataValueService( TrackedEntityDataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    @Required
    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    @Required
    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    // -------------------------------------------------------------------------
    // MobileDataSetService
    // -------------------------------------------------------------------------
    @Required
    public void setInterpretationService( InterpretationService interpretationService )
    {
        this.interpretationService = interpretationService;
    }

    public void setChartService( ChartService chartService )
    {
        this.chartService = chartService;
    }

    private TrackedEntityAttribute groupByAttribute;

    @Override
    public ActivityPlan getCurrentActivityPlan( OrganisationUnit unit, String localeString )
    {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.DATE, 30 );

        long upperBound = cal.getTime().getTime();

        cal.add( Calendar.DATE, -60 );
        long lowerBound = cal.getTime().getTime();

        List<Activity> items = new ArrayList<>();

        TrackedEntityInstanceQueryParams param = new TrackedEntityInstanceQueryParams()
            .addOrganisationUnit( unit );

        Grid trackedEntityDrid = entityInstanceService.getTrackedEntityInstancesGrid( param );
        List<List<Object>> entityInstanceList = trackedEntityDrid.getRows();

        for ( List<Object> entityInstance : entityInstanceList )
        {
            TrackedEntityInstance trackedEntityInstance = entityInstanceService
                .getTrackedEntityInstance( entityInstance.get( 0 ).toString() );
            for ( ProgramStageInstance programStageInstance : programStageInstanceService
                .getProgramStageInstances( trackedEntityInstance, EventStatus.ACTIVE ) )
            {
                if ( programStageInstance.getDueDate().getTime() >= lowerBound
                    && programStageInstance.getDueDate().getTime() <= upperBound )
                {
                    items.add( getActivity( programStageInstance, false ) );
                }
            }
        }

        if ( items.isEmpty() )
        {
            return null;
        }

        Collections.sort( items, activityComparator );

        return new ActivityPlan( items );
    }

    @Override
    public ActivityPlan getAllActivityPlan( OrganisationUnit unit, String localeString )
    {

        List<Activity> items = new ArrayList<>();

        TrackedEntityInstanceQueryParams param = new TrackedEntityInstanceQueryParams();
        param.addOrganisationUnit( unit );

        Grid trackedEntityDrid = entityInstanceService.getTrackedEntityInstancesGrid( param );
        List<List<Object>> entityInstanceList = trackedEntityDrid.getRows();

        for ( List<Object> entityInstance : entityInstanceList )
        {
            TrackedEntityInstance trackedEntityInstance = entityInstanceService
                .getTrackedEntityInstance( entityInstance.get( 0 ).toString() );
            for ( ProgramStageInstance programStageInstance : programStageInstanceService
                .getProgramStageInstances( trackedEntityInstance, EventStatus.ACTIVE ) )
            {

                items.add( getActivity( programStageInstance, false ) );
            }

        }

        if ( items.isEmpty() )
        {
            return null;
        }

        Collections.sort( items, activityComparator );
        return new ActivityPlan( items );
    }

    // -------------------------------------------------------------------------
    // DataValueService
    // -------------------------------------------------------------------------

    @Override
    public void saveActivityReport( OrganisationUnit unit, ActivityValue activityValue, Integer programStageSectionId )
        throws NotAllowedException
    {

        ProgramStageInstance programStageInstance = programStageInstanceService
            .getProgramStageInstance( activityValue.getProgramInstanceId() );
        if ( programStageInstance == null )
        {
            throw NotAllowedException.INVALID_PROGRAM_STAGE;
        }

        programStageInstance.getProgramStage();
        List<org.hisp.dhis.dataelement.DataElement> dataElements = new ArrayList<>();

        ProgramStageSection programStageSection = programStageSectionService
            .getProgramStageSection( programStageSectionId );

        if ( programStageSectionId != 0 )
        {
            dataElements.addAll( programStageSection.getDataElements() );
        }
        else
        {
            for ( ProgramStageDataElement de : programStageInstance.getProgramStage().getProgramStageDataElements() )
            {
                dataElements.add( de.getDataElement() );
            }
        }

        programStageInstance.getProgramStage().getProgramStageDataElements();
        Collection<Integer> dataElementIds = new ArrayList<>( activityValue.getDataValues().size() );

        for ( DataValue dv : activityValue.getDataValues() )
        {
            dataElementIds.add( dv.getId() );
        }

        if ( dataElements.size() != dataElementIds.size() )
        {
            throw NotAllowedException.INVALID_PROGRAM_STAGE;
        }

        Map<Integer, org.hisp.dhis.dataelement.DataElement> dataElementMap = new HashMap<>();
        for ( org.hisp.dhis.dataelement.DataElement dataElement : dataElements )
        {
            if ( !dataElementIds.contains( dataElement.getId() ) )
            {
                throw NotAllowedException.INVALID_PROGRAM_STAGE;
            }
            dataElementMap.put( dataElement.getId(), dataElement );
        }

        // Set ProgramStageInstance to completed
        if ( programStageSectionId == 0 )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }

        // Everything is fine, hence save
        saveDataValues( activityValue, programStageInstance, dataElementMap );

    }

    @Override
    public String saveProgramStage( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage mobileProgramStage,
        int patientId, int orgUnitId )
            throws NotAllowedException
    {
        if ( mobileProgramStage.isSingleEvent() )
        {
            TrackedEntityInstance patient = entityInstanceService.getTrackedEntityInstance( patientId );
            ProgramStageInstance prStageInstance = programStageInstanceService
                .getProgramStageInstance( mobileProgramStage.getId() );
            ProgramStage programStage = programStageService
                .getProgramStage( prStageInstance.getProgramStage().getId() );
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnitId );

            // ---------------------------------------------------------------------
            // Add a new program-instance
            //
            // ---------------------------------------------------------------------
            ProgramInstance programInstance = new ProgramInstance();
            programInstance.setEnrollmentDate( new Date() );
            programInstance.setIncidentDate( new Date() );
            programInstance.setProgram( programStage.getProgram() );
            programInstance.setStatus( ProgramStatus.COMPLETED );
            programInstance.setEntityInstance( patient );

            programInstanceService.addProgramInstance( programInstance );

            // ---------------------------------------------------------------------
            // Add a new program-stage-instance
            //
            // ---------------------------------------------------------------------

            ProgramStageInstance programStageInstance = new ProgramStageInstance();
            programStageInstance.setProgramInstance( programInstance );
            programStageInstance.setProgramStage( programStage );
            programStageInstance.setDueDate( new Date() );
            programStageInstance.setExecutionDate( new Date() );
            programStageInstance.setOrganisationUnit( organisationUnit );
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstanceService.addProgramStageInstance( programStageInstance );

            // ---------------------------------------------------------------------
            // Save value
            //
            // ---------------------------------------------------------------------

            List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement> dataElements = mobileProgramStage
                .getDataElements();

            for ( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement dataElement1 : dataElements )
            {
                DataElement dataElement = dataElementService.getDataElement( dataElement1.getId() );

                String value = dataElement1.getValue();

                if ( ValueType.DATE == dataElement.getValueType() && !value.trim().equals( "" ) )
                {
                    value = PeriodUtil.convertDateFormat( value );
                }

                TrackedEntityDataValue patientDataValue = new TrackedEntityDataValue();
                patientDataValue.setDataElement( dataElement );

                patientDataValue.setValue( value );
                patientDataValue.setProgramStageInstance( programStageInstance );
                patientDataValue.setLastUpdated( new Date() );
                dataValueService.saveTrackedEntityDataValue( patientDataValue );

            }

            return SINGLE_EVENT_UPLOADED;

        }
        else
        {
            ProgramStageInstance programStageInstance = programStageInstanceService
                .getProgramStageInstance( mobileProgramStage.getId() );

            List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement> dataElements = mobileProgramStage
                .getDataElements();

            try
            {
                OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnitId );
                programStageInstance.setOrganisationUnit( organisationUnit );
            }
            catch ( Exception e )
            {
                programStageInstance.setOrganisationUnit( null );
            }

            for ( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement dataElement1 : dataElements )
            {
                DataElement dataElement = dataElementService.getDataElement( dataElement1.getId() );
                String value = dataElement1.getValue();
                if ( value != null )
                {
                    if ( ValueType.DATE == dataElement.getValueType() && !value.trim().equals( "" ) )
                    {
                        value = PeriodUtil.convertDateFormat( value );
                    }

                    TrackedEntityDataValue previousPatientDataValue = dataValueService
                        .getTrackedEntityDataValue( programStageInstance, dataElement );

                    if ( previousPatientDataValue == null )
                    {
                        TrackedEntityDataValue patientDataValue = new TrackedEntityDataValue( programStageInstance,
                            dataElement, value );
                        dataValueService.saveTrackedEntityDataValue( patientDataValue );
                    }
                    else
                    {
                        previousPatientDataValue.setValue( value );
                        previousPatientDataValue.setLastUpdated( new Date() );
                        previousPatientDataValue.setProvidedElsewhere( false );
                        dataValueService.updateTrackedEntityDataValue( previousPatientDataValue );
                    }

                }

            }

            if ( DateUtils.getMediumDate( mobileProgramStage.getReportDate() ) != null )
            {
                programStageInstance.setExecutionDate( DateUtils.getMediumDate( mobileProgramStage.getReportDate() ) );
            }
            else
            {
                programStageInstance.setExecutionDate( new Date() );
            }

            if ( programStageInstance.getProgramStage().getProgramStageDataElements().size() > dataElements.size() )
            {
                programStageInstanceService.updateProgramStageInstance( programStageInstance );
                return PROGRAM_STAGE_SECTION_UPLOADED;
            }
            else
            {
                if ( mobileProgramStage.isCompleted() )
                {
                    programStageInstance.setStatus( EventStatus.COMPLETED );
                }
                programStageInstanceService.updateProgramStageInstance( programStageInstance );

                // check if all belonged program stage are completed
                if ( !mobileProgramStage.isRepeatable() && isAllProgramStageFinished( programStageInstance ) == true )
                {

                    ProgramInstance programInstance = programStageInstance.getProgramInstance();
                    programInstance.setStatus( ProgramStatus.COMPLETED );
                    programInstanceService.updateProgramInstance( programInstance );
                }

                if ( mobileProgramStage.isRepeatable() )
                {
                    Date nextDate = DateUtils.getDateAfterAddition( new Date(),
                        mobileProgramStage.getStandardInterval() );

                    return PROGRAM_STAGE_UPLOADED + "$" + DateUtils.getMediumDateString( nextDate );
                }
                else
                {
                    return PROGRAM_STAGE_UPLOADED;
                }
            }
        }
    }

    private boolean isAllProgramStageFinished( ProgramStageInstance programStageInstance )
    {
        ProgramInstance programInstance = programStageInstance.getProgramInstance();
        Collection<ProgramStageInstance> programStageInstances = programInstance.getProgramStageInstances();
        if ( programStageInstances != null )
        {
            if ( programStageInstances.size() < programInstance.getProgram().getProgramStages().size() )
            {
                return false;
            }
            Iterator<ProgramStageInstance> iterator = programStageInstances.iterator();

            while ( iterator.hasNext() )
            {
                ProgramStageInstance each = iterator.next();
                if ( !each.isCompleted() )
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Patient enrollProgram( String enrollInfo,
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage> mobileProgramStageList, Date incidentDate )
            throws NotAllowedException
    {
        String[] enrollProgramInfo = enrollInfo.split( "-" );
        int patientId = Integer.parseInt( enrollProgramInfo[0] );
        int programId = Integer.parseInt( enrollProgramInfo[1] );

        TrackedEntityInstance patient = entityInstanceService.getTrackedEntityInstance( patientId );
        Program program = programService.getProgram( programId );

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( patient, program,
            new Date(), incidentDate, patient.getOrganisationUnit() );

        Iterator<ProgramStage> programStagesIterator = program.getProgramStages().iterator();

        for ( int i = 0; i < program.getProgramStages().size(); i++ )
        {
            ProgramStage programStage = programStagesIterator.next();

            if ( programStage.getAutoGenerateEvent() )
            {
                ProgramStageInstance programStageInstance = programStageInstanceService.createProgramStageInstance(
                    programInstance, programStage, new Date(), incidentDate, patient.getOrganisationUnit() );

                int programStageInstanceId = programStageInstance.getId();

                // Inject Datavalue avaiable on-the-fly
                if ( mobileProgramStageList != null && mobileProgramStageList.size() > 0 )
                {
                    org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage mobileProgramStage = mobileProgramStageList
                        .get( i );
                    if ( mobileProgramStage != null && mobileProgramStage.getDataElements().size() > 0 )
                    {
                        mobileProgramStage.setId( programStageInstanceId );
                        if ( mobileProgramStage.isSingleEvent() )
                        {
                            this.saveProgramStage( mobileProgramStage, patientId,
                                patient.getOrganisationUnit().getId() );
                        }
                        else
                        {
                            this.saveProgramStage( mobileProgramStage, patientId, 0 );
                        }
                    }
                }

                programInstance.getProgramStageInstances().add( programStageInstance );
            }
        }
        programInstanceService.updateProgramInstance( programInstance );
        patient.getProgramInstances().add( programInstance );
        entityInstanceService.updateTrackedEntityInstance( patient );
        patient = entityInstanceService.getTrackedEntityInstance( patientId );

        return getPatientModel( patient );
    }

    // -------------------------------------------------------------------------
    // Supportive method
    // -------------------------------------------------------------------------

    private Activity getActivity( ProgramStageInstance instance, boolean late )
    {

        Activity activity = new Activity();
        TrackedEntityInstance patient = instance.getProgramInstance().getEntityInstance();

        activity.setBeneficiary( getBeneficiaryModel( patient ) );
        activity.setDueDate( instance.getDueDate() );
        activity.setTask( getTask( instance ) );
        activity.setLate( late );
        activity.setExpireDate( DateUtils.getDateAfterAddition( instance.getDueDate(), 30 ) );

        return activity;
    }

    private Task getTask( ProgramStageInstance instance )
    {
        if ( instance == null )
            return null;

        Task task = new Task();
        task.setCompleted( instance.isCompleted() );
        task.setId( instance.getId() );
        task.setProgramStageId( instance.getProgramStage().getId() );
        task.setProgramId( instance.getProgramInstance().getProgram().getId() );
        return task;
    }

    private Beneficiary getBeneficiaryModel( TrackedEntityInstance patient )
    {
        Beneficiary beneficiary = new Beneficiary();
        List<org.hisp.dhis.api.mobile.model.PatientAttribute> patientAtts = new ArrayList<>();
        beneficiary.setId( patient.getId() );
        beneficiary.setName( patient.getName() );

        // Set attribute which is used to group beneficiary on mobile (only if
        // there is attribute which is set to be group factor)
        org.hisp.dhis.api.mobile.model.PatientAttribute beneficiaryAttribute = null;

        if ( groupByAttribute != null )
        {
            beneficiaryAttribute = new org.hisp.dhis.api.mobile.model.PatientAttribute();
            beneficiaryAttribute.setName( groupByAttribute.getName() );
            TrackedEntityAttributeValue value = attValueService.getTrackedEntityAttributeValue( patient,
                groupByAttribute );
            beneficiaryAttribute.setValue( value == null ? "Unknown" : value.getValue() );
            beneficiary.setGroupAttribute( beneficiaryAttribute );
        }

        beneficiary.setPatientAttValues( patientAtts );
        return beneficiary;
    }

    // get patient model for LWUIT
    private org.hisp.dhis.api.mobile.model.LWUITmodel.Patient getPatientModel( TrackedEntityInstance patient )
    {
        org.hisp.dhis.api.mobile.model.LWUITmodel.Patient patientModel = new org.hisp.dhis.api.mobile.model.LWUITmodel.Patient();
        List<org.hisp.dhis.api.mobile.model.PatientAttribute> patientAtts = new ArrayList<>();

        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance> mobileProgramInstanceList = new ArrayList<>();

        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance> mobileCompletedProgramInstanceList = new ArrayList<>();

        patientModel.setId( patient.getId() );

        if ( patient.getOrganisationUnit() != null )
        {
            patientModel.setOrganisationUnitName( patient.getOrganisationUnit().getName() );
        }

        if ( patient.getTrackedEntity() != null )
        {
            patientModel.setTrackedEntityName( patient.getTrackedEntity().getName() );
        }
        else
        {
            patientModel.setTrackedEntityName( "" );
        }

        List<TrackedEntityAttributeValue> atts = new ArrayList<>( patient.getTrackedEntityAttributeValues() );

        for ( TrackedEntityAttributeValue value : atts )
        {
            if ( value != null )
            {
                org.hisp.dhis.api.mobile.model.PatientAttribute patientAttribute = new org.hisp.dhis.api.mobile.model.PatientAttribute(
                    value.getAttribute().getName(), value.getValue(), null, false,
                    value.getAttribute().getDisplayInListNoProgram(), new OptionSet() );
                patientAttribute.setType( value.getAttribute().getValueType() );

                patientAtts.add( patientAttribute );

            }
        }

        patientModel.setAttributes( patientAtts );

        List<ProgramInstance> listOfProgramInstance = new ArrayList<>(
            programInstanceService.getProgramInstances( new ProgramInstanceQueryParams()
                .setTrackedEntityInstance( patient )
                .setProgramStatus( ProgramStatus.ACTIVE )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL ) ) );

        if ( listOfProgramInstance.size() > 0 )
        {
            for ( ProgramInstance each : listOfProgramInstance )
            {
                mobileProgramInstanceList.add( getMobileProgramInstance( each ) );
            }
        }
        patientModel.setEnrollmentPrograms( mobileProgramInstanceList );

        List<ProgramInstance> listOfCompletedProgramInstance = new ArrayList<>(
            programInstanceService.getProgramInstances( new ProgramInstanceQueryParams()
                .setTrackedEntityInstance( patient )
                .setProgramStatus( ProgramStatus.COMPLETED )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL ) ) );

        if ( listOfCompletedProgramInstance.size() > 0 )
        {
            for ( ProgramInstance each : listOfCompletedProgramInstance )
            {
                mobileCompletedProgramInstanceList.add( getMobileProgramInstance( each ) );
            }
        }

        patientModel.setCompletedPrograms( mobileCompletedProgramInstanceList );

        // Set Relationship
        Collection<Relationship> relationships = relationshipService
            .getRelationshipsForTrackedEntityInstance( patient );
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship> relationshipList = new ArrayList<>();

        for ( Relationship eachRelationship : relationships )
        {
            org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship relationshipMobile = new org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship();
            relationshipMobile.setId( eachRelationship.getId() );
            if ( eachRelationship.getEntityInstanceA().getId() == patient.getId() )
            {
                relationshipMobile.setName( eachRelationship.getRelationshipType().getaIsToB() );
                relationshipMobile.setaIsToB( eachRelationship.getRelationshipType().getaIsToB() );
                relationshipMobile.setbIsToA( eachRelationship.getRelationshipType().getbIsToA() );
                relationshipMobile.setPersonBId( eachRelationship.getEntityInstanceB().getId() );
            }
            else
            {
                relationshipMobile.setName( eachRelationship.getRelationshipType().getbIsToA() );
                relationshipMobile.setaIsToB( eachRelationship.getRelationshipType().getbIsToA() );
                relationshipMobile.setbIsToA( eachRelationship.getRelationshipType().getaIsToB() );
                relationshipMobile.setPersonBId( eachRelationship.getEntityInstanceA().getId() );
            }

            // get relative's name
            TrackedEntityInstance relative = entityInstanceService
                .getTrackedEntityInstance( relationshipMobile.getPersonBId() );
            List<TrackedEntityAttributeValue> attributes = new ArrayList<>(
                relative.getTrackedEntityAttributeValues() );
            List<TrackedEntityAttributeValue> attributesInList = new ArrayList<>();

            for ( TrackedEntityAttributeValue value : attributes )
            {
                if ( value != null && value.getAttribute().getDisplayInListNoProgram() )
                {
                    attributesInList.add( value );
                }
            }

            Collections.sort( attributesInList, new TrackedEntityAttributeValueSortOrderComparator() );

            String relativeName = "";
            for ( TrackedEntityAttributeValue value : attributesInList )
            {
                if ( value != null && value.getAttribute().getDisplayInListNoProgram() )
                {
                    relativeName += value.getValue() + " ";
                }
            }
            relationshipMobile.setPersonBName( relativeName );

            relationshipList.add( relationshipMobile );
        }
        patientModel.setRelationships( relationshipList );

        return patientModel;
    }

    private org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance getMobileProgramInstance(
        ProgramInstance programInstance )
    {
        org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance mobileProgramInstance = new org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance();

        mobileProgramInstance.setId( programInstance.getId() );
        mobileProgramInstance.setName( programInstance.getProgram().getName() );
        mobileProgramInstance.setStatus( programInstance.getStatus().getValue() );
        mobileProgramInstance
            .setDateOfEnrollment( DateUtils.getMediumDateString( programInstance.getEnrollmentDate() ) );
        mobileProgramInstance.setDateOfIncident( DateUtils.getMediumDateString( programInstance.getIncidentDate() ) );
        mobileProgramInstance.setPatientId( programInstance.getEntityInstance().getId() );
        mobileProgramInstance.setProgramId( programInstance.getProgram().getId() );
        mobileProgramInstance.setProgramStageInstances( getMobileProgramStages( programInstance ) );
        return mobileProgramInstance;
    }

    private List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage> getMobileProgramStages(
        ProgramInstance programInstance )
    {
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage> mobileProgramStages = new ArrayList<>();
        List<ProgramStageInstance> proStageInstanceList = new ArrayList<>( programInstance.getProgramStageInstances() );

        Collections.sort( proStageInstanceList, new ProgramStageInstanceVisitDateComparator() );

        for ( ProgramStageInstance eachProgramStageInstance : proStageInstanceList )
        {
            // only for Mujhu database, because there is null program stage
            // instance. This condition should be removed in the future
            if ( eachProgramStageInstance != null )
            {
                ProgramStage programStage = eachProgramStageInstance.getProgramStage();

                org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage mobileProgramStage = new org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage();
                List<org.hisp.dhis.api.mobile.model.LWUITmodel.Section> mobileSections = new ArrayList<>();
                mobileProgramStage.setId( eachProgramStageInstance.getId() );
                /* mobileProgramStage.setName( eachProgramStage.getName() ); */
                mobileProgramStage.setName( programStage.getName() );

                // get report date
                if ( eachProgramStageInstance.getExecutionDate() != null )
                {
                    mobileProgramStage
                        .setReportDate( DateUtils.getMediumDateString( eachProgramStageInstance.getExecutionDate() ) );
                }
                else
                {
                    mobileProgramStage.setReportDate( "" );
                }

                if ( programStage.getExecutionDateLabel() == null )
                {
                    mobileProgramStage.setReportDateDescription( "Report Date" );
                }
                else
                {
                    mobileProgramStage.setReportDateDescription( programStage.getExecutionDateLabel() );
                }

                // get due date
                if ( eachProgramStageInstance.getDueDate() != null )
                {
                    mobileProgramStage
                        .setDueDate( DateUtils.getMediumDateString( eachProgramStageInstance.getDueDate() ) );
                }
                else
                {
                    mobileProgramStage.setDueDate( "" );
                }

                // is repeatable
                mobileProgramStage.setRepeatable( programStage.getRepeatable() );

                if ( programStage.getStandardInterval() == null )
                {
                    mobileProgramStage.setStandardInterval( 0 );
                }
                else
                {
                    mobileProgramStage.setStandardInterval( programStage.getStandardInterval() );
                }

                // is completed
                /*
                 * mobileProgramStage.setCompleted(
                 * checkIfProgramStageCompleted( patient,
                 * programInstance.getProgram(), programStage ) );
                 */
                mobileProgramStage.setCompleted( eachProgramStageInstance.isCompleted() );

                // is single event
                mobileProgramStage.setSingleEvent( programInstance.getProgram().isWithoutRegistration() );

                // Set all data elements
                mobileProgramStage
                    .setDataElements( getDataElementsForMobile( programStage, eachProgramStageInstance ) );

                // Set all program sections
                if ( programStage.getProgramStageSections().size() > 0 )
                {
                    for ( ProgramStageSection eachSection : programStage.getProgramStageSections() )
                    {
                        org.hisp.dhis.api.mobile.model.LWUITmodel.Section mobileSection = new org.hisp.dhis.api.mobile.model.LWUITmodel.Section();
                        mobileSection.setId( eachSection.getId() );
                        mobileSection.setName( eachSection.getName() );

                        // Set all data elements' id, then we could have full
                        // from
                        // data element list of program stage
                        List<Integer> dataElementIds = new ArrayList<>();
                        for ( DataElement dataElement : eachSection.getDataElements() )
                        {
                            dataElementIds.add( dataElement.getId() );
                        }
                        mobileSection.setDataElementIds( dataElementIds );
                        mobileSections.add( mobileSection );
                    }
                }
                mobileProgramStage.setSections( mobileSections );

                mobileProgramStages.add( mobileProgramStage );
            }
        }
        return mobileProgramStages;
    }

    private List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement> getDataElementsForMobile(
        ProgramStage programStage, ProgramStageInstance programStageInstance )
    {
        List<ProgramStageDataElement> programStageDataElements = new ArrayList<>(
            programStage.getProgramStageDataElements() );
        List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement> mobileDataElements = new ArrayList<>();
        for ( ProgramStageDataElement programStageDataElement : programStageDataElements )
        {
            org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement mobileDataElement = new org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement();
            mobileDataElement.setId( programStageDataElement.getDataElement().getId() );

            String dataElementName;

            if ( programStageDataElement.getDataElement().getFormName() != null
                && !programStageDataElement.getDataElement().getFormName().trim().equals( "" ) )
            {
                dataElementName = programStageDataElement.getDataElement().getFormName();
            }
            else
            {
                dataElementName = programStageDataElement.getDataElement().getName();
            }

            mobileDataElement.setName( dataElementName );

            mobileDataElement.setType( programStageDataElement.getDataElement().getValueType() );

            // problem
            mobileDataElement.setCompulsory( programStageDataElement.isCompulsory() );

            // mobileDataElement.setNumberType(
            // programStageDataElement.getDataElement().getNumberType() );

            // Value
            TrackedEntityDataValue patientDataValue = dataValueService.getTrackedEntityDataValue( programStageInstance,
                programStageDataElement.getDataElement() );

            if ( patientDataValue != null )
            {
                // Convert to standard date format before send to client
                if ( ValueType.DATE == programStageDataElement.getDataElement().getValueType() )
                {
                    mobileDataElement.setValue( PeriodUtil.convertDateFormat( patientDataValue.getValue() ) );
                }
                else
                {
                    mobileDataElement.setValue( patientDataValue.getValue() );
                }
            }
            else
            {
                mobileDataElement.setValue( null );
            }

            // Option set
            if ( programStageDataElement.getDataElement().getOptionSet() != null )
            {
                mobileDataElement.setOptionSet( ModelMapping.getOptionSet( programStageDataElement.getDataElement() ) );
            }
            else
            {
                mobileDataElement.setOptionSet( null );
            }

            // Category Option Combo
            if ( programStageDataElement.getDataElement().getCategoryCombos() != null )
            {
                mobileDataElement.setCategoryOptionCombos(
                    ModelMapping.getCategoryOptionCombos( programStageDataElement.getDataElement() ) );
            }
            else
            {
                mobileDataElement.setCategoryOptionCombos( null );
            }
            mobileDataElements.add( mobileDataElement );
        }
        return mobileDataElements;
    }

    private boolean isNumber( String value )
    {
        try
        {
            Double.parseDouble( value );
        }
        catch ( NumberFormatException e )
        {
            return false;
        }
        return true;
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Patient addRelationship(
        org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship enrollmentRelationship, int orgUnitId )
            throws NotAllowedException
    {
        TrackedEntityInstance patientB;
        if ( enrollmentRelationship.getPersonBId() != 0 )
        {
            patientB = entityInstanceService.getTrackedEntityInstance( enrollmentRelationship.getPersonBId() );
        }
        else
        {
            String instanceInfo = findPatientInAdvanced( enrollmentRelationship.getPersonBName(), orgUnitId, 0 );
            if ( instanceInfo == null || instanceInfo.trim().length() == 0 )
            {
                throw NotAllowedException.NO_BENEFICIARY_FOUND;
            }
            else
            {
                throw new NotAllowedException( instanceInfo );
            }
        }
        TrackedEntityInstance patientA = entityInstanceService
            .getTrackedEntityInstance( enrollmentRelationship.getPersonAId() );
        RelationshipType relationshipType = relationshipTypeService
            .getRelationshipType( enrollmentRelationship.getId() );

        Relationship relationship = new Relationship();
        relationship.setRelationshipType( relationshipType );
        if ( enrollmentRelationship.getChosenRelationship().equals( relationshipType.getaIsToB() ) )
        {
            relationship.setEntityInstanceA( patientA );
            relationship.setEntityInstanceB( patientB );
        }
        else
        {
            relationship.setEntityInstanceA( patientB );
            relationship.setEntityInstanceB( patientA );
        }
        relationshipService.addRelationship( relationship );
        // return getPatientModel( orgUnitId, patientA );
        return getPatientModel( patientA );
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Program getAllProgramByOrgUnit( int orgUnitId, String type )
        throws NotAllowedException
    {
        String programsInfo = "";

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( orgUnitId );

        Set<Program> tempPrograms = null;

        ProgramType programType = ProgramType.fromValue( type );

        if ( programType == ProgramType.WITHOUT_REGISTRATION )
        {
            tempPrograms = programService.getUserPrograms( ProgramType.WITHOUT_REGISTRATION );
        }
        else // ProgramType.WITH_REGISTRATION
        {
            tempPrograms = programService.getUserPrograms( ProgramType.WITH_REGISTRATION );
        }

        List<Program> programs = new ArrayList<>();

        for ( Program program : tempPrograms )
        {
            if ( program.getOrganisationUnits().contains( organisationUnit ) )
            {
                programs.add( program );
            }
        }

        if ( programs.size() != 0 )
        {
            if ( programs.size() == 1 )
            {
                Program program = programs.get( 0 );

                return getMobileProgramWithoutData( program );
            }
            else
            {
                for ( Program program : programs )
                {
                    if ( program.getOrganisationUnits().contains( organisationUnit ) )
                    {
                        programsInfo += program.getId() + "/" + program.getName() + "$";
                    }
                }

                throw new NotAllowedException( programsInfo );
            }
        }
        else
        {
            throw NotAllowedException.NO_PROGRAM_FOUND;
        }
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Program findProgram( String programInfo )
        throws NotAllowedException
    {
        if ( isNumber( programInfo ) == false )
        {
            return null;
        }
        else
        {
            Program program = programService.getProgram( Integer.parseInt( programInfo ) );
            if ( program.isWithoutRegistration() )
            {
                return getMobileProgramWithoutData( program );
            }
            else
            {
                return null;
            }
        }
    }

    // If the return program is anonymous, the client side will show the entry
    // form as normal
    // If the return program is not anonymous, it is still OK because in client
    // side, we only need name and id
    private org.hisp.dhis.api.mobile.model.LWUITmodel.Program getMobileProgramWithoutData( Program program )
    {
        Comparator<ProgramStageDataElement> orderBySortOrder = ( ProgramStageDataElement i1,
            ProgramStageDataElement i2 ) -> i1.getSortOrder().compareTo( i2.getSortOrder() );

        org.hisp.dhis.api.mobile.model.LWUITmodel.Program anonymousProgramMobile = new org.hisp.dhis.api.mobile.model.LWUITmodel.Program();

        anonymousProgramMobile.setId( program.getId() );

        anonymousProgramMobile.setName( program.getName() );

        // if ( program.getType() == Program.SINGLE_EVENT_WITHOUT_REGISTRATION )
        {
            anonymousProgramMobile.setVersion( program.getVersion() );

            ProgramStage programStage = program.getProgramStages().iterator().next();

            List<ProgramStageDataElement> programStageDataElements = new ArrayList<>(
                programStage.getProgramStageDataElements() );
            Collections.sort( programStageDataElements, orderBySortOrder );

            List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage> mobileProgramStages = new ArrayList<>();

            org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage mobileProgramStage = new org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage();

            List<org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement> mobileProgramStageDataElements = new ArrayList<>();

            mobileProgramStage.setId( programStage.getId() );
            mobileProgramStage.setName( programStage.getName() );
            mobileProgramStage.setCompleted( false );
            mobileProgramStage.setRepeatable( false );
            mobileProgramStage.setSingleEvent( true );
            mobileProgramStage.setSections( new ArrayList<>() );

            // get report date
            mobileProgramStage.setReportDate( DateUtils.getMediumDateString() );

            if ( programStage.getExecutionDateLabel() == null )
            {
                mobileProgramStage.setReportDateDescription( "Report Date" );
            }
            else
            {
                mobileProgramStage.setReportDateDescription( programStage.getExecutionDateLabel() );
            }

            for ( ProgramStageDataElement programStageDataElement : programStageDataElements )
            {
                org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement mobileDataElement = new org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement();
                mobileDataElement.setId( programStageDataElement.getDataElement().getId() );
                mobileDataElement.setName( programStageDataElement.getDataElement().getName() );
                mobileDataElement.setType( programStageDataElement.getDataElement().getValueType() );

                // problem
                mobileDataElement.setCompulsory( programStageDataElement.isCompulsory() );

                // mobileDataElement.setNumberType(
                // programStageDataElement.getDataElement().getNumberType() );

                mobileDataElement.setValue( "" );

                if ( programStageDataElement.getDataElement().getOptionSet() != null )
                {
                    mobileDataElement
                        .setOptionSet( ModelMapping.getOptionSet( programStageDataElement.getDataElement() ) );
                }
                else
                {
                    mobileDataElement.setOptionSet( null );
                }
                if ( programStageDataElement.getDataElement().getCategoryCombos() != null )
                {
                    mobileDataElement.setCategoryOptionCombos(
                        ModelMapping.getCategoryOptionCombos( programStageDataElement.getDataElement() ) );
                }
                else
                {
                    mobileDataElement.setCategoryOptionCombos( null );
                }
                mobileProgramStageDataElements.add( mobileDataElement );
            }
            mobileProgramStage.setDataElements( mobileProgramStageDataElements );
            mobileProgramStages.add( mobileProgramStage );
            anonymousProgramMobile.setProgramStages( mobileProgramStages );
        }

        return anonymousProgramMobile;
    }

    private void saveDataValues( ActivityValue activityValue, ProgramStageInstance programStageInstance,
        Map<Integer, DataElement> dataElementMap )
    {
        org.hisp.dhis.dataelement.DataElement dataElement;
        String value;

        for ( DataValue dv : activityValue.getDataValues() )
        {
            value = dv.getValue();

            if ( value != null && value.trim().length() == 0 )
            {
                value = null;
            }

            if ( value != null )
            {
                value = value.trim();
            }

            dataElement = dataElementMap.get( dv.getId() );
            TrackedEntityDataValue dataValue = dataValueService.getTrackedEntityDataValue( programStageInstance,
                dataElement );
            if ( dataValue == null )
            {
                if ( value != null )
                {
                    if ( programStageInstance.getExecutionDate() == null )
                    {
                        programStageInstance.setExecutionDate( new Date() );
                        programStageInstanceService.updateProgramStageInstance( programStageInstance );
                    }

                    dataValue = new TrackedEntityDataValue( programStageInstance, dataElement, value );

                    dataValueService.saveTrackedEntityDataValue( dataValue );
                }
            }
            else
            {
                if ( programStageInstance.getExecutionDate() == null )
                {
                    programStageInstance.setExecutionDate( new Date() );
                    programStageInstanceService.updateProgramStageInstance( programStageInstance );
                }

                dataValue.setValue( value );
                dataValue.setLastUpdated( new Date() );

                dataValueService.updateTrackedEntityDataValue( dataValue );
            }
        }
    }

    @Override
    public List<TrackedEntityAttribute> getPatientAtts( String programId )
    {
        List<TrackedEntityAttribute> patientAttributes = null;

        if ( programId != null && !programId.trim().equals( "" ) )
        {
            Program program = programService.getProgram( Integer.parseInt( programId ) );
            patientAttributes = program.getTrackedEntityAttributes();
        }
        else
        {
            patientAttributes = attributeService.getAllTrackedEntityAttributes();
        }

        return patientAttributes;
    }

    @Override
    public List<org.hisp.dhis.api.mobile.model.PatientAttribute> getAttsForMobile()
    {
        List<org.hisp.dhis.api.mobile.model.PatientAttribute> list = new ArrayList<>();

        for ( TrackedEntityAttribute patientAtt : getPatientAtts( null ) )
        {
            PatientAttribute patientAttribute = new PatientAttribute( patientAtt.getName(), null, null, false,
                patientAtt.getDisplayInListNoProgram(), new OptionSet() );
            patientAttribute.setType( patientAtt.getValueType() );
            list.add( patientAttribute );
        }

        return list;
    }

    @Override
    public List<org.hisp.dhis.api.mobile.model.PatientAttribute> getPatientAttributesForMobile( String programId )
    {
        List<org.hisp.dhis.api.mobile.model.PatientAttribute> list = new ArrayList<>();

        for ( TrackedEntityAttribute pa : getPatientAtts( programId ) )
        {
            PatientAttribute patientAttribute = new PatientAttribute();
            String name = pa.getName();

            patientAttribute.setName( name );
            patientAttribute.setType( pa.getValueType() );
            patientAttribute.setValue( "" );

            list.add( patientAttribute );
        }

        return list;
    }

    @Required
    public void setRelationshipTypeService( RelationshipTypeService relationshipTypeService )
    {
        this.relationshipTypeService = relationshipTypeService;
    }

    @Required
    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    @Override
    public Patient savePatient( org.hisp.dhis.api.mobile.model.LWUITmodel.Patient patient, int orgUnitId,
        String programIdText )
            throws NotAllowedException
    {
        TrackedEntityInstance patientWeb = new TrackedEntityInstance();
        patientWeb.setOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );

        Set<TrackedEntityAttribute> patientAttributeSet = new HashSet<>();
        Set<TrackedEntityAttributeValue> patientAttributeValues = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.PatientAttribute> attributesMobile = patient.getAttributes();

        if ( attributesMobile != null )
        {
            for ( org.hisp.dhis.api.mobile.model.PatientAttribute paAtt : attributesMobile )
            {

                TrackedEntityAttribute patientAttribute = attributeService
                    .getTrackedEntityAttributeByName( paAtt.getName() );

                patientAttributeSet.add( patientAttribute );

                TrackedEntityAttributeValue patientAttributeValue = new TrackedEntityAttributeValue();

                patientAttributeValue.setEntityInstance( patientWeb );
                patientAttributeValue.setAttribute( patientAttribute );
                patientAttributeValue.setValue( paAtt.getValue() );
                patientAttributeValues.add( patientAttributeValue );
            }
        }

        patientWeb.setTrackedEntity( trackedEntityService.getTrackedEntityByName( patient.getTrackedEntityName() ) );
        patientId = entityInstanceService.createTrackedEntityInstance( patientWeb, null, null, patientAttributeValues );
        TrackedEntityInstance newTrackedEntityInstance = entityInstanceService
            .getTrackedEntityInstance( this.patientId );
        String errorMsg = null;

        try
        {
            for ( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance mobileProgramInstance : patient
                .getEnrollmentPrograms() )
            {
                Date incidentDate = DateUtils.getMediumDate( mobileProgramInstance.getDateOfIncident() );
                enrollProgram( patientId + "-" + mobileProgramInstance.getProgramId(),
                    mobileProgramInstance.getProgramStageInstances(), incidentDate );
            }

            Program program = programService.getProgram( Integer.parseInt( programIdText ) );
            String[] errorCode = entityInstanceService
                .validateTrackedEntityInstance( newTrackedEntityInstance, program ).split( "_" );
            int code = Integer.parseInt( errorCode[0] );

            if ( code >= 1 )
            {
                entityInstanceService.deleteTrackedEntityInstance( newTrackedEntityInstance );
                if ( code == TrackedEntityInstanceService.ERROR_DUPLICATE_IDENTIFIER )
                {
                    errorMsg = "Duplicate value of " + attributeService
                        .getTrackedEntityAttribute( Integer.parseInt( errorCode[1] ) ).getDisplayName();
                }
                else
                {
                    errorMsg = "Validation error";
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        if ( errorMsg != null )
        {
            throw new NotAllowedException( errorMsg );
        }

        return getPatientModel( newTrackedEntityInstance );

    }

    @Override
    public Patient updatePatient( org.hisp.dhis.api.mobile.model.LWUITmodel.Patient patient, int orgUnitId,
        String programIdText )
            throws NotAllowedException
    {
        TrackedEntityInstance entityInstance = entityInstanceService.getTrackedEntityInstance( patient.getId() );
        TrackedEntityInstance tempTEI = entityInstance;
        TrackedEntity trackedEntity = null;

        Program program = programService.getProgram( Integer.parseInt( programIdText ) );
        trackedEntity = program.getTrackedEntity();

        entityInstance.setTrackedEntity( trackedEntity );

        // get attributes to be saved/updated/deleted
        Collection<TrackedEntityAttribute> attributes = attributeService.getAllTrackedEntityAttributes();

        List<TrackedEntityAttributeValue> valuesForSave = new ArrayList<>();
        List<TrackedEntityAttributeValue> valuesForUpdate = new ArrayList<>();
        Collection<TrackedEntityAttributeValue> valuesForDelete = null;

        TrackedEntityAttributeValue attributeValue = null;

        Collection<org.hisp.dhis.api.mobile.model.PatientAttribute> attributesMobile = patient.getAttributes();

        if ( attributes != null && attributes.size() > 0 )
        {
            valuesForDelete = attValueService.getTrackedEntityAttributeValues( entityInstance );
            tempTEI.getTrackedEntityAttributeValues().clear();

            for ( TrackedEntityAttribute attribute : attributes )
            {
                String value = getAttributeValue( attributesMobile, attribute.getName() );

                if ( value != null )
                {
                    attributeValue = attValueService.getTrackedEntityAttributeValue( entityInstance, attribute );

                    if ( attributeValue == null )
                    {
                        attributeValue = new TrackedEntityAttributeValue();
                        attributeValue.setEntityInstance( entityInstance );
                        attributeValue.setAttribute( attribute );
                        attributeValue.setValue( value.trim() );
                        valuesForSave.add( attributeValue );
                    }
                    else
                    {
                        attributeValue.setValue( value.trim() );
                        valuesForUpdate.add( attributeValue );
                        valuesForDelete.remove( attributeValue );
                    }
                    tempTEI.getTrackedEntityAttributeValues().add( attributeValue );
                }
            }
        }

        // validate
        String[] errorCode = entityInstanceService.validateTrackedEntityInstance( tempTEI, program ).split( "_" );
        int code = Integer.parseInt( errorCode[0] );

        if ( code >= 1 )
        {
            if ( code == TrackedEntityInstanceService.ERROR_DUPLICATE_IDENTIFIER )
            {
                throw new NotAllowedException( "Duplicate value of "
                    + attributeService.getTrackedEntityAttribute( Integer.parseInt( errorCode[1] ) ).getDisplayName() );
            }
            else
            {
                throw new NotAllowedException( "Validation error" );
            }
        }

        entityInstanceService.updateTrackedEntityInstance( entityInstance, null, null, valuesForSave, valuesForUpdate,
            valuesForDelete );
        enrollProgram( patient.getId() + "-" + programIdText, null, new Date() );
        entityInstance = entityInstanceService.getTrackedEntityInstance( patient.getId() );
        entityInstance.setTrackedEntity( trackedEntity );

        return getPatientModel( entityInstance );
    }

    public String getAttributeValue( Collection<org.hisp.dhis.api.mobile.model.PatientAttribute> attributesMobile,
        String attributeName )
    {
        for ( org.hisp.dhis.api.mobile.model.PatientAttribute attributeMobile : attributesMobile )
        {
            if ( attributeMobile.getName().equals( attributeName ) )
            {
                return attributeMobile.getValue();
            }
        }
        return null;
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Patient findPatient( String patientId )
        throws NotAllowedException
    {
        TrackedEntityInstance patient = entityInstanceService.getTrackedEntityInstance( patientId );

        // Temporary fix
        if ( patient == null )
        {
            patient = entityInstanceService.getTrackedEntityInstance( Integer.parseInt( patientId ) );
        }

        org.hisp.dhis.api.mobile.model.LWUITmodel.Patient patientMobile = getPatientModel( patient );
        return patientMobile;
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.PatientList findPatients( String patientIds )
        throws NotAllowedException
    {
        PatientList patientlist = new PatientList();

        while ( patientIds.length() > 0 )
        {
            int patientId = Integer.parseInt( patientIds.substring( 0, patientIds.indexOf( "$" ) ) );
            TrackedEntityInstance patient = entityInstanceService.getTrackedEntityInstance( patientId );
            patientlist.getPatientList().add( getPatientModel( patient ) );
            patientIds = patientIds.substring( patientIds.indexOf( "$" ) + 1, patientIds.length() );
        }

        return patientlist;
    }

    /**
     * keyword is on format of
     * {attribute-id1}:{operator1}:{filter-value1};{attribute
     * -id2}:{operator2}:{filter-value2}
     */
    @Override
    public String findPatientInAdvanced( String keyword, int orgUnitId, int programId )
        throws NotAllowedException
    {
        TrackedEntityInstanceQueryParams param = new TrackedEntityInstanceQueryParams();
        List<TrackedEntityAttribute> displayAttributes = new ArrayList<>(
            attributeService.getTrackedEntityAttributesDisplayInList() );

        for ( TrackedEntityAttribute trackedEntityAttribute : displayAttributes )
        {
            QueryItem queryItem = new QueryItem( trackedEntityAttribute );
            param.addAttribute( queryItem );
        }

        if ( programId != 0 )
        {
            param.setProgram( programService.getProgram( programId ) );
        }

        if ( orgUnitId != 0 )
        {
            param.addOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );
            param.setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED );
        }
        else
        {
            param.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        }

        String[] items = keyword.split( ";" );

        if ( items == null )
        {
            items = new String[1];
            items[0] = keyword;
        }

        for ( int i = 0; i < items.length; i++ )
        {
            String[] split = keyword.split( ":" );
            if ( split == null || (split.length != 3 && split.length != 2) )
            {
                throw NotAllowedException.INVALID_FILTER;
            }

            if ( split.length == 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[0] );
                param.setQuery( new QueryFilter( operator, split[1] ) );
            }
            else
            {
                TrackedEntityAttribute at = attributeService.getTrackedEntityAttributeByName( split[0] );
                QueryItem queryItem = new QueryItem( at, at.getLegendSets().get( 0 ), at.getValueType(), at.getAggregationType(),
                    at.getOptionSet() );
                QueryOperator operator = QueryOperator.fromString( split[1] );
                queryItem.getFilters().add( new QueryFilter( operator, split[2] ) );
                param.getFilters().add( queryItem );
            }
        }

        Grid trackedEntityInstanceGrid = entityInstanceService.getTrackedEntityInstancesGrid( param );
        List<List<Object>> listOfTrackedEntityInstance = trackedEntityInstanceGrid.getRows();

        if ( listOfTrackedEntityInstance.size() == 0 )
        {
            throw NotAllowedException.NO_BENEFICIARY_FOUND;
        }

        /**
         * Grid columns: 0 = instance 1 = created 2 = lastupdated 3 = ou 4 = te
         * 5 onwards = attributes
         */
        int instanceIndex = 0;
        int teIndex = 4;
        List<Integer> attributesIndex = new ArrayList<>();
        List<GridHeader> headers = trackedEntityInstanceGrid.getHeaders();
        int index = 0;
        for ( GridHeader header : headers )
        {
            if ( header.getName().equals( "instance" ) )
            {
                instanceIndex = index;
            }
            else if ( header.getName().equals( "te" ) )
            {
                teIndex = index;
            }
            else if ( !header.getName().equals( "created" ) && !header.getName().equals( "lastupdated" )
                && !header.getName().equals( "ou" ) )
            {
                attributesIndex.add( new Integer( index ) );
            }
            index++;
        }

        String instanceInfo = "";
        String trackedEntityName = "";
        for ( List<Object> row : listOfTrackedEntityInstance )
        {
            TrackedEntity te = trackedEntityService.getTrackedEntity( (String) row.get( teIndex ) );
            if ( !trackedEntityName.equals( te.getDisplayName() ) )
            {
                trackedEntityName = te.getDisplayName();
                instanceInfo += te.getDisplayName() + "$";
            }

            // NOTE: this line should be here but because the mobile client uses
            // the int TEI id, we will temprarily get the int id for now.
            // instanceInfo += (String) row.get( instanceIndex ) + "/";

            TrackedEntityInstance tei = entityInstanceService
                .getTrackedEntityInstance( (String) row.get( instanceIndex ) );
            instanceInfo += tei.getId() + "/";
            // end of temproary fix

            String attText = "";
            for ( Integer attIndex : attributesIndex )
            {
                if ( row.get( attIndex.intValue() ) != null )
                {
                    attText += (String) row.get( attIndex.intValue() ) + " ";
                }
            }
            instanceInfo += attText.trim() + "$";
        }

        return instanceInfo;
    }

    @Override
    public String findLostToFollowUp( int orgUnitId, String searchEventInfos )
        throws NotAllowedException
    {
        String[] searchEventInfosArray = searchEventInfos.split( "-" );

        EventStatus eventStatus = EventStatus.ACTIVE;

        if ( searchEventInfosArray[1].equalsIgnoreCase( "Scheduled in future" ) )
        {
            eventStatus = EventStatus.SCHEDULE;
        }
        else if ( searchEventInfosArray[1].equalsIgnoreCase( "Overdue" ) )
        {
            eventStatus = EventStatus.OVERDUE;
        }

        String eventsInfo = "";

        Calendar toCalendar = new GregorianCalendar();
        toCalendar.add( Calendar.DATE, -1 );
        toCalendar.add( Calendar.YEAR, 100 );
        Date toDate = toCalendar.getTime();

        Calendar fromCalendar = new GregorianCalendar();
        fromCalendar.add( Calendar.DATE, -1 );
        fromCalendar.add( Calendar.YEAR, -100 );

        Date fromDate = fromCalendar.getTime();

        TrackedEntityInstanceQueryParams param = new TrackedEntityInstanceQueryParams();
        List<TrackedEntityAttribute> trackedEntityAttributeList = new ArrayList<>(
            attributeService.getTrackedEntityAttributesByDisplayOnVisitSchedule( true ) );

        for ( TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributeList )
        {
            QueryItem queryItem = new QueryItem( trackedEntityAttribute );
            param.addAttribute( queryItem );
        }

        param.addOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );
        param.setEventStatus( eventStatus );
        param.setEventStartDate( fromDate );
        param.setEventEndDate( toDate );

        Grid programStageInstanceGrid = entityInstanceService.getTrackedEntityInstancesGrid( param );
        List<List<Object>> rows = programStageInstanceGrid.getRows();

        if ( rows.size() == 0 )
        {
            throw NotAllowedException.NO_EVENT_FOUND;
        }
        else if ( rows.size() > 0 )
        {
            for ( List<Object> row : rows )
            {
                for ( int i = 5; i < row.size(); i++ )
                {
                    eventsInfo += row.get( i ) + "/";
                    if ( i == row.size() - 1 )
                    {
                        eventsInfo += "$";
                    }
                }

            }

            throw new NotAllowedException( eventsInfo );
        }
        else
        {
            return "";
        }

    }

    @SuppressWarnings( "finally" )
    @Override
    public Notification handleLostToFollowUp( LostEvent lostEvent )
        throws NotAllowedException
    {
        Notification notification = new Notification();
        try
        {
            ProgramStageInstance programStageInstance = programStageInstanceService
                .getProgramStageInstance( lostEvent.getId() );
            programStageInstance.setDueDate( DateUtils.getMediumDate( lostEvent.getDueDate() ) );
            programStageInstance.setStatus( EventStatus.fromInt( lostEvent.getStatus() ) );

            if ( lostEvent.getComment() != null )
            {
                List<MessageConversation> conversationList = new ArrayList<>();

                MessageConversation conversation = new MessageConversation( lostEvent.getName(),
                    currentUserService.getCurrentUser(), MessageType.PRIVATE );

                conversation
                    .addMessage( new Message( lostEvent.getComment(), null, currentUserService.getCurrentUser() ) );

                conversation.setRead( true );

                conversationList.add( conversation );

                programStageInstance.setMessageConversations( conversationList );

                messageService.saveMessageConversation( conversation );
            }

            programStageInstanceService.updateProgramStageInstance( programStageInstance );

            // send SMS
            if ( programStageInstance.getProgramInstance().getEntityInstance().getTrackedEntityAttributeValues() != null
                && lostEvent.getSMS() != null )
            {
                List<User> recipientsList = new ArrayList<>();
                for ( TrackedEntityAttributeValue attrValue : programStageInstance.getProgramInstance()
                    .getEntityInstance().getTrackedEntityAttributeValues() )
                {
                    if ( ValueType.PHONE_NUMBER == attrValue.getAttribute().getValueType() )
                    {
                        User user = new User();
                        user.setPhoneNumber( attrValue.getValue() );
                        recipientsList.add( user );
                    }

                }

                Set<User> recipients = new HashSet<>();
                recipients.addAll( recipientsList );

                smsSender.sendMessage( lostEvent.getName(), lostEvent.getSMS(), null,
                    currentUserService.getCurrentUser(), recipients, false );
            }

            notification.setMessage( "Success" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            notification.setMessage( "Fail" );
        }
        finally
        {
            return notification;
        }
    }

    @Override
    public org.hisp.dhis.api.mobile.model.LWUITmodel.Patient generateRepeatableEvent( int orgUnitId, String eventInfo )
        throws NotAllowedException
    {
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitId );
        String[] keys = eventInfo.split( "_" );
        ProgramStage programStage = programStageService.getProgramStage( Integer.parseInt( keys[4] ) );
        int mobileProgramStageId = Integer.parseInt( keys[3] );
        String nextDueDate = keys[2];
        Program program = programService.getProgram( Integer.parseInt( keys[1] ) );
        TrackedEntityInstance trackedEntityInstance = entityInstanceService
            .getTrackedEntityInstance( Integer.parseInt( keys[0] ) );

        ProgramInstance programInstance = null;
        ProgramStageInstance newProgramStageInstance = null;
        if ( mobileProgramStageId != 0 )
        {
            ProgramStageInstance oldProgramStageIntance = programStageInstanceService
                .getProgramStageInstance( mobileProgramStageId );

            programInstance = oldProgramStageIntance.getProgramInstance();

            newProgramStageInstance = new ProgramStageInstance( programInstance,
                oldProgramStageIntance.getProgramStage() );

            newProgramStageInstance.setDueDate( DateUtils.getMediumDate( nextDueDate ) );
        }
        else
        {
            programInstance = programInstanceService.getProgramInstances( new ProgramInstanceQueryParams()
                .setTrackedEntityInstance( trackedEntityInstance )
                .setProgram( program )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL ) )
                .iterator().next();

            newProgramStageInstance = new ProgramStageInstance();
            newProgramStageInstance.setProgramInstance( programInstance );
            newProgramStageInstance.setProgramStage( programStage );
            newProgramStageInstance.setDueDate( DateUtils.getMediumDate( nextDueDate ) );
            newProgramStageInstance.setExecutionDate( DateUtils.getMediumDate( nextDueDate ) );
        }

        newProgramStageInstance.setOrganisationUnit( orgUnit );
        programInstance.getProgramStageInstances().add( newProgramStageInstance );

        List<ProgramStageInstance> proStageInstanceList = new ArrayList<>( programInstance.getProgramStageInstances() );

        Collections.sort( proStageInstanceList, new ProgramStageInstanceVisitDateComparator() );

        programInstance.getProgramStageInstances().removeAll( proStageInstanceList );
        programInstance.getProgramStageInstances().addAll( proStageInstanceList );

        programStageInstanceService.addProgramStageInstance( newProgramStageInstance );

        programInstanceService.updateProgramInstance( programInstance );

        TrackedEntityInstance tei = entityInstanceService
            .getTrackedEntityInstance( programInstance.getEntityInstance().getId() );
        org.hisp.dhis.api.mobile.model.LWUITmodel.Patient mobilePatient = getPatientModel( tei );

        return mobilePatient;
    }

    @Override
    public String saveSingleEventWithoutRegistration(
        org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStage mobileProgramStage, int orgUnitId )
            throws NotAllowedException
    {
        ProgramStage programStage = programStageService.getProgramStage( mobileProgramStage.getId() );

        Program program = programStage.getProgram();

        ProgramInstance programInstance = new ProgramInstance();

        programInstance.setEnrollmentDate( new Date() );

        programInstance.setIncidentDate( new Date() );

        programInstance.setProgram( program );

        programInstance.setStatus( ProgramStatus.COMPLETED );

        programInstanceService.addProgramInstance( programInstance );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();

        programStageInstance.setProgramInstance( programInstance );

        programStageInstance.setProgramStage( programStage );

        programStageInstance.setDueDate( new Date() );

        programStageInstance.setExecutionDate( new Date() );

        programStageInstance.setStatus( EventStatus.COMPLETED );

        programStageInstance.setOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );

        programStageInstanceService.addProgramStageInstance( programStageInstance );

        for ( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramStageDataElement mobileDataElement : mobileProgramStage
            .getDataElements() )
        {

            TrackedEntityDataValue trackedEntityDataValue = new TrackedEntityDataValue();

            trackedEntityDataValue.setDataElement( dataElementService.getDataElement( mobileDataElement.getId() ) );

            String value = mobileDataElement.getValue();

            if ( value != null && !value.trim().equals( "" ) )
            {

                trackedEntityDataValue.setValue( value );

                trackedEntityDataValue.setProgramStageInstance( programStageInstance );

                trackedEntityDataValue.setProvidedElsewhere( false );

                trackedEntityDataValue.setLastUpdated( new Date() );

                dataValueService.saveTrackedEntityDataValue( trackedEntityDataValue );
            }

        }
        return SINGLE_EVENT_WITHOUT_REGISTRATION_UPLOADED;
    }

    @Override
    public String sendFeedback( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {

        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        messageService.sendTicketMessage( subject, text, metaData );

        return FEEDBACK_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.User> findUser( String keyword )
        throws NotAllowedException
    {
        Collection<User> users = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.User> userList = new HashSet<>();

        if ( keyword != null )
        {
            int index = keyword.indexOf( ' ' );

            if ( index != -1 && index == keyword.lastIndexOf( ' ' ) )
            {
                String[] keys = keyword.split( " " );
                keyword = keys[0] + "  " + keys[1];
            }
        }

        UserQueryParams params = new UserQueryParams();
        params.setQuery( keyword );
        users = userService.getUsers( params );

        for ( User userCore : users )
        {

            org.hisp.dhis.api.mobile.model.User user = new org.hisp.dhis.api.mobile.model.User();
            user.setId( userCore.getId() );
            user.setSurname( userCore.getSurname() );
            user.setFirstName( userCore.getFirstName() );
            userList.add( user );

        }

        return userList;
    }

    @Override
    public String findVisitSchedule( int orgUnitId, int programId, String info )
        throws NotAllowedException
    {
        String status = info.substring( 0, info.indexOf( "$" ) );
        String fromDays = info.substring( info.indexOf( "$" ) + 1, info.indexOf( "/" ) );
        String toDays = info.substring( info.indexOf( "/" ) + 1 );

        // Event Status
        EventStatus eventStatus = null;

        if ( status.equals( "Schedule in future" ) )
        {
            eventStatus = EventStatus.SCHEDULE;
        }
        else if ( status.equals( "Overdue" ) )
        {
            eventStatus = EventStatus.OVERDUE;
        }
        else if ( status.equals( "Incomplete" ) )
        {
            eventStatus = EventStatus.VISITED;
        }
        else if ( status.equals( "Completed" ) )
        {
            eventStatus = EventStatus.COMPLETED;
        }
        else if ( status.equals( "Skipped" ) )
        {
            eventStatus = EventStatus.SKIPPED;
        }

        // From/To Date
        Date fromDate = getDate( -1, fromDays );
        Date toDate = getDate( 1, toDays );

        TrackedEntityInstanceQueryParams param = new TrackedEntityInstanceQueryParams();
        List<TrackedEntityAttribute> trackedEntityAttributeList = new ArrayList<>(
            attributeService.getTrackedEntityAttributesByDisplayOnVisitSchedule( true ) );

        for ( TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributeList )
        {
            QueryItem queryItem = new QueryItem( trackedEntityAttribute );
            param.addAttribute( queryItem );
        }

        param.setProgram( programService.getProgram( programId ) );
        param.addOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );
        param.setEventStatus( eventStatus );
        param.setEventStartDate( fromDate );
        param.setEventEndDate( toDate );

        Grid programStageInstanceGrid = entityInstanceService.getTrackedEntityInstancesGrid( param );
        List<List<Object>> listOfListProgramStageInstance = programStageInstanceGrid.getRows();

        if ( listOfListProgramStageInstance.size() == 0 )
        {
            throw NotAllowedException.NO_EVENT_FOUND;
        }

        String eventsInfo = "";
        for ( List<Object> row : listOfListProgramStageInstance )
        {
            TrackedEntityInstance instance = entityInstanceService.getTrackedEntityInstance( (String) row.get( 0 ) );
            Collection<TrackedEntityAttribute> displayAttributes = attributeService
                .getTrackedEntityAttributesDisplayInList();

            eventsInfo += instance.getId() + "/";
            String displayName = "";
            for ( TrackedEntityAttribute displayAttribute : displayAttributes )
            {
                TrackedEntityAttributeValue value = attValueService.getTrackedEntityAttributeValue( instance,
                    displayAttribute );
                if ( value != null )
                {
                    displayName += value.getValue() + " ";
                }
            }
            eventsInfo += displayName.trim() + "$";
        }

        return eventsInfo;
    }

    public Date getDate( int operation, String adjustment )
    {
        Calendar calendar = Calendar.getInstance();

        if ( adjustment.equals( "1 day" ) )
        {
            calendar.add( Calendar.DATE, operation );
        }
        else if ( adjustment.equals( "3 days" ) )
        {
            calendar.add( Calendar.DATE, operation * 3 );
        }
        else if ( adjustment.equals( "1 week" ) )
        {
            calendar.add( Calendar.DATE, operation * 7 );
        }
        else if ( adjustment.equals( "1 month" ) )
        {
            calendar.add( Calendar.DATE, operation * 30 );
        }
        return calendar.getTime();
    }

    @Override
    public String sendMessage( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {
        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        Set<User> users = new HashSet<>();

        for ( org.hisp.dhis.api.mobile.model.User user : message.getRecipient().getUserList() )
        {
            User userWeb = userService.getUser( user.getId() );
            users.add( userWeb );

        }

        messageService.sendPrivateMessage( subject, text, metaData, users );

        return MESSAGE_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.MessageConversation> downloadMessageConversation()
        throws NotAllowedException
    {
        Collection<MessageConversation> conversations = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.MessageConversation> mobileConversationList = new HashSet<>();

        conversations = new ArrayList<>( messageService.getMessageConversations( 0, 10 ) );

        for ( MessageConversation conversation : conversations )
        {
            if ( conversation.getLastSenderFirstname() != null )
            {
                org.hisp.dhis.api.mobile.model.MessageConversation messConversation = new org.hisp.dhis.api.mobile.model.MessageConversation();
                messConversation.setId( conversation.getId() );
                messConversation.setSubject( conversation.getSubject() );
                mobileConversationList.add( messConversation );
            }

        }

        return mobileConversationList;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.Message> getMessage( String conversationId )
        throws NotAllowedException
    {

        MessageConversation conversation = messageService.getMessageConversation( Integer.parseInt( conversationId ) );
        List<Message> messageList = new ArrayList<>( conversation.getMessages() );

        Collection<org.hisp.dhis.api.mobile.model.Message> messages = new HashSet<>();

        for ( Message message : messageList )
        {

            if ( message.getSender().getFirstName() != null )
            {

                org.hisp.dhis.api.mobile.model.Message messageMobile = new org.hisp.dhis.api.mobile.model.Message();
                messageMobile.setSubject( conversation.getSubject() );
                messageMobile.setText( message.getText() );
                messageMobile.setLastSenderName( message.getSender().getName() );
                messages.add( messageMobile );
            }
        }

        return messages;
    }

    @Override
    public String replyMessage( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {
        String metaData = MessageService.META_USER_AGENT;

        MessageConversation conversation = messageService
            .getMessageConversation( Integer.parseInt( message.getSubject() ) );

        messageService.sendReply( conversation, message.getText(), metaData, false );

        return MESSAGE_SENT;
    }

    @Override
    public Interpretation getInterpretation( String uId )
        throws NotAllowedException
    {
        Chart chart = chartService.getChart( uId );
        org.hisp.dhis.interpretation.Interpretation interpretationCore = interpretationService
            .getInterpretationByChart( chart.getId() );

        Collection<InterpretationComment> interComments = new HashSet<>();

        for ( org.hisp.dhis.interpretation.InterpretationComment interCommentsCore : interpretationCore.getComments() )
        {

            InterpretationComment interComment = new InterpretationComment();
            interComment.setText( interCommentsCore.getText() );
            interComments.add( interComment );
        }

        Interpretation interpretation = new Interpretation();
        interpretation.setId( interpretationCore.getId() );
        interpretation.setText( interpretationCore.getText() );
        interpretation.setInComments( interComments );

        return interpretation;
    }

    private org.hisp.dhis.interpretation.Interpretation interpretation;

    public void setInterpretation( org.hisp.dhis.interpretation.Interpretation interpretation )
    {
        this.interpretation = interpretation;
    }

    public org.hisp.dhis.interpretation.Interpretation getInterpretation()
    {
        return interpretation;
    }

    @Override
    public String postInterpretation( String data )
        throws NotAllowedException
    {

        String uId = data.substring( 0, 11 );

        String interpretation = data.substring( 11, data.length() - 0 );

        Chart c = chartService.getChart( uId );

        org.hisp.dhis.interpretation.Interpretation i = new org.hisp.dhis.interpretation.Interpretation( c, null,
            interpretation );

        i.setUser( currentUserService.getCurrentUser() );

        interpretationService.saveInterpretation( i );

        return INTERPRETATION_SENT;
    }

    @Override
    public String postInterpretationComment( String data )
        throws NotAllowedException
    {
        int interpretationId = Integer.parseInt( data.substring( 0, 7 ) );
        String comment = data.substring( 7, data.length() - 0 );

        setInterpretation( interpretationService.getInterpretation( interpretationId ) );
        interpretationService.addInterpretationComment( interpretation.getUid(), comment );

        return COMMENT_SENT;
    }

    @Override
    public String completeProgramInstance( int programId )
        throws NotAllowedException
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programId );
        programInstance.setStatus( ProgramStatus.COMPLETED );
        programInstanceService.updateProgramInstance( programInstance );

        return PROGRAM_COMPLETED;
    }

    public I18nManager getI18nManager()
    {
        return i18nManager;
    }

    public void setI18nManager( I18nManager i18nManager )
    {
        this.i18nManager = i18nManager;
    }

    public TrackedEntityService getTrackedEntityService()
    {
        return trackedEntityService;
    }

    public void setTrackedEntityService( TrackedEntityService trackedEntityService )
    {
        this.trackedEntityService = trackedEntityService;
    }

    @Override
    public Patient registerRelative( Patient patient, int orgUnitId, String programId )
        throws NotAllowedException
    {
        TrackedEntityInstance patientWeb = new TrackedEntityInstance();
        patientWeb.setOrganisationUnit( organisationUnitService.getOrganisationUnit( orgUnitId ) );

        Set<TrackedEntityAttribute> patientAttributeSet = new HashSet<>();
        Set<TrackedEntityAttributeValue> patientAttributeValues = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.PatientAttribute> attributesMobile = patient.getAttributes();

        if ( attributesMobile != null )
        {
            for ( org.hisp.dhis.api.mobile.model.PatientAttribute paAtt : attributesMobile )
            {

                TrackedEntityAttribute patientAttribute = attributeService
                    .getTrackedEntityAttributeByName( paAtt.getName() );

                patientAttributeSet.add( patientAttribute );

                TrackedEntityAttributeValue patientAttributeValue = new TrackedEntityAttributeValue();

                patientAttributeValue.setEntityInstance( patientWeb );
                patientAttributeValue.setAttribute( patientAttribute );
                patientAttributeValue.setValue( paAtt.getValue() );
                patientAttributeValues.add( patientAttributeValue );

            }
        }

        patientWeb.setTrackedEntity( trackedEntityService.getTrackedEntityByName( patient.getTrackedEntityName() ) );

        if ( patient.getIdToAddRelative() != 0 )
        {
            TrackedEntityInstance relative = entityInstanceService
                .getTrackedEntityInstance( patient.getIdToAddRelative() );
            if ( relative == null )
            {
                throw new NotAllowedException( "relative does not exist" );
            }

            patientId = entityInstanceService.createTrackedEntityInstance( patientWeb, relative.getUid(),
                patient.getRelTypeIdToAdd(), patientAttributeValues );
        }
        else
        {
            patientId = entityInstanceService.createTrackedEntityInstance( patientWeb, null, null,
                patientAttributeValues );
        }
        TrackedEntityInstance newTrackedEntityInstance = entityInstanceService
            .getTrackedEntityInstance( this.patientId );

        String errorMsg = null;

        try
        {
            for ( org.hisp.dhis.api.mobile.model.LWUITmodel.ProgramInstance mobileProgramInstance : patient
                .getEnrollmentPrograms() )
            {
                Date incidentDate = DateUtils.getMediumDate( mobileProgramInstance.getDateOfIncident() );
                enrollProgram( patientId + "-" + mobileProgramInstance.getProgramId(),
                    mobileProgramInstance.getProgramStageInstances(), incidentDate );
            }

            Program program = programService.getProgram( Integer.parseInt( programId ) );
            String[] errorCode = entityInstanceService
                .validateTrackedEntityInstance( newTrackedEntityInstance, program ).split( "_" );
            int code = Integer.parseInt( errorCode[0] );

            if ( code >= 1 )
            {
                entityInstanceService.deleteTrackedEntityInstance( newTrackedEntityInstance );
                if ( code == TrackedEntityInstanceService.ERROR_DUPLICATE_IDENTIFIER )
                {
                    errorMsg = "Duplicate value of " + attributeService
                        .getTrackedEntityAttribute( Integer.parseInt( errorCode[1] ) ).getDisplayName();
                }
                else
                {
                    errorMsg = "Validation error";
                }
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        if ( errorMsg != null )
        {
            throw new NotAllowedException( errorMsg );
        }

        if ( patient.getEnrollmentRelationship() != null )
        {
            org.hisp.dhis.api.mobile.model.LWUITmodel.Relationship enrollmentRelationship = patient
                .getEnrollmentRelationship();
            enrollmentRelationship.setPersonBId( newTrackedEntityInstance.getId() );
            addRelationship( enrollmentRelationship, orgUnitId );
        }

        return getPatientModel( newTrackedEntityInstance );
    }
}

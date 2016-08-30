package org.hisp.dhis.trackedentity.action.program;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw
 * @version $Id$
 */
public class AddProgramAction
    implements Action
{
    private static String REPORT_DATE_DESCRIPTION = "Report date";

    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataApprovalService dataApprovalService;

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }
    
    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String enrollmentDateLabel;

    public void setEnrollmentDateLabel( String enrollmentDateLabel )
    {
        this.enrollmentDateLabel = enrollmentDateLabel;
    }

    private String incidentDateLabel;

    public void setIncidentDateLabel( String incidentDateLabel )
    {
        this.incidentDateLabel = incidentDateLabel;
    }

    private ProgramType programType;

    public void setProgramType( ProgramType programType )
    {
        this.programType = programType;
    }

    private Boolean displayIncidentDate;

    public void setDisplayIncidentDate( Boolean displayIncidentDate )
    {
        this.displayIncidentDate = displayIncidentDate;
    }

    private List<String> selectedPropertyIds = new ArrayList<>();

    public void setSelectedPropertyIds( List<String> selectedPropertyIds )
    {
        this.selectedPropertyIds = selectedPropertyIds;
    }

    private List<Boolean> personDisplayNames = new ArrayList<>();

    public void setPersonDisplayNames( List<Boolean> personDisplayNames )
    {
        this.personDisplayNames = personDisplayNames;
    }

    private List<Boolean> allowFutureDate = new ArrayList<>();

    public void setAllowFutureDate( List<Boolean> allowFutureDate )
    {
        this.allowFutureDate = allowFutureDate;
    }

    private List<Boolean> mandatory = new ArrayList<>();

    public void setMandatory( List<Boolean> mandatory )
    {
        this.mandatory = mandatory;
    }

    private Boolean ignoreOverdueEvents;

    public void setIgnoreOverdueEvents( Boolean ignoreOverdueEvents )
    {
        this.ignoreOverdueEvents = ignoreOverdueEvents;
    }

    private Boolean onlyEnrollOnce = false;

    public void setOnlyEnrollOnce( Boolean onlyEnrollOnce )
    {
        this.onlyEnrollOnce = onlyEnrollOnce;
    }

    private Boolean selectEnrollmentDatesInFuture;

    public void setSelectEnrollmentDatesInFuture( Boolean selectEnrollmentDatesInFuture )
    {
        this.selectEnrollmentDatesInFuture = selectEnrollmentDatesInFuture;
    }

    private Boolean selectIncidentDatesInFuture;

    public void setSelectIncidentDatesInFuture( Boolean selectIncidentDatesInFuture )
    {
        this.selectIncidentDatesInFuture = selectIncidentDatesInFuture;
    }

    private String relationshipText;

    public void setRelationshipText( String relationshipText )
    {
        this.relationshipText = relationshipText;
    }

    private Integer relationshipTypeId;

    public void setRelationshipTypeId( Integer relationshipTypeId )
    {
        this.relationshipTypeId = relationshipTypeId;
    }

    private Integer relatedProgramId;

    public void setRelatedProgramId( Integer relatedProgramId )
    {
        this.relatedProgramId = relatedProgramId;
    }

    private Boolean relationshipFromA;

    public void setRelationshipFromA( Boolean relationshipFromA )
    {
        this.relationshipFromA = relationshipFromA;
    }

    private Boolean dataEntryMethod;

    public void setDataEntryMethod( Boolean dataEntryMethod )
    {
        this.dataEntryMethod = dataEntryMethod;
    }

    private Integer trackedEntityId;

    public void setTrackedEntityId( Integer trackedEntityId )
    {
        this.trackedEntityId = trackedEntityId;
    }

    private List<String> jsonAttributeValues = new ArrayList<>();

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    private Integer categoryComboId;

    public void setCategoryComboId( Integer categoryComboId )
    {
        this.categoryComboId = categoryComboId;
    }

    private boolean skipOffline;

    public void setSkipOffline( boolean skipOffline )
    {
        this.skipOffline = skipOffline;
    }

    private Integer workflowId;

    public void setWorkflowId( Integer workflowId )
    {
        this.workflowId = workflowId;
    }
    
    private boolean displayFrontPageList;

    public void setDisplayFrontPageList( boolean displayFrontPageList )
    {
        this.displayFrontPageList = displayFrontPageList;
    }
    
    private boolean useFirstStageDuringRegistration;

    public void setUseFirstStageDuringRegistration( boolean useFirstStageDuringRegistration )
    {
        this.useFirstStageDuringRegistration = useFirstStageDuringRegistration;
    }
    
    private boolean captureCoordinates;

    public void setCaptureCoordinates( boolean captureCoordinates )
    {
        this.captureCoordinates = captureCoordinates;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        displayIncidentDate = (displayIncidentDate == null) ? false : displayIncidentDate;
        ignoreOverdueEvents = (ignoreOverdueEvents == null) ? false : ignoreOverdueEvents;
        onlyEnrollOnce = (onlyEnrollOnce == null) ? false : onlyEnrollOnce;
        selectEnrollmentDatesInFuture = (selectEnrollmentDatesInFuture == null) ? false : selectEnrollmentDatesInFuture;
        selectIncidentDatesInFuture = (selectIncidentDatesInFuture == null) ? false : selectIncidentDatesInFuture;
        dataEntryMethod = (dataEntryMethod == null) ? false : dataEntryMethod;

        Program program = new Program();

        program.setName( StringUtils.trimToNull( name ) );
        program.setShortName( StringUtils.trimToNull( shortName ) );
        program.setDescription( StringUtils.trimToNull( description ) );
        program.setVersion( 1 );
        program.setEnrollmentDateLabel( StringUtils.trimToNull( enrollmentDateLabel ) );
        program.setIncidentDateLabel( StringUtils.trimToNull( incidentDateLabel ) );
        program.setProgramType( programType );
        program.setDisplayIncidentDate( displayIncidentDate );
        program.setOnlyEnrollOnce( onlyEnrollOnce );
        program.setSelectEnrollmentDatesInFuture( selectEnrollmentDatesInFuture );
        program.setSelectIncidentDatesInFuture( selectIncidentDatesInFuture );
        program.setDataEntryMethod( dataEntryMethod );
        program.setSkipOffline( skipOffline );
        program.setDisplayFrontPageList( displayFrontPageList );
        program.setUseFirstStageDuringRegistration( useFirstStageDuringRegistration );
        program.setCaptureCoordinates( captureCoordinates );

        if ( programType == ProgramType.WITH_REGISTRATION )
        {
            program.setIgnoreOverdueEvents( ignoreOverdueEvents );
        }
        else
        {
            program.setIgnoreOverdueEvents( false );
        }

        if ( relationshipTypeId != null )
        {
            RelationshipType relationshipType = relationshipTypeService.getRelationshipType( relationshipTypeId );
            program.setRelationshipType( relationshipType );
            program.setRelationshipFromA( relationshipFromA );
            program.setRelationshipText( relationshipText );

            Program relatedProgram = programService.getProgram( relatedProgramId );
            program.setRelatedProgram( relatedProgram );
        }
        else
        {
            program.setRelationshipType( null );
            program.setRelationshipFromA( null );
            program.setRelationshipText( null );
            program.setRelatedProgram( null );
        }

        if ( trackedEntityId != null )
        {
            TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity( trackedEntityId );
            program.setTrackedEntity( trackedEntity );
        }

        if ( categoryComboId != null )
        {
            program.setCategoryCombo( categoryService.getDataElementCategoryCombo( categoryComboId ) );
        }

        if ( workflowId != null && workflowId > 0 )
        {
            program.setWorkflow( dataApprovalService.getWorkflow( workflowId ) );
        }

        programService.addProgram( program );

        int index = 0;
        for ( String selectedPropertyId : selectedPropertyIds )
        {
            String[] ids = selectedPropertyId.split( "_" );

            if ( ids[0].equals( TrackedEntityInstance.PREFIX_TRACKED_ENTITY_ATTRIBUTE ) )
            {
                TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute(
                    Integer.parseInt( ids[1] ) );

                ProgramTrackedEntityAttribute programAttribute = new ProgramTrackedEntityAttribute( program, attribute,
                    personDisplayNames.get( index ), mandatory.get( index ), allowFutureDate.get( index ) );
                programAttribute.setAutoFields();

                program.getProgramAttributes().add( programAttribute );
            }

            index++;
        }

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( program, jsonAttributeValues );
        }

        programService.updateProgram( program );

        if ( program.isWithoutRegistration() )
        {
            ProgramStage programStage = new ProgramStage();

            programStage.setName( name );
            programStage.setDescription( description );
            programStage.setProgram( program );
            programStage.setMinDaysFromStart( 0 );
            programStage.setAutoGenerateEvent( true );
            programStage.setExecutionDateLabel( REPORT_DATE_DESCRIPTION );

            programStageService.saveProgramStage( programStage );
        }

        // ---------------------------------------------------------------------
        // create program-instance for anonymous program
        // ---------------------------------------------------------------------

        if ( program.isWithoutRegistration() )
        {
            // Add a new program-instance
            ProgramInstance programInstance = new ProgramInstance();
            programInstance.setEnrollmentDate( new Date() );
            programInstance.setIncidentDate( new Date() );
            programInstance.setProgram( program );
            programInstance.setStatus( ProgramStatus.ACTIVE );

            programInstanceService.addProgramInstance( programInstance );
        }

        return SUCCESS;
    }
}
package org.hisp.dhis.trackedentity.action.programstage;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw
 * @modified Tran Thanh Tri
 */
public class AddProgramStageAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private ProgramStageDataElementService programStageDataElementService;

    public void setProgramStageDataElementService( ProgramStageDataElementService programStageDataElementService )
    {
        this.programStageDataElementService = programStageDataElementService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    @Autowired
    private AttributeService attributeService;

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private int id;

    public void setId( int id )
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private Integer minDaysFromStart;

    public void setMinDaysFromStart( Integer minDaysFromStart )
    {
        this.minDaysFromStart = minDaysFromStart;
    }

    private List<Integer> selectedDataElementsValidator = new ArrayList<>();

    public void setSelectedDataElementsValidator( List<Integer> selectedDataElementsValidator )
    {
        this.selectedDataElementsValidator = selectedDataElementsValidator;
    }

    private List<Boolean> compulsories = new ArrayList<>();

    public void setCompulsories( List<Boolean> compulsories )
    {
        this.compulsories = compulsories;
    }

    private List<Boolean> allowProvidedElsewhere = new ArrayList<>();

    public void setAllowProvidedElsewhere( List<Boolean> allowProvidedElsewhere )
    {
        this.allowProvidedElsewhere = allowProvidedElsewhere;
    }

    private Boolean repeatable;

    public void setRepeatable( Boolean repeatable )
    {
        this.repeatable = repeatable;
    }

    private Integer standardInterval;

    public void setStandardInterval( Integer standardInterval )
    {
        this.standardInterval = standardInterval;
    }

    private String excecutionDateLabel;

    public void setExcecutionDateLabel( String excecutionDateLabel )
    {
        this.excecutionDateLabel = excecutionDateLabel;
    }
    
    private String dueDateLabel;

    public void setDueDateLabel( String dueDateLabel )
    {
        this.dueDateLabel = dueDateLabel;
    }

    private Boolean autoGenerateEvent;

    public void setAutoGenerateEvent( Boolean autoGenerateEvent )
    {
        this.autoGenerateEvent = autoGenerateEvent;
    }

    private List<Boolean> displayInReports = new ArrayList<>();

    public void setDisplayInReports( List<Boolean> displayInReports )
    {
        this.displayInReports = displayInReports;
    }

    private Boolean validCompleteOnly;

    public void setValidCompleteOnly( Boolean validCompleteOnly )
    {
        this.validCompleteOnly = validCompleteOnly;
    }

    private Boolean displayGenerateEventBox;

    public void setDisplayGenerateEventBox( Boolean displayGenerateEventBox )
    {
        this.displayGenerateEventBox = displayGenerateEventBox;
    }

    private Boolean captureCoordinates;

    public void setCaptureCoordinates( Boolean captureCoordinates )
    {
        this.captureCoordinates = captureCoordinates;
    }

    private List<Boolean> allowFutureDates;

    public void setAllowFutureDates( List<Boolean> allowFutureDates )
    {
        this.allowFutureDates = allowFutureDates;
    }

    private Boolean relatedEntityInstance;

    public void setRelatedEntityInstance( Boolean relatedEntityInstance )
    {
        this.relatedEntityInstance = relatedEntityInstance;
    }

    private Boolean generatedByEnrollmentDate;

    public void setGeneratedByEnrollmentDate( Boolean generatedByEnrollmentDate )
    {
        this.generatedByEnrollmentDate = generatedByEnrollmentDate;
    }

    private Boolean blockEntryForm;

    public void setBlockEntryForm( Boolean blockEntryForm )
    {
        this.blockEntryForm = blockEntryForm;
    }

    private Boolean remindCompleted = false;

    public void setRemindCompleted( Boolean remindCompleted )
    {
        this.remindCompleted = remindCompleted;
    }

    private Boolean allowGenerateNextVisit;

    public void setAllowGenerateNextVisit( Boolean allowGenerateNextVisit )
    {
        this.allowGenerateNextVisit = allowGenerateNextVisit;
    }

    private Boolean openAfterEnrollment;

    public void setOpenAfterEnrollment( Boolean openAfterEnrollment )
    {
        this.openAfterEnrollment = openAfterEnrollment;
    }

    private String reportDateToUse;

    public void setReportDateToUse( String reportDateToUse )
    {
        this.reportDateToUse = reportDateToUse;
    }

    private Boolean preGenerateUID;

    public void setPreGenerateUID( Boolean preGenerateUID )
    {
        this.preGenerateUID = preGenerateUID;
    }

    private String periodTypeName;

    public void setPeriodTypeName( String periodTypeName )
    {
        this.periodTypeName = periodTypeName;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }
    
    private Boolean hideDueDate;

    public void setHideDueDate( Boolean hideDueDate )
    {
        this.hideDueDate = hideDueDate;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        minDaysFromStart = (minDaysFromStart == null) ? 0 : minDaysFromStart;
        repeatable = (repeatable == null) ? false : repeatable;
        autoGenerateEvent = (autoGenerateEvent == null) ? false : autoGenerateEvent;
        validCompleteOnly = (validCompleteOnly == null) ? false : validCompleteOnly;
        displayGenerateEventBox = (displayGenerateEventBox == null) ? false : displayGenerateEventBox;
        captureCoordinates = (captureCoordinates == null) ? false : captureCoordinates;
        relatedEntityInstance = (relatedEntityInstance == null) ? false : relatedEntityInstance;
        generatedByEnrollmentDate = (generatedByEnrollmentDate == null) ? false : generatedByEnrollmentDate;
        blockEntryForm = (blockEntryForm == null) ? false : blockEntryForm;
        remindCompleted = (remindCompleted == null) ? false : remindCompleted;
        allowGenerateNextVisit = (allowGenerateNextVisit == null) ? false : allowGenerateNextVisit;
        openAfterEnrollment = (openAfterEnrollment == null) ? false : openAfterEnrollment;
        preGenerateUID = (preGenerateUID == null) ? false : preGenerateUID;
        hideDueDate = (hideDueDate == null) ? false : hideDueDate;

        ProgramStage programStage = new ProgramStage();
        Program program = programService.getProgram( id );

        programStage.setName( StringUtils.trimToNull( name ) );
        programStage.setDescription( StringUtils.trimToNull( description ) );
        programStage.setProgram( program );
        programStage.setStandardInterval( standardInterval );
        programStage.setExecutionDateLabel( StringUtils.trimToNull( excecutionDateLabel ) );
        programStage.setRepeatable( repeatable );
        programStage.setMinDaysFromStart( minDaysFromStart );
        programStage.setDisplayGenerateEventBox( displayGenerateEventBox );
        programStage.setValidCompleteOnly( validCompleteOnly );
        
        if( !hideDueDate )
        {
            programStage.setDueDateLabel( StringUtils.trimToNull( dueDateLabel ) );
        }

        periodTypeName = StringUtils.trimToNull( periodTypeName );

        if ( periodTypeName != null )
        {
            PeriodType periodType = PeriodType.getPeriodTypeByName( periodTypeName );
            programStage.setPeriodType( periodService.getPeriodTypeByClass( periodType.getClass() ) );
        }
        else
        {
            programStage.setPeriodType( null );
        }

        if ( program.isWithoutRegistration() )
        {
            programStage.setAutoGenerateEvent( true );
        }
        else
        {
            programStage.setAutoGenerateEvent( autoGenerateEvent );
        }

        programStage.setCaptureCoordinates( captureCoordinates );
        programStage.setBlockEntryForm( blockEntryForm );
        programStage.setRemindCompleted( remindCompleted );
        programStage.setGeneratedByEnrollmentDate( generatedByEnrollmentDate );
        programStage.setAllowGenerateNextVisit( allowGenerateNextVisit );
        programStage.setOpenAfterEnrollment( openAfterEnrollment );
        programStage.setReportDateToUse( reportDateToUse );
        programStage.setPreGenerateUID( preGenerateUID );
        programStage.setSortOrder( program.getProgramStages().size() + 1 );
        programStage.setHideDueDate( hideDueDate );

        program.getProgramStages().add( programStage );

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( programStage, jsonAttributeValues );
        }

        programStageService.saveProgramStage( programStage );

        // Data elements

        for ( int i = 0; i < this.selectedDataElementsValidator.size(); i++ )
        {
            DataElement dataElement = dataElementService.getDataElement( selectedDataElementsValidator.get( i ) );
            Boolean allowed = allowProvidedElsewhere.get( i ) == null ? false : allowProvidedElsewhere.get( i );
            Boolean displayInReport = displayInReports.get( i ) == null ? false : displayInReports.get( i );
            Boolean allowDate = allowFutureDates.get( i ) == null ? false : allowFutureDates.get( i );

            ProgramStageDataElement programStageDataElement = new ProgramStageDataElement( programStage, dataElement,
                this.compulsories.get( i ), new Integer( i ) );
            programStageDataElement.setAllowProvidedElsewhere( allowed );
            programStageDataElement.setDisplayInReports( displayInReport );
            programStageDataElement.setAllowFutureDate( allowDate );
            programStageDataElementService.addProgramStageDataElement( programStageDataElement );
        }

        return SUCCESS;
    }
}
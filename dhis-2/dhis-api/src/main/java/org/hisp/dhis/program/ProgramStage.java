package org.hisp.dhis.program;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStage
    extends BaseIdentifiableObject
{
    private String description;

    private int minDaysFromStart;

    private boolean repeatable;

    private Program program;

    private Set<ProgramStageDataElement> programStageDataElements = new HashSet<>();

    private Set<ProgramStageSection> programStageSections = new HashSet<>();

    private DataEntryForm dataEntryForm;

    private Integer standardInterval;

    private String executionDateLabel;

    private Set<ProgramNotificationTemplate> notificationTemplates = new HashSet<>();

    private Boolean autoGenerateEvent = true;

    private Boolean validCompleteOnly = false;

    private Boolean displayGenerateEventBox = true;

    private Boolean captureCoordinates = false;

    private Boolean blockEntryForm = false;

    private Boolean preGenerateUID = false;

    /**
     * Enabled this property to show a pop-up for confirming Complete a program
     * after to complete a program-stage
     */
    private Boolean remindCompleted = false;

    private Boolean generatedByEnrollmentDate = false;

    private Boolean allowGenerateNextVisit = false;

    private Boolean openAfterEnrollment = false;

    private String reportDateToUse;

    private Integer sortOrder;

    private PeriodType periodType;

    private Boolean hideDueDate = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramStage()
    {

    }

    public ProgramStage( String name, Program program )
    {
        this.name = name;
        this.program = program;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public List<DataElement> getAllDataElements()
    {
        return programStageDataElements.stream()
            .filter( element -> element.getDataElement() != null )
            .map( ProgramStageDataElement::getDataElement ).collect( Collectors.toList() );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FormType getFormType()
    {
        if ( dataEntryForm != null )
        {
            return FormType.CUSTOM;
        }

        if ( programStageSections.size() > 0 )
        {
            return FormType.SECTION;
        }

        return FormType.DEFAULT;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getGeneratedByEnrollmentDate()
    {
        return generatedByEnrollmentDate;
    }

    public void setGeneratedByEnrollmentDate( Boolean generatedByEnrollmentDate )
    {
        this.generatedByEnrollmentDate = generatedByEnrollmentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getBlockEntryForm()
    {
        return blockEntryForm;
    }

    public void setBlockEntryForm( Boolean blockEntryForm )
    {
        this.blockEntryForm = blockEntryForm;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getRemindCompleted()
    {
        return remindCompleted;
    }

    public void setRemindCompleted( Boolean remindCompleted )
    {
        this.remindCompleted = remindCompleted;
    }

    @JsonProperty( "notificationTemplates" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "notificationTemplates", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "notificationTemplate", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramNotificationTemplate> getNotificationTemplates()
    {
        return notificationTemplates;
    }

    public void setNotificationTemplates( Set<ProgramNotificationTemplate> notificationTemplates )
    {
        this.notificationTemplates = notificationTemplates;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    public void setDataEntryForm( DataEntryForm dataEntryForm )
    {
        this.dataEntryForm = dataEntryForm;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty( "programStageSections" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programStageSections", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStageSection", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramStageSection> getProgramStageSections()
    {
        return programStageSections;
    }

    public void setProgramStageSections( Set<ProgramStageSection> programStageSections )
    {
        this.programStageSections = programStageSections;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getStandardInterval()
    {
        return standardInterval;
    }

    public void setStandardInterval( Integer standardInterval )
    {
        this.standardInterval = standardInterval;
    }

    @JsonProperty( "repeatable" )
    @JacksonXmlProperty( localName = "repeatable", namespace = DxfNamespaces.DXF_2_0 )
    public boolean getRepeatable()
    {
        return repeatable;
    }

    public void setRepeatable( boolean repeatable )
    {
        this.repeatable = repeatable;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getMinDaysFromStart()
    {
        return minDaysFromStart;
    }

    public void setMinDaysFromStart( int minDaysFromStart )
    {
        this.minDaysFromStart = minDaysFromStart;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programStageDataElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStageDataElement", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramStageDataElement> getProgramStageDataElements()
    {
        return programStageDataElements;
    }

    public void setProgramStageDataElements( Set<ProgramStageDataElement> programStageDataElements )
    {
        this.programStageDataElements = programStageDataElements;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getExecutionDateLabel()
    {
        return executionDateLabel;
    }

    public void setExecutionDateLabel( String executionDateLabel )
    {
        this.executionDateLabel = executionDateLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getAutoGenerateEvent()
    {
        return autoGenerateEvent;
    }

    public void setAutoGenerateEvent( Boolean autoGenerateEvent )
    {
        this.autoGenerateEvent = autoGenerateEvent;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getValidCompleteOnly()
    {
        return validCompleteOnly;
    }

    public void setValidCompleteOnly( Boolean validCompleteOnly )
    {
        this.validCompleteOnly = validCompleteOnly;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayGenerateEventBox()
    {
        return displayGenerateEventBox;
    }

    public void setDisplayGenerateEventBox( Boolean displayGenerateEventBox )
    {
        this.displayGenerateEventBox = displayGenerateEventBox;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getCaptureCoordinates()
    {
        return captureCoordinates;
    }

    public void setCaptureCoordinates( Boolean captureCoordinates )
    {
        this.captureCoordinates = captureCoordinates;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getAllowGenerateNextVisit()
    {
        return allowGenerateNextVisit;
    }

    public void setAllowGenerateNextVisit( Boolean allowGenerateNextVisit )
    {
        this.allowGenerateNextVisit = allowGenerateNextVisit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getOpenAfterEnrollment()
    {
        return openAfterEnrollment;
    }

    public void setOpenAfterEnrollment( Boolean openAfterEnrollment )
    {
        this.openAfterEnrollment = openAfterEnrollment;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getReportDateToUse()
    {
        return reportDateToUse;
    }

    public void setReportDateToUse( String reportDateToUse )
    {
        this.reportDateToUse = reportDateToUse;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getPreGenerateUID()
    {
        return preGenerateUID;
    }

    public void setPreGenerateUID( Boolean preGenerateUID )
    {
        this.preGenerateUID = preGenerateUID;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( Integer sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JsonSerialize( using = JacksonPeriodTypeSerializer.class )
    @JsonDeserialize( using = JacksonPeriodTypeDeserializer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.TEXT )
    public PeriodType getPeriodType()
    {
        return periodType;
    }

    public void setPeriodType( PeriodType periodType )
    {
        this.periodType = periodType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getHideDueDate()
    {
        return hideDueDate;
    }

    public void setHideDueDate( Boolean hideDueDate )
    {
        this.hideDueDate = hideDueDate;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            ProgramStage programStage = (ProgramStage) other;

            minDaysFromStart = programStage.getMinDaysFromStart();
            autoGenerateEvent = programStage.isAutoGenerated();
            repeatable = programStage.getRepeatable();

            if ( mergeMode.isReplace() )
            {
                description = programStage.getDescription();
                repeatable = programStage.getRepeatable();
                program = programStage.getProgram();
                dataEntryForm = programStage.getDataEntryForm();
                standardInterval = programStage.getStandardInterval();
                executionDateLabel = programStage.getExecutionDateLabel();
                validCompleteOnly = programStage.getValidCompleteOnly();
                displayGenerateEventBox = programStage.getDisplayGenerateEventBox();
                captureCoordinates = programStage.getCaptureCoordinates();
                blockEntryForm = programStage.getBlockEntryForm();
                remindCompleted = programStage.getRemindCompleted();
                generatedByEnrollmentDate = programStage.getGeneratedByEnrollmentDate();
                allowGenerateNextVisit = programStage.getAllowGenerateNextVisit();
                openAfterEnrollment = programStage.getOpenAfterEnrollment();
                reportDateToUse = programStage.getReportDateToUse();
                preGenerateUID = programStage.getPreGenerateUID();
                hideDueDate = programStage.getHideDueDate();
            }
            else if ( mergeMode.isMerge() )
            {
                description = programStage.getDescription() == null ? description : programStage.getDescription();
                program = programStage.getProgram() == null ? program : programStage.getProgram();
                dataEntryForm = programStage.getDataEntryForm() == null ? dataEntryForm : programStage
                    .getDataEntryForm();
                standardInterval = programStage.getStandardInterval() == null ? standardInterval : programStage
                    .getStandardInterval();
                executionDateLabel = programStage.getExecutionDateLabel() == null ? executionDateLabel
                    : programStage.getExecutionDateLabel();
                validCompleteOnly = programStage.getValidCompleteOnly() == null ? validCompleteOnly : programStage
                    .getValidCompleteOnly();
                displayGenerateEventBox = programStage.getDisplayGenerateEventBox() == null ? displayGenerateEventBox
                    : programStage.getDisplayGenerateEventBox();
                captureCoordinates = programStage.getCaptureCoordinates() == null ? captureCoordinates : programStage
                    .getCaptureCoordinates();
                blockEntryForm = programStage.getBlockEntryForm() == null ? blockEntryForm : programStage
                    .getBlockEntryForm();
                remindCompleted = programStage.getRemindCompleted() == null ? remindCompleted : programStage
                    .getRemindCompleted();
                generatedByEnrollmentDate = programStage.getGeneratedByEnrollmentDate() == null ? generatedByEnrollmentDate
                    : programStage.getGeneratedByEnrollmentDate();
                allowGenerateNextVisit = programStage.getAllowGenerateNextVisit() == null ? allowGenerateNextVisit
                    : programStage.getAllowGenerateNextVisit();
                openAfterEnrollment = programStage.getOpenAfterEnrollment() == null ? openAfterEnrollment
                    : programStage.getOpenAfterEnrollment();
                reportDateToUse = programStage.getReportDateToUse() == null ? reportDateToUse : programStage
                    .getReportDateToUse();
                preGenerateUID = programStage.getPreGenerateUID() == null ? preGenerateUID : programStage
                    .getPreGenerateUID();
                hideDueDate = programStage.getHideDueDate() == null ? hideDueDate : programStage.getHideDueDate();
            }

            programStageDataElements.clear();

            for ( ProgramStageDataElement programStageDataElement : programStage.getProgramStageDataElements() )
            {
                programStageDataElements.add( programStageDataElement );
                programStageDataElement.setProgramStage( this );
            }

            programStageSections.clear();

            for ( ProgramStageSection programStageSection : programStage.getProgramStageSections() )
            {
                programStageSections.add( programStageSection );
                programStageSection.setProgramStage( this );
            }

            notificationTemplates.clear();
            notificationTemplates.addAll( programStage.getNotificationTemplates() );
        }
    }
}

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
package org.hisp.dhis.program;

import static org.hisp.dhis.util.ObjectUtils.newSetFromObjectOrEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.util.StreamUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStage
    extends BaseNameableObject
    implements MetadataObject
{
    private String description;

    /**
     * The i18n variant of the description. Should not be persisted.
     */
    protected transient String displayDescription;

    private String formName;

    private int minDaysFromStart;

    private boolean repeatable;

    private Program program;

    private Set<ProgramStageDataElement> programStageDataElements = new HashSet<>();

    private Set<ProgramStageSection> programStageSections = new HashSet<>();

    private DataEntryForm dataEntryForm;

    private Integer standardInterval;

    private String executionDateLabel;

    private String dueDateLabel;

    private Set<ProgramNotificationTemplate> notificationTemplates = new HashSet<>();

    private Boolean autoGenerateEvent = true;

    private ValidationStrategy validationStrategy = ValidationStrategy.ON_COMPLETE;

    private Boolean displayGenerateEventBox = true;

    private FeatureType featureType;

    private Boolean blockEntryForm = false;

    private Boolean preGenerateUID = false;

    private ObjectStyle style;

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

    private Boolean enableUserAssignment = false;

    private DataElement nextScheduleDate;

    private boolean referral;

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

    /**
     * Returns all data elements part of this program stage.
     */
    public Set<DataElement> getDataElements()
    {
        return programStageDataElements.stream()
            .map( ProgramStageDataElement::getDataElement )
            .filter( Objects::nonNull )
            .collect( Collectors.toSet() );
    }

    public boolean addDataElement( DataElement dataElement, Integer sortOrder )
    {
        ProgramStageDataElement element = new ProgramStageDataElement( this, dataElement, false, sortOrder );
        element.setAutoFields();

        return this.programStageDataElements.add( element );
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

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    @Override
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "executionDateLabel", key = "EXECUTION_DATE_LABEL" )
    public String getDisplayExecutionDateLabel()
    {
        return getTranslation( "EXECUTION_DATE_LABEL", getExecutionDateLabel() );
    }

    public void setExecutionDateLabel( String executionDateLabel )
    {
        this.executionDateLabel = executionDateLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDueDateLabel()
    {
        return dueDateLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "dueDateLabel", key = "DUE_DATE_LABEL" )
    public String getDisplayDueDateLabel()
    {
        return getTranslation( "DUE_DATE_LABEL", getDueDateLabel() );
    }

    public void setDueDateLabel( String dueDateLabel )
    {
        this.dueDateLabel = dueDateLabel;
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
    public ValidationStrategy getValidationStrategy()
    {
        return validationStrategy;
    }

    public void setValidationStrategy( ValidationStrategy validationStrategy )
    {
        this.validationStrategy = validationStrategy;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ObjectStyle getStyle()
    {
        return style;
    }

    public void setStyle( ObjectStyle style )
    {
        this.style = style;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFormName()
    {
        return formName;
    }

    @Override
    public void setFormName( String formName )
    {
        this.formName = formName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FeatureType getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType( FeatureType featureType )
    {
        this.featureType = featureType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isEnableUserAssignment()
    {
        return enableUserAssignment;
    }

    public void setEnableUserAssignment( Boolean enableUserAssignment )
    {
        this.enableUserAssignment = enableUserAssignment;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getNextScheduleDate()
    {
        return nextScheduleDate;
    }

    public void setNextScheduleDate( DataElement nextScheduleDate )
    {
        this.nextScheduleDate = nextScheduleDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isReferral()
    {
        return referral;
    }

    public void setReferral( boolean referral )
    {
        this.referral = referral;
    }

    public static ProgramStage copyOf( ProgramStage original, Program programCopy, Map<String, String> options )
    {
        ProgramStage copy = new ProgramStage();
        copy.setProgram( programCopy );
        setShallowCopyValues( copy, original, options );
        setDeepCopyValues( copy, original, options );
        return copy;
    }

    private static void setShallowCopyValues( ProgramStage copy, ProgramStage original, Map<String, String> options )
    {
        String prefix = options.getOrDefault( "prefix", "Copy of " );
        copy.setAllowGenerateNextVisit( original.getAllowGenerateNextVisit() );
        copy.setAutoFields();
        copy.setAutoGenerateEvent( original.getAutoGenerateEvent() );
        copy.setBlockEntryForm( original.getBlockEntryForm() );
        copy.setCode( CodeGenerator.generateCode( CodeGenerator.CODESIZE ) );
        copy.setDataEntryForm( original.getDataEntryForm() );
        copy.setDescription( original.getDescription() );
        copy.setDisplayGenerateEventBox( original.getDisplayGenerateEventBox() );
        copy.setDueDateLabel( original.getDueDateLabel() );
        copy.setEnableUserAssignment( original.isEnableUserAssignment() );
        copy.setExecutionDateLabel( original.getExecutionDateLabel() );
        copy.setFeatureType( original.getFeatureType() );
        copy.setFormName( original.getFormName() );
        copy.setGeneratedByEnrollmentDate( original.getGeneratedByEnrollmentDate() );
        copy.setHideDueDate( original.getHideDueDate() );
        //        copy.setLastUpdatedBy(); //TODO this is blank in DB when saved
        copy.setMinDaysFromStart( original.getMinDaysFromStart() );
        copy.setNextScheduleDate( original.getNextScheduleDate() );
        copy.setName( prefix + original.getName() );
        copy.setNotificationTemplates( newSetFromObjectOrEmpty( original.getNotificationTemplates() ) );
        copy.setOpenAfterEnrollment( original.getOpenAfterEnrollment() );
        copy.setPeriodType( original.getPeriodType() );
        copy.setPreGenerateUID( original.getPreGenerateUID() );
        copy.setReferral( original.isReferral() );
        copy.setRemindCompleted( original.getRemindCompleted() );
        copy.setRepeatable( original.getRepeatable() );
        copy.setReportDateToUse( original.getReportDateToUse() );
        copy.setSharing( original.getSharing() );
        copy.setShortName( original.getShortName() );
        copy.setSortOrder( original.getSortOrder() );
        copy.setStandardInterval( original.getStandardInterval() );
        copy.setStyle( original.getStyle() );
        copy.setValidationStrategy( original.getValidationStrategy() );
    }

    private static void setDeepCopyValues( ProgramStage copy, ProgramStage original, Map<String, String> options )
    {
        copyProgramStageDataElements( copy, original.getProgramStageDataElements(), options );
        copyProgramStageSections( copy, original.getProgramStageSections(), options );
    }

    private static void copyProgramStageDataElements( ProgramStage copy,
        Set<ProgramStageDataElement> original, Map<String, String> options )
    {
        copy.setProgramStageDataElements(
            StreamUtils.nullSafeCollectionToStream( original )
                .map( element -> ProgramStageDataElement.copyOf( element, copy ) )
                .collect( Collectors.toSet() ) );
    }

    private static void copyProgramStageSections( ProgramStage copy,
        Set<ProgramStageSection> original, Map<String, String> options )
    {
        copy.setProgramStageSections(
            StreamUtils.nullSafeCollectionToStream( original )
                .map( element -> ProgramStageSection.copyOf( element, copy ) )
                .collect( Collectors.toSet() ) );
    }
}

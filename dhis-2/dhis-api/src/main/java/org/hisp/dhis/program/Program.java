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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.validation.ValidationCriteria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "program", namespace = DxfNamespaces.DXF_2_0 )
public class Program
    extends BaseNameableObject
    implements VersionedObject
{
    private int version;

    private String enrollmentDateLabel;

    private String incidentDateLabel;

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<ProgramStage> programStages = new HashSet<>();

    private Set<ValidationCriteria> validationCriteria = new HashSet<>();

    private ProgramType programType;

    private Boolean displayIncidentDate = true;

    private Boolean ignoreOverdueEvents = false;

    private List<ProgramTrackedEntityAttribute> programAttributes = new ArrayList<>();

    private Set<UserAuthorityGroup> userRoles = new HashSet<>();

    private Set<ProgramIndicator> programIndicators = new HashSet<>();
    
    private Set<ProgramRule> programRules = new HashSet<>();
    
    private Set<ProgramRuleVariable> programRuleVariables = new HashSet<>();

    private Boolean onlyEnrollOnce = false;

    private Set<ProgramNotificationTemplate> notificationTemplates = new HashSet<>();

    private Boolean selectEnrollmentDatesInFuture = false;

    private Boolean selectIncidentDatesInFuture = false;

    private String relationshipText;

    private RelationshipType relationshipType;

    private Boolean relationshipFromA = false;

    private Program relatedProgram;

    private Boolean dataEntryMethod = false;

    private TrackedEntity trackedEntity;

    private DataEntryForm dataEntryForm;

    /**
     * The CategoryCombo used for data attributes.
     */
    private DataElementCategoryCombo categoryCombo;

    /**
     * Property indicating whether offline storage is enabled for this program
     * or not
     */
    private boolean skipOffline;

    /**
     * The approval workflow (if any) for this program.
     */
    private DataApprovalWorkflow workflow;

    private Boolean displayFrontPageList = false;

    /**
     * Property indicating whether first stage can appear for data entry on the
     * same page with registration
     */
    private Boolean useFirstStageDuringRegistration = false;
    
    /**
     * Property indicating whether program allows for capturing of coordinates
     */
    private Boolean captureCoordinates = false;
    
    /**
     * How many days after period is over will this program block creation and modification of events
     */
    private int expiryDays;
    
    /**
     * The PeriodType indicating the frequency that this program will use to decide on expiry
     */
    private PeriodType expiryPeriodType;
    
    /**
     * How many days after an event is completed will this program block modification of the event
     */
    private int completeEventsExpiryDays;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Program()
    {
    }

    public Program( String name, String description )
    {
        this.name = name;
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------

    public void addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.add( organisationUnit );
        organisationUnit.getPrograms().add( this );
    }

    public void removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.remove( organisationUnit );
        organisationUnit.getPrograms().remove( this );
    }

    public void updateOrganisationUnits( Set<OrganisationUnit> updates )
    {
        Set<OrganisationUnit> toRemove = Sets.difference( organisationUnits, updates );
        Set<OrganisationUnit> toAdd = Sets.difference( updates, organisationUnits );

        toRemove.stream().forEach( u -> u.getPrograms().remove( this ) );
        toAdd.stream().forEach( u -> u.getPrograms().add( this ) );

        organisationUnits.clear();
        organisationUnits.addAll( updates );
    }

    /**
     * Returns the ProgramTrackedEntityAttribute of this Program which contains
     * the given TrackedEntityAttribute.
     */
    public ProgramTrackedEntityAttribute getAttribute( TrackedEntityAttribute attribute )
    {
        for ( ProgramTrackedEntityAttribute programAttribute : programAttributes )
        {
            if ( programAttribute != null && programAttribute.getAttribute().equals( attribute ) )
            {
                return programAttribute;
            }
        }

        return null;
    }

    /**
     * Returns all data elements which are part of the stages of this program.
     */
    public Set<DataElement> getDataElements()
    {
        Set<DataElement> elements = new HashSet<>();

        for ( ProgramStage stage : programStages )
        {
            elements.addAll( stage.getAllDataElements() );
        }

        return elements;
    }

    /**
     * Returns data elements which are part of the stages of this program which
     * have a legend set and is of numeric value type.
     */
    public Set<DataElement> getDataElementsWithLegendSet()
    {
        return getDataElements().stream().filter( e -> e.hasLegendSet() && e.isNumericType() ).collect( Collectors.toSet() );
    }

    /**
     * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes. Use
     * getAttributes() to access the persisted attribute list.
     */
    public List<TrackedEntityAttribute> getTrackedEntityAttributes()
    {
        List<TrackedEntityAttribute> attributes = new ArrayList<>();

        for ( ProgramTrackedEntityAttribute attribute : programAttributes )
        {
            attributes.add( attribute.getAttribute() );
        }

        return attributes;
    }

    /**
     * Returns non-confidential TrackedEntityAttributes from ProgramTrackedEntityAttributes. Use
     * getAttributes() to access the persisted attribute list.
     */
    public List<TrackedEntityAttribute> getNonConfidentialTrackedEntityAttributes()
    {
        return getTrackedEntityAttributes().stream().filter( a -> !a.isConfidentialBool() ).collect( Collectors.toList() );
    }

    /**
     * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes which
     * have a legend set and is of numeric value type.
     */
    public List<TrackedEntityAttribute> getNonConfidentialTrackedEntityAttributesWithLegendSet()
    {
        return getTrackedEntityAttributes().stream().filter( a -> !a.isConfidentialBool() && a.hasLegendSet() && a.isNumericType() ).collect( Collectors.toList() );
    }

    public ProgramStage getProgramStageByStage( int stage )
    {
        int count = 1;

        for ( ProgramStage programStage : programStages )
        {
            if ( count == stage )
            {
                return programStage;
            }

            count++;
        }

        return null;
    }

    public boolean isSingleProgramStage()
    {
        return programStages != null && programStages.size() == 1;
    }

    public boolean hasOrganisationUnit( OrganisationUnit unit )
    {
        return organisationUnits.contains( unit );
    }

    @Override
    public int increaseVersion()
    {
        return ++version;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getVersion()
    {
        return version;
    }

    @Override
    public void setVersion( int version )
    {
        this.version = version;
    }

    @JsonProperty( "organisationUnits" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @JsonProperty( "programStages" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programStages", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramStage> getProgramStages()
    {
        return programStages;
    }

    public void setProgramStages( Set<ProgramStage> programStages )
    {
        this.programStages = programStages;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getEnrollmentDateLabel()
    {
        return enrollmentDateLabel;
    }

    public void setEnrollmentDateLabel( String enrollmentDateLabel )
    {
        this.enrollmentDateLabel = enrollmentDateLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getIncidentDateLabel()
    {
        return incidentDateLabel;
    }

    public void setIncidentDateLabel( String incidentDateLabel )
    {
        this.incidentDateLabel = incidentDateLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramType getProgramType()
    {
        return programType;
    }

    public void setProgramType( ProgramType programType )
    {
        this.programType = programType;
    }

    @JsonProperty( "validationCriterias" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "validationCriterias", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "validationCriteria", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValidationCriteria> getValidationCriteria()
    {
        return validationCriteria;
    }

    public void setValidationCriteria( Set<ValidationCriteria> validationCriteria )
    {
        this.validationCriteria = validationCriteria;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayIncidentDate()
    {
        return displayIncidentDate;
    }

    public void setDisplayIncidentDate( Boolean displayIncidentDate )
    {
        this.displayIncidentDate = displayIncidentDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getIgnoreOverdueEvents()
    {
        return ignoreOverdueEvents;
    }

    public void setIgnoreOverdueEvents( Boolean ignoreOverdueEvents )
    {
        this.ignoreOverdueEvents = ignoreOverdueEvents;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRegistration()
    {
        return programType == ProgramType.WITH_REGISTRATION;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWithoutRegistration()
    {
        return programType == ProgramType.WITHOUT_REGISTRATION;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "userRoles", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userRole", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserAuthorityGroup> getUserRoles()
    {
        return userRoles;
    }

    public void setUserRoles( Set<UserAuthorityGroup> userRoles )
    {
        this.userRoles = userRoles;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramIndicator> getProgramIndicators()
    {
        return programIndicators;
    }

    public void setProgramIndicators( Set<ProgramIndicator> programIndicators )
    {
        this.programIndicators = programIndicators;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programRules", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRule", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramRule> getProgramRules()
    {
        return programRules;
    }

    public void setProgramRules( Set<ProgramRule> programRules )
    {
        this.programRules = programRules;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programRuleVariables", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programRuleVariable", namespace = DxfNamespaces.DXF_2_0 )
    public Set<ProgramRuleVariable> getProgramRuleVariables()
    {
        return programRuleVariables;
    }

    public void setProgramRuleVariables( Set<ProgramRuleVariable> programRuleVariables )
    {
        this.programRuleVariables = programRuleVariables;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getOnlyEnrollOnce()
    {
        return onlyEnrollOnce;
    }

    public void setOnlyEnrollOnce( Boolean onlyEnrollOnce )
    {
        this.onlyEnrollOnce = onlyEnrollOnce;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "notificationTemplate", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlElementWrapper( localName = "notificationTemplates", namespace = DxfNamespaces.DXF_2_0 )
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
    public Boolean getSelectEnrollmentDatesInFuture()
    {
        return selectEnrollmentDatesInFuture;
    }

    public void setSelectEnrollmentDatesInFuture( Boolean selectEnrollmentDatesInFuture )
    {
        this.selectEnrollmentDatesInFuture = selectEnrollmentDatesInFuture;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getSelectIncidentDatesInFuture()
    {
        return selectIncidentDatesInFuture;
    }

    public void setSelectIncidentDatesInFuture( Boolean selectIncidentDatesInFuture )
    {
        this.selectIncidentDatesInFuture = selectIncidentDatesInFuture;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getRelationshipText()
    {
        return relationshipText;
    }

    public void setRelationshipText( String relationshipText )
    {
        this.relationshipText = relationshipText;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }

    public void setRelationshipType( RelationshipType relationshipType )
    {
        this.relationshipType = relationshipType;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getRelatedProgram()
    {
        return relatedProgram;
    }

    public void setRelatedProgram( Program relatedProgram )
    {
        this.relatedProgram = relatedProgram;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getRelationshipFromA()
    {
        return relationshipFromA;
    }

    public void setRelationshipFromA( Boolean relationshipFromA )
    {
        this.relationshipFromA = relationshipFromA;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDataEntryMethod()
    {
        return dataEntryMethod;
    }

    public void setDataEntryMethod( Boolean dataEntryMethod )
    {
        this.dataEntryMethod = dataEntryMethod;
    }

    @JsonProperty( "programTrackedEntityAttributes" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programTrackedEntityAttributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programTrackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramTrackedEntityAttribute> getProgramAttributes()
    {
        return programAttributes;
    }

    public void setProgramAttributes( List<ProgramTrackedEntityAttribute> programAttributes )
    {
        this.programAttributes = programAttributes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "trackedEntity", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntity", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0 )
    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    public void setDataEntryForm( DataEntryForm dataEntryForm )
    {
        this.dataEntryForm = dataEntryForm;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataApprovalWorkflow getWorkflow()
    {
        return workflow;
    }

    public void setWorkflow( DataApprovalWorkflow workflow )
    {
        this.workflow = workflow;
    }

    /**
     * Indicates whether this program has a category combination which is different
     * from the default category combination.
     */
    public boolean hasCategoryCombo()
    {
        return categoryCombo != null && !DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals( categoryCombo.getName() );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipOffline()
    {
        return skipOffline;
    }

    public void setSkipOffline( boolean skipOffline )
    {
        this.skipOffline = skipOffline;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayFrontPageList()
    {
        return displayFrontPageList;
    }

    public void setDisplayFrontPageList( Boolean displayFrontPageList )
    {
        this.displayFrontPageList = displayFrontPageList;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getUseFirstStageDuringRegistration()
    {
        return useFirstStageDuringRegistration;
    }

    public void setUseFirstStageDuringRegistration( Boolean useFirstStageDuringRegistration )
    {
        this.useFirstStageDuringRegistration = useFirstStageDuringRegistration;
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
    public int getExpiryDays() {
		return expiryDays;
	}

	public void setExpiryDays(int expiryDays) {
		this.expiryDays = expiryDays;
	}
	
	@JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
	public PeriodType getExpiryPeriodType() {
		return expiryPeriodType;
	}

	public void setExpiryPeriodType(PeriodType expiryPeriodType) {
		this.expiryPeriodType = expiryPeriodType;
	}

	@JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
	public int getCompleteEventsExpiryDays() {
		return completeEventsExpiryDays;
	}

	public void setCompleteEventsExpiryDays(int completeEventsExpiryDays) {
		this.completeEventsExpiryDays = completeEventsExpiryDays;
	}

	@Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Program program = (Program) other;

            version = program.getVersion();
            expiryDays = program.getExpiryDays();            
            completeEventsExpiryDays = program.getCompleteEventsExpiryDays();

            if ( mergeMode.isReplace() )
            {
                enrollmentDateLabel = program.getEnrollmentDateLabel();
                incidentDateLabel = program.getIncidentDateLabel();
                programType = program.getProgramType();
                displayIncidentDate = program.getDisplayIncidentDate();
                ignoreOverdueEvents = program.getIgnoreOverdueEvents();
                onlyEnrollOnce = program.getOnlyEnrollOnce();
                selectEnrollmentDatesInFuture = program.getSelectEnrollmentDatesInFuture();
                selectIncidentDatesInFuture = program.getSelectIncidentDatesInFuture();
                relationshipText = program.getRelationshipText();
                relationshipType = program.getRelationshipType();
                relationshipFromA = program.getRelationshipFromA();
                relatedProgram = program.getRelatedProgram();
                dataEntryMethod = program.getDataEntryMethod();
                trackedEntity = program.getTrackedEntity();
                useFirstStageDuringRegistration = program.getUseFirstStageDuringRegistration();
                categoryCombo = program.getCategoryCombo();
                captureCoordinates = program.getCaptureCoordinates();
                expiryPeriodType = program.getExpiryPeriodType();
            }
            else if ( mergeMode.isMerge() )
            {
                enrollmentDateLabel = program.getEnrollmentDateLabel() == null ? enrollmentDateLabel : program.getEnrollmentDateLabel();
                incidentDateLabel = program.getIncidentDateLabel() == null ? incidentDateLabel : program.getIncidentDateLabel();
                programType = program.getProgramType() == null ? programType : program.getProgramType();
                displayIncidentDate = program.getDisplayIncidentDate() == null ? displayIncidentDate : program.getDisplayIncidentDate();
                ignoreOverdueEvents = program.getIgnoreOverdueEvents() == null ? ignoreOverdueEvents : program.getIgnoreOverdueEvents();
                onlyEnrollOnce = program.getOnlyEnrollOnce() == null ? onlyEnrollOnce : program.getOnlyEnrollOnce();
                selectEnrollmentDatesInFuture = program.getSelectEnrollmentDatesInFuture() == null ? selectEnrollmentDatesInFuture : program.getSelectEnrollmentDatesInFuture();
                selectIncidentDatesInFuture = program.getSelectIncidentDatesInFuture() == null ? selectIncidentDatesInFuture : program.getSelectIncidentDatesInFuture();
                relationshipText = program.getRelationshipText() == null ? relationshipText : program.getRelationshipText();
                relationshipType = program.getRelationshipType() == null ? relationshipType : program.getRelationshipType();
                relationshipFromA = program.getRelationshipFromA() == null ? relationshipFromA : program.getRelationshipFromA();
                relatedProgram = program.getRelatedProgram() == null ? relatedProgram : program.getRelatedProgram();
                dataEntryMethod = program.getDataEntryMethod() == null ? dataEntryMethod : program.getDataEntryMethod();
                trackedEntity = program.getTrackedEntity() == null ? trackedEntity : program.getTrackedEntity();
                useFirstStageDuringRegistration = program.getUseFirstStageDuringRegistration() == null ? useFirstStageDuringRegistration : program.getUseFirstStageDuringRegistration();
                categoryCombo = program.getCategoryCombo() == null ? categoryCombo : program.getCategoryCombo();
                captureCoordinates = program.getCaptureCoordinates() == null ? captureCoordinates : program.getCaptureCoordinates();
                expiryPeriodType = program.getExpiryPeriodType() == null ? expiryPeriodType : program.getExpiryPeriodType();
            }

            organisationUnits.clear();
            organisationUnits.addAll( program.getOrganisationUnits() );

            programStages.clear();

            for ( ProgramStage programStage : program.getProgramStages() )
            {
                programStages.add( programStage );
                programStage.setProgram( this );
            }

            validationCriteria.clear();
            validationCriteria.addAll( program.getValidationCriteria() );

            programAttributes.clear();
            programAttributes.addAll( program.getProgramAttributes() );

            userRoles.clear();
            userRoles.addAll( program.getUserRoles() );

            notificationTemplates.clear();
            notificationTemplates.addAll( program.getNotificationTemplates() );
        }
    }
}

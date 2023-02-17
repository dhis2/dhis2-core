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
package org.hisp.dhis.dataset;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.InterpretableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * This class is used for defining the standardized DataSets. A DataSet consists
 * of a collection of DataElements.
 *
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
public class DataSet
    extends BaseDimensionalItemObject
    implements VersionedObject, MetadataObject, InterpretableObject
{
    public static final int NO_EXPIRY = 0;

    private String formName;

    /**
     * The PeriodType indicating the frequency that this DataSet should be used
     */
    private PeriodType periodType;

    /**
     * The dataInputPeriods is a set of periods with opening and closing dates,
     * which determines the period of which data can belong (period) and at
     * which dates (between opening and closing dates) actually registering this
     * data is allowed. The same period can exist at the same time with
     * different opening and closing dates to allow for multiple periods for
     * registering data.
     */
    private Set<DataInputPeriod> dataInputPeriods = new HashSet<>();

    /**
     * All DataElements associated with this DataSet.
     */
    private Set<DataSetElement> dataSetElements = new HashSet<>();

    /**
     * Indicators associated with this data set. Indicators are used for view
     * and output purposes, such as calculated fields in forms and reports.
     */
    private Set<Indicator> indicators = new HashSet<>();

    /**
     * The DataElementOperands for which data must be entered in order for the
     * DataSet to be considered as complete.
     */
    private Set<DataElementOperand> compulsoryDataElementOperands = new HashSet<>();

    /**
     * All Sources that register data with this DataSet.
     */
    private Set<OrganisationUnit> sources = new HashSet<>();

    /**
     * The Sections associated with the DataSet.
     */
    private Set<Section> sections = new HashSet<>();

    /**
     * The CategoryCombo used for data attributes.
     */
    private CategoryCombo categoryCombo;

    /**
     * Property indicating if the dataset could be collected using mobile data
     * entry.
     */
    private boolean mobile; // TODO Remove, mobile service is now removed

    /**
     * Indicating custom data entry form, can be null.
     */
    private DataEntryForm dataEntryForm;

    /**
     * Indicating version number.
     */
    private int version;

    /**
     * How many days after period is over will this dataSet auto-lock
     */
    private int expiryDays;

    /**
     * Days after period end to qualify for timely data submission
     */
    private int timelyDays;

    /**
     * User group which will receive notifications when data set is marked
     * complete, can be null.
     */
    private UserGroup notificationRecipients;

    /**
     * Indicating whether the user completing this data set should be sent a
     * notification.
     */
    private boolean notifyCompletingUser;

    /**
     * The approval workflow for this data set, can be null.
     */
    private DataApprovalWorkflow workflow;

    /**
     * Interpretations of this data set.
     */
    private Set<Interpretation> interpretations = new HashSet<>();

    // -------------------------------------------------------------------------
    // Form properties
    // -------------------------------------------------------------------------

    /**
     * Number of periods in the future to open for data capture, 0 means capture
     * not allowed for current period.
     */
    private int openFuturePeriods;

    /**
     * Number of periods to open for data capture that are after the category
     * option's end date.
     */
    private int openPeriodsAfterCoEndDate;

    /**
     * Property indicating that all fields for a data element must be filled.
     */
    private boolean fieldCombinationRequired;

    /**
     * Property indicating that all validation rules must pass before the form
     * can be completed.
     */
    private boolean validCompleteOnly;

    /**
     * Property indicating whether a comment is required for all fields in a
     * form which are not entered, including false for boolean values.
     */
    private boolean noValueRequiresComment;

    /**
     * Property indicating whether offline storage is enabled for this dataSet
     * or not
     */
    private boolean skipOffline;

    /**
     * Property indicating whether it should enable data elements decoration in
     * forms.
     */
    private boolean dataElementDecoration;

    /**
     * Render default and section forms with tabs instead of multiple sections
     * in one page
     */
    private boolean renderAsTabs;

    /**
     * Render multi-organisationUnit forms either with OU vertically or
     * horizontally.
     */
    private boolean renderHorizontally;

    /**
     * Property indicating whether all compulsory fields should be filled before
     * completing data set
     */
    private boolean compulsoryFieldsCompleteOnly;

    private ObjectStyle style;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataSet()
    {
    }

    public DataSet( String name )
    {
        this.name = name;
    }

    public DataSet( String name, PeriodType periodType )
    {
        this( name );
        this.periodType = periodType;
    }

    public DataSet( String name, String shortName, PeriodType periodType )
    {
        this( name, periodType );
        this.shortName = shortName;
    }

    public DataSet( String name, String shortName, String code, PeriodType periodType )
    {
        this( name, shortName, periodType );
        this.code = code;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        sources.add( organisationUnit );
        organisationUnit.getDataSets().add( this );
    }

    public void addOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::addOrganisationUnit );
    }

    public boolean removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        sources.remove( organisationUnit );
        return organisationUnit.getDataSets().remove( this );
    }

    public void removeOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::removeOrganisationUnit );
    }

    public void removeAllOrganisationUnits()
    {
        for ( OrganisationUnit unit : sources )
        {
            unit.getDataSets().remove( this );
        }

        sources.clear();
    }

    public void updateOrganisationUnits( Set<OrganisationUnit> updates )
    {
        Set<OrganisationUnit> toRemove = Sets.difference( sources, updates );
        Set<OrganisationUnit> toAdd = Sets.difference( updates, sources );

        toRemove.forEach( u -> u.getDataSets().remove( this ) );
        toAdd.forEach( u -> u.getDataSets().add( this ) );

        sources.clear();
        sources.addAll( updates );
    }

    public boolean hasOrganisationUnit( OrganisationUnit unit )
    {
        return sources.contains( unit );
    }

    public boolean addDataInputPeriod( DataInputPeriod dataInputPeriod )
    {
        return dataInputPeriods.add( dataInputPeriod );
    }

    public boolean addDataSetElement( DataSetElement element )
    {
        element.getDataElement().getDataSetElements().add( element );
        return dataSetElements.add( element );
    }

    /**
     * Adds a data set element using this data set, the given data element and
     * no category combo.
     *
     * @param dataElement the data element.
     */
    public boolean addDataSetElement( DataElement dataElement )
    {
        DataSetElement element = new DataSetElement( this, dataElement, null );
        dataElement.getDataSetElements().add( element );
        return dataSetElements.add( element );
    }

    /**
     * Adds a data set element using this data set, the given data element and
     * the given category combo.
     *
     * @param dataElement the data element.
     * @param categoryCombo the category combination.
     */
    public boolean addDataSetElement( DataElement dataElement, CategoryCombo categoryCombo )
    {
        DataSetElement element = new DataSetElement( this, dataElement, categoryCombo );
        dataElement.getDataSetElements().add( element );
        return dataSetElements.add( element );
    }

    public boolean removeDataSetElement( DataSetElement element )
    {
        dataSetElements.remove( element );
        return element.getDataElement().getDataSetElements().remove( element );
    }

    public void removeDataSetElement( DataElement dataElement )
    {
        Iterator<DataSetElement> elements = dataSetElements.iterator();

        while ( elements.hasNext() )
        {
            DataSetElement element = elements.next();

            DataSetElement other = new DataSetElement( this, dataElement );

            if ( element.objectEquals( other ) )
            {
                elements.remove();
                element.getDataElement().getDataSetElements().remove( element );
            }
        }
    }

    public void removeAllDataSetElements()
    {
        for ( DataSetElement element : dataSetElements )
        {
            element.getDataElement().getDataSetElements().remove( element );
        }

        dataSetElements.clear();
    }

    public void addIndicator( Indicator indicator )
    {
        indicators.add( indicator );
        indicator.getDataSets().add( this );
    }

    public boolean removeIndicator( Indicator indicator )
    {
        indicators.remove( indicator );
        return indicator.getDataSets().remove( this );
    }

    public void addCompulsoryDataElementOperand( DataElementOperand dataElementOperand )
    {
        compulsoryDataElementOperands.add( dataElementOperand );
    }

    public void removeCompulsoryDataElementOperand( DataElementOperand dataElementOperand )
    {
        compulsoryDataElementOperands.remove( dataElementOperand );
    }

    public void assignWorkflow( DataApprovalWorkflow workflow )
    {
        workflow.getDataSets().add( this );
        this.workflow = workflow;
    }

    public boolean hasDataEntryForm()
    {
        return dataEntryForm != null && dataEntryForm.hasForm();
    }

    public boolean hasSections()
    {
        return sections != null && sections.size() > 0;
    }

    /**
     * Indicates whether data should be approved for this data set, i.e. whether
     * this data set is part of a data approval workflow.
     */
    public boolean isApproveData()
    {
        return workflow != null;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FormType getFormType()
    {
        if ( hasDataEntryForm() )
        {
            return FormType.CUSTOM;
        }

        if ( hasSections() )
        {
            return FormType.SECTION;
        }

        return FormType.DEFAULT;
    }

    /**
     * Note that this method returns an immutable set and can not be used to
     * modify the model. Returns an immutable set of data sets associated with
     * this data element.
     */
    public Set<DataElement> getDataElements()
    {
        return ImmutableSet.copyOf(
            dataSetElements.stream().map( DataSetElement::getDataElement ).collect( Collectors.toSet() ) );
    }

    public Set<DataElement> getDataElementsInSections()
    {
        Set<DataElement> dataElements = new HashSet<>();

        for ( Section section : sections )
        {
            dataElements.addAll( section.getDataElements() );
        }

        return dataElements;
    }

    public Set<CategoryOptionCombo> getDataElementOptionCombos()
    {
        Set<CategoryOptionCombo> optionCombos = new HashSet<>();

        for ( DataSetElement element : dataSetElements )
        {
            optionCombos.addAll( element.getResolvedCategoryCombo().getOptionCombos() );
        }

        return optionCombos;
    }

    @Override
    public int increaseVersion()
    {
        return ++version;
    }

    /**
     * Returns a set of category option group sets which are linked to this data
     * set through its category combination.
     */
    public Set<CategoryOptionGroupSet> getCategoryOptionGroupSets()
    {
        Set<CategoryOptionGroupSet> groupSets = new HashSet<>();

        if ( categoryCombo != null )
        {
            for ( Category category : categoryCombo.getCategories() )
            {
                for ( CategoryOption categoryOption : category.getCategoryOptions() )
                {
                    groupSets.addAll( categoryOption.getGroupSets() );
                }
            }
        }

        return groupSets;
    }

    /**
     * Indicates whether this data set has a category combination which is
     * different from the default category combination.
     */
    public boolean hasCategoryCombo()
    {
        return categoryCombo != null && !CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals( categoryCombo.getName() );
    }

    /**
     * Indicates whether the data set is locked for data entry based on the
     * expiry days.
     *
     * @param period the period to compare with.
     * @param now the date indicating now, uses current date if null.
     */
    public boolean isLocked( User user, Period period, Date now )
    {
        if ( user != null && user.isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            return false;
        }

        Date date = now != null ? now : new Date();

        return expiryDays != DataSet.NO_EXPIRY
            && !Period.isDateInTimeFrame( null, new DateTime( period.getEndDate() ).plusDays( expiryDays ).toDate(),
                date );
    }

    /**
     * Checks if the given period and date combination conforms to any of the
     * dataInputPeriods. Returns true if no dataInputPeriods exists, or the
     * combination conforms to at least one dataInputPeriod.
     *
     * @param period
     * @param date
     * @return true if period and date conforms to a dataInputPeriod, or no
     *         dataInputPeriods exists.
     */
    public boolean isDataInputPeriodAndDateAllowed( Period period, Date date )
    {
        return dataInputPeriods.isEmpty() || dataInputPeriods.stream()
            .anyMatch( dataInputPeriod -> dataInputPeriod.isPeriodAndDateValid( period, date ) );
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.REPORTING_RATE;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

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
    @JacksonXmlElementWrapper( localName = "dataInputPeriods", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataInputPeriods", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataInputPeriod> getDataInputPeriods()
    {
        return dataInputPeriods;
    }

    public void setDataInputPeriods( Set<DataInputPeriod> dataInputPeriods )
    {
        this.dataInputPeriods = dataInputPeriods;
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
    @JacksonXmlElementWrapper( localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSetElement", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataSetElement> getDataSetElements()
    {
        return dataSetElements;
    }

    public void setDataSetElements( Set<DataSetElement> dataSetElements )
    {
        this.dataSetElements = dataSetElements;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "indicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicator", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Indicator> getIndicators()
    {
        return indicators;
    }

    public void setIndicators( Set<Indicator> indicators )
    {
        this.indicators = indicators;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "compulsoryDataElementOperands", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "compulsoryDataElementOperand", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementOperand> getCompulsoryDataElementOperands()
    {
        return compulsoryDataElementOperands;
    }

    public void setCompulsoryDataElementOperands( Set<DataElementOperand> compulsoryDataElementOperands )
    {
        this.compulsoryDataElementOperands = compulsoryDataElementOperands;
    }

    @JsonProperty( value = "organisationUnits" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getSources()
    {
        return sources;
    }

    public void setSources( Set<OrganisationUnit> sources )
    {
        this.sources = sources;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "sections", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "section", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Section> getSections()
    {
        return sections;
    }

    public void setSections( Set<Section> sections )
    {
        this.sections = sections;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public CategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( CategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMobile()
    {
        return mobile;
    }

    public void setMobile( boolean mobile )
    {
        this.mobile = mobile;
    }

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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = Integer.MIN_VALUE )
    public int getExpiryDays()
    {
        return expiryDays;
    }

    public void setExpiryDays( int expiryDays )
    {
        this.expiryDays = expiryDays;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getTimelyDays()
    {
        return timelyDays;
    }

    public void setTimelyDays( int timelyDays )
    {
        this.timelyDays = timelyDays;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getNotificationRecipients()
    {
        return notificationRecipients;
    }

    public void setNotificationRecipients( UserGroup notificationRecipients )
    {
        this.notificationRecipients = notificationRecipients;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isNotifyCompletingUser()
    {
        return notifyCompletingUser;
    }

    public void setNotifyCompletingUser( boolean notifyCompletingUser )
    {
        this.notifyCompletingUser = notifyCompletingUser;
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

    @Override
    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "interpretations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "interpretation", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Interpretation> getInterpretations()
    {
        return interpretations;
    }

    public void setInterpretations( Set<Interpretation> interpretations )
    {
        this.interpretations = interpretations;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getOpenFuturePeriods()
    {
        return openFuturePeriods;
    }

    public void setOpenFuturePeriods( int openFuturePeriods )
    {
        this.openFuturePeriods = openFuturePeriods;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getOpenPeriodsAfterCoEndDate()
    {
        return openPeriodsAfterCoEndDate;
    }

    public void setOpenPeriodsAfterCoEndDate( int openPeriodsAfterCoEndDate )
    {
        this.openPeriodsAfterCoEndDate = openPeriodsAfterCoEndDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isFieldCombinationRequired()
    {
        return fieldCombinationRequired;
    }

    public void setFieldCombinationRequired( boolean fieldCombinationRequired )
    {
        this.fieldCombinationRequired = fieldCombinationRequired;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isValidCompleteOnly()
    {
        return validCompleteOnly;
    }

    public void setValidCompleteOnly( boolean validCompleteOnly )
    {
        this.validCompleteOnly = validCompleteOnly;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isNoValueRequiresComment()
    {
        return noValueRequiresComment;
    }

    public void setNoValueRequiresComment( boolean noValueRequiresComment )
    {
        this.noValueRequiresComment = noValueRequiresComment;
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
    public boolean isRenderAsTabs()
    {
        return renderAsTabs;
    }

    public void setRenderAsTabs( boolean renderAsTabs )
    {
        this.renderAsTabs = renderAsTabs;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRenderHorizontally()
    {
        return renderHorizontally;
    }

    public void setRenderHorizontally( boolean renderHorizontally )
    {
        this.renderHorizontally = renderHorizontally;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDataElementDecoration()
    {
        return dataElementDecoration;
    }

    public void setDataElementDecoration( boolean dataElementDecoration )
    {
        this.dataElementDecoration = dataElementDecoration;
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
    public boolean isCompulsoryFieldsCompleteOnly()
    {
        return compulsoryFieldsCompleteOnly;
    }

    public void setCompulsoryFieldsCompleteOnly( boolean compulsoryFieldsCompleteOnly )
    {
        this.compulsoryFieldsCompleteOnly = compulsoryFieldsCompleteOnly;
    }

}

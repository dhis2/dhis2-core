package org.hisp.dhis.dataset;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Sets;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used for defining the standardized DataSets. A DataSet consists
 * of a collection of DataElements.
 *
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
public class DataSet
    extends BaseNameableObject
    implements VersionedObject
{
    public static final int NO_EXPIRY = 0;

    /**
     * The PeriodType indicating the frequency that this DataSet should be used
     */
    private PeriodType periodType;

    /**
     * All DataElements associated with this DataSet.
     */
    @Scanned
    private Set<DataElement> dataElements = new HashSet<>();

    /**
     * Indicators associated with this data set. Indicators are used for view
     * and output purposes, such as calculated fields in forms and reports.
     */
    @Scanned
    private Set<Indicator> indicators = new HashSet<>();

    /**
     * The DataElementOperands for which data must be entered in order for the
     * DataSet to be considered as complete.
     */
    private Set<DataElementOperand> compulsoryDataElementOperands = new HashSet<>();

    /**
     * All Sources that register data with this DataSet.
     */
    @Scanned
    private Set<OrganisationUnit> sources = new HashSet<>();

    /**
     * The Sections associated with the DataSet.
     */
    private Set<Section> sections = new HashSet<>();

    /**
     * The CategoryCombo used for data attributes.
     */
    private DataElementCategoryCombo categoryCombo;

    /**
     * Property indicating if the dataset could be collected using mobile data
     * entry.
     */
    private boolean mobile;

    /**
     * Indicating custom data entry form.
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
     * complete.
     */
    private UserGroup notificationRecipients;

    /**
     * Indicating whether the user completing this data set should be sent a
     * notification.
     */
    private boolean notifyCompletingUser;

    /**
     * Indicating whether to approve data for this data set.
     */
    private boolean approveData;

    /**
     * Set of the dynamic attributes values that belong to this data element.
     */
    private Set<AttributeValue> attributeValues = new HashSet<>();

    // -------------------------------------------------------------------------
    // Form properties
    // -------------------------------------------------------------------------

    /**
     * Number of periods in the future to open for data capture, 0 means capture
     * not allowed for current period.
     */
    private int openFuturePeriods;

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
     * Property indicating whether a comment is required for all fields in a form
     * which are not entered, including false for boolean values.
     */
    private boolean noValueRequiresComment;

    /**
     * Property indicating whether offline storage is enabled for this dataSet
     * or not
     */
    private boolean skipOffline;

    /**
     * Property indicating whether it should enable data elements decoration in forms.
     */
    private boolean dataElementDecoration;

    /**
     * Render default and section forms with tabs instead of multiple sections in one page
     */
    private boolean renderAsTabs;

    /**
     * Render multi-organisationUnit forms either with OU vertically or horizontally.
     */
    private boolean renderHorizontally;

    /**
     * The legend set for this indicator.
     */
    private LegendSet legendSet;

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

    public boolean removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        sources.remove( organisationUnit );
        return organisationUnit.getDataSets().remove( this );
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

        toRemove.stream().forEach( u -> u.getDataSets().remove( this ) );
        toAdd.stream().forEach( u -> u.getDataSets().add( this ) );

        sources.clear();
        sources.addAll( updates );
    }

    public void addDataElement( DataElement dataElement )
    {
        dataElements.add( dataElement );
        dataElement.getDataSets().add( this );
    }

    public boolean removeDataElement( DataElement dataElement )
    {
        dataElements.remove( dataElement );
        return dataElement.getDataSets().remove( dataElement );
    }

    public void updateDataElements( Set<DataElement> updates )
    {
        Set<DataElement> toRemove = Sets.difference( dataElements, updates );
        Set<DataElement> toAdd = Sets.difference( updates, dataElements );

        toRemove.stream().forEach( d -> d.getDataSets().remove( this ) );
        toAdd.stream().forEach( d -> d.getDataSets().add( this ) );

        dataElements.clear();
        dataElements.addAll( updates );
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

    public boolean hasDataEntryForm()
    {
        return dataEntryForm != null && dataEntryForm.hasForm();
    }

    public boolean hasSections()
    {
        return sections != null && sections.size() > 0;
    }


    @JsonProperty
    @JsonView( { DetailedView.class } )
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

    public Set<DataElement> getDataElementsInSections()
    {
        Set<DataElement> dataElements = new HashSet<>();

        for ( Section section : sections )
        {
            dataElements.addAll( section.getDataElements() );
        }

        return dataElements;
    }

    public Set<DataElementCategoryOptionCombo> getDataElementOptionCombos()
    {
        Set<DataElementCategoryOptionCombo> optionCombos = new HashSet<>();

        for ( DataElement element : dataElements )
        {
            if ( element.hasCategoryCombo() )
            {
                optionCombos.addAll( element.getCategoryCombo().getOptionCombos() );
            }
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
            for ( DataElementCategory category : categoryCombo.getCategories() )
            {
                for ( DataElementCategoryOption categoryOption : category.getCategoryOptions() )
                {
                    groupSets.addAll( categoryOption.getGroupSets() );
                }
            }
        }

        return groupSets;
    }

    /**
     * Indicates whether this data set has a category combination which is different
     * from the default category combination.
     */
    public boolean hasCategoryCombo()
    {
        return categoryCombo != null && !DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals( categoryCombo.getName() );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @JsonProperty
    @JsonSerialize( using = JacksonPeriodTypeSerializer.class )
    @JsonDeserialize( using = JacksonPeriodTypeDeserializer.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public PeriodType getPeriodType()
    {
        return periodType;
    }

    public void setPeriodType( PeriodType periodType )
    {
        this.periodType = periodType;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "dataElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( Set<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getExpiryDays()
    {
        return expiryDays;
    }

    public void setExpiryDays( int expiryDays )
    {
        this.expiryDays = expiryDays;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isApproveData()
    {
        return approveData;
    }

    public void setApproveData( boolean approveData )
    {
        this.approveData = approveData;
    }

    @JsonProperty( "attributeValues" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "attributeValues", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AttributeValue> getAttributeValues()
    {
        return attributeValues;
    }

    public void setAttributeValues( Set<AttributeValue> attributeValues )
    {
        this.attributeValues = attributeValues;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonView( { DetailedView.class, ExportView.class } )
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            DataSet dataSet = (DataSet) other;

            dataElementDecoration = dataSet.isDataElementDecoration();
            skipOffline = dataSet.isSkipOffline();
            renderAsTabs = dataSet.isRenderAsTabs();
            renderHorizontally = dataSet.isRenderHorizontally();
            expiryDays = dataSet.getExpiryDays();
            openFuturePeriods = dataSet.getOpenFuturePeriods();
            fieldCombinationRequired = dataSet.isFieldCombinationRequired();
            mobile = dataSet.isMobile();
            validCompleteOnly = dataSet.isValidCompleteOnly();
            version = dataSet.getVersion();

            if ( strategy.isReplace() )
            {
                periodType = dataSet.getPeriodType();
                dataEntryForm = dataSet.getDataEntryForm();
                legendSet = dataSet.getLegendSet();
                notificationRecipients = dataSet.getNotificationRecipients();
            }
            else if ( strategy.isMerge() )
            {
                periodType = dataSet.getPeriodType() == null ? periodType : dataSet.getPeriodType();
                dataEntryForm = dataSet.getDataEntryForm() == null ? dataEntryForm : dataSet.getDataEntryForm();
                legendSet = dataSet.getLegendSet() == null ? legendSet : dataSet.getLegendSet();
                notificationRecipients = dataSet.getNotificationRecipients() == null ? notificationRecipients : dataSet.getNotificationRecipients();
            }

            dataElements.clear();
            dataSet.getDataElements().forEach( this::addDataElement );

            indicators.clear();
            dataSet.getIndicators().forEach( this::addIndicator );

            compulsoryDataElementOperands.clear();
            dataSet.getCompulsoryDataElementOperands().forEach( this::addCompulsoryDataElementOperand );

            removeAllOrganisationUnits();
            dataSet.getSources().forEach( this::addOrganisationUnit );

            attributeValues.clear();
            attributeValues.addAll( dataSet.getAttributeValues() );
        }
    }
}

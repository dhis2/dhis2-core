package org.hisp.dhis.dataelement;

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
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.comparator.DataSetApprovalFrequencyComparator;
import org.hisp.dhis.dataset.comparator.DataSetFrequencyComparator;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.TranslationProperty;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.dataset.DataSet.NO_EXPIRY;

/**
 * A DataElement is a definition (meta-information about) of the entities that
 * are captured in the system. An example from public health care is a
 * DataElement representing the number BCG doses; A DataElement with "BCG dose"
 * as name, with type DataElement.TYPE_INT. DataElements can be structured
 * hierarchically, one DataElement can have a parent and a collection of
 * children. The sum of the children represent the same entity as the parent.
 * Hierarchies of DataElements are used to give more fine- or course-grained
 * representations of the entities.
 * <p>
 * DataElement acts as a DimensionSet in the dynamic dimensional model, and as a
 * DimensionOption in the static DataElement dimension.
 *
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
public class DataElement
    extends BaseDimensionalItemObject
{
    public static final String[] I18N_PROPERTIES = { "name", "shortName", "description", "formName" };

    /**
     * Data element value type (int, boolean, etc)
     */
    private ValueType valueType;

    /**
     * The name to appear in forms.
     */
    private String formName;

    /**
     * The i18n variant of the display name. Should not be persisted.
     */
    protected transient String displayFormName;

    /**
     * The domain of this DataElement; e.g. DataElementDomainType.AGGREGATE or
     * DataElementDomainType.TRACKER.
     */
    private DataElementDomain domainType;

    /**
     * A combination of categories to capture data.
     */
    private DataElementCategoryCombo categoryCombo;

    /**
     * URL for lookup of additional information on the web.
     */
    private String url;

    /**
     * The data element groups which this
     */
    private Set<DataElementGroup> groups = new HashSet<>();

    /**
     * The data sets which this data element is a member of.
     */
    private Set<DataSet> dataSets = new HashSet<>();

    /**
     * The lower organisation unit levels for aggregation.
     */
    private List<Integer> aggregationLevels = new ArrayList<>();

    /**
     * Indicates whether to store zero data values.
     */
    private boolean zeroIsSignificant;

    /**
     * The option set for data values linked to this data element, can be null.
     */
    private OptionSet optionSet;

    /**
     * The option set for comments linked to this data element, can be null.
     */
    private OptionSet commentOptionSet;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElement()
    {
    }

    public DataElement( String name )
    {
        this();
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementGroup( DataElementGroup group )
    {
        groups.add( group );
        group.getMembers().add( this );
    }

    public void removeDataElementGroup( DataElementGroup group )
    {
        groups.remove( group );
        group.getMembers().remove( this );
    }

    public void updateDataElementGroups( Set<DataElementGroup> updates )
    {
        for ( DataElementGroup group : new HashSet<>( groups ) )
        {
            if ( !updates.contains( group ) )
            {
                removeDataElementGroup( group );
            }
        }

        updates.forEach( this::addDataElementGroup );
    }

    public void addDataSet( DataSet dataSet )
    {
        dataSets.add( dataSet );
        dataSet.getDataElements().add( this );
    }

    public void removeDataSet( DataSet dataSet )
    {
        dataSets.remove( dataSet );
        dataSet.getDataElements().remove( this );
    }

    /**
     * Indicates whether the value type of this data element is numeric.
     */
    public boolean isNumericType()
    {
        return getValueType().isNumeric();
    }

    /**
     * Indicates whether the value type of this data element is a file (externally stored resource)
     */
    public boolean isFileType()
    {
        return getValueType().isFile();
    }

    /**
     * Returns the data set of this data element. If this data element has
     * multiple data sets, the data set with the highest collection frequency is
     * returned.
     */
    public DataSet getDataSet()
    {
        List<DataSet> list = new ArrayList<>( dataSets );
        Collections.sort( list, DataSetFrequencyComparator.INSTANCE );
        return !list.isEmpty() ? list.get( 0 ) : null;
    }

    /**
     * Returns the data set of this data element. If this data element has
     * multiple data sets, the data set with approval enabled, then the highest
     * collection frequency, is returned.
     */
    public DataSet getApprovalDataSet()
    {
        List<DataSet> list = new ArrayList<>( dataSets );
        Collections.sort( list, DataSetApprovalFrequencyComparator.INSTANCE );
        return !list.isEmpty() ? list.get( 0 ) : null;
    }

    /**
     * Returns the category combinations associated with the data sets of this
     * data element.
     */
    public Set<DataElementCategoryCombo> getDataSetCategoryCombos()
    {
        Set<DataElementCategoryCombo> categoryCombos = new HashSet<>();

        for ( DataSet dataSet : dataSets )
        {
            categoryCombos.add( dataSet.getCategoryCombo() );
        }

        return categoryCombos;
    }

    /**
     * Returns the category options combinations associated with the data sets of this
     * data element.
     */
    public Set<DataElementCategoryOptionCombo> getDataSetCategoryOptionCombos()
    {
        Set<DataElementCategoryOptionCombo> categoryOptionCombos = new HashSet<>();

        for ( DataSet dataSet : dataSets )
        {
            categoryOptionCombos.addAll( dataSet.getCategoryCombo().getOptionCombos() );
        }

        return categoryOptionCombos;
    }

    /**
     * Returns the PeriodType of the DataElement, based on the PeriodType of the
     * DataSet which the DataElement is associated with. If this data element has
     * multiple data sets, the data set with the highest collection frequency is
     * returned.
     */
    public PeriodType getPeriodType()
    {
        DataSet dataSet = getDataSet();

        return dataSet != null ? dataSet.getPeriodType() : null;
    }

    /**
     * Returns the PeriodTypes of the DataElement, based on the PeriodType of the
     * DataSets which the DataElement is associated with.
     */
    public Set<PeriodType> getPeriodTypes()
    {
        return Sets.newHashSet( dataSets ).stream().map( DataSet::getPeriodType ).collect( Collectors.toSet() );
    }

    /**
     * Indicates whether this data element requires approval of data. Returns true
     * if only one of the data sets associated with this data element requires
     * approval.
     */
    public boolean isApproveData()
    {
        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet != null && dataSet.getWorkflow() != null )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Number of periods in the future to open for data capture, 0 means capture
     * not allowed for current period. Based on the data sets of which this data
     * element is a member.
     */
    public int getOpenFuturePeriods()
    {
        int maxOpenPeriods = 0;

        for ( DataSet dataSet : dataSets )
        {
            maxOpenPeriods = Math.max( maxOpenPeriods, dataSet.getOpenFuturePeriods() );
        }

        return maxOpenPeriods;
    }

    /**
     * Returns the latest period which is open for data input. Returns null if
     * data element is not associated with any data sets.
     *
     * @return the latest period which is open for data input.
     */
    public Period getLatestOpenFuturePeriod()
    {
        int periods = getOpenFuturePeriods();

        PeriodType periodType = getPeriodType();

        if ( periodType != null )
        {
            Period period = periodType.createPeriod();

            // Rewind one as 0 open periods implies current period is locked

            period = periodType.getPreviousPeriod( period );

            return periodType.getNextPeriod( period, periods );
        }

        return null;
    }

    /**
     * Returns the frequency order for the PeriodType of this DataElement. If no
     * PeriodType exists, 0 is returned.
     */
    public int getFrequencyOrder()
    {
        PeriodType periodType = getPeriodType();

        return periodType != null ? periodType.getFrequencyOrder() : YearlyPeriodType.FREQUENCY_ORDER;
    }

    /**
     * Tests whether a PeriodType can be defined for the DataElement, which
     * requires that the DataElement is registered for DataSets with the same
     * PeriodType.
     */
    public boolean periodTypeIsValid()
    {
        PeriodType periodType = null;

        for ( DataSet dataSet : dataSets )
        {
            if ( periodType != null && !periodType.equals( dataSet.getPeriodType() ) )
            {
                return false;
            }

            periodType = dataSet.getPeriodType();
        }

        return true;
    }

    /**
     * Tests whether more than one aggregation level exists for the DataElement.
     */
    public boolean hasAggregationLevels()
    {
        return aggregationLevels != null && aggregationLevels.size() > 0;
    }

    public boolean hasCategoryCombo()
    {
        return categoryCombo != null;
    }

    /**
     * Tests whether the DataElement is associated with a
     * DataElementCategoryCombo with more than one DataElementCategory, or any
     * DataElementCategory with more than one DataElementCategoryOption.
     */
    public boolean isMultiDimensional()
    {
        if ( categoryCombo != null )
        {
            if ( categoryCombo.getCategories().size() > 1 )
            {
                return true;
            }

            for ( DataElementCategory category : categoryCombo.getCategories() )
            {
                if ( category.getCategoryOptions().size() > 1 )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the form name, or the name if it does not exist.
     */
    public String getFormNameFallback()
    {
        return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDisplayFormName()
    {
        displayFormName = getTranslation( TranslationProperty.FORM_NAME, displayFormName );
        return displayFormName != null ? displayFormName : getFormName() != null && !getFormName().isEmpty() ? getFormName() : getDisplayName();
    }

    public void setDisplayFormName( String displayFormName )
    {
        this.displayFormName = displayFormName;
    }

    /**
     * Returns the minimum number of expiry days from the data sets of this data
     * element. Returns {@link DataSet.NO_EXPIRY} if no data sets has expiry.
     */
    public int getExpiryDays()
    {
        int expiryDays = Integer.MAX_VALUE;

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getExpiryDays() != NO_EXPIRY && dataSet.getExpiryDays() < expiryDays )
            {
                expiryDays = dataSet.getExpiryDays();
            }
        }

        return expiryDays == Integer.MAX_VALUE ? NO_EXPIRY : expiryDays;
    }

    /**
     * Indicates whether the given period is considered expired for the end date
     * of the given date based on the expiry days of the data sets associated
     * with this data element.
     *
     * @param period the period.
     * @param now    the date used as basis.
     * @return true or false.
     */
    public boolean isExpired( Period period, Date now )
    {
        int expiryDays = getExpiryDays();

        return expiryDays != DataSet.NO_EXPIRY && new DateTime( period.getEndDate() ).plusDays( expiryDays ).isBefore( new DateTime( now ) );
    }

    public boolean hasDescription()
    {
        return description != null && !description.trim().isEmpty();
    }

    public boolean hasUrl()
    {
        return url != null && !url.trim().isEmpty();
    }

    public boolean hasOptionSet()
    {
        return optionSet != null;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    //TODO can also be dimension

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.DATA_ELEMENT;
    }

    // -------------------------------------------------------------------------
    // Helper getters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOptionSetValue()
    {
        return optionSet != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        //TODO
        //return optionSet != null ? optionSet.getValueType() : valueType;
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getFormName()
    {
        return formName;
    }

    public void setFormName( String formName )
    {
        this.formName = formName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementDomain getDomainType()
    {
        return domainType;
    }

    public void setDomainType( DataElementDomain domainType )
    {
        this.domainType = domainType;
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
    @Property( PropertyType.URL )
    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @JsonProperty( "dataElementGroups" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataElementGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElementGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<DataElementGroup> groups )
    {
        this.groups = groups;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSet", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataSet> getDataSets()
    {
        return dataSets;
    }

    public void setDataSets( Set<DataSet> dataSets )
    {
        this.dataSets = dataSets;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Integer> getAggregationLevels()
    {
        return aggregationLevels;
    }

    public void setAggregationLevels( List<Integer> aggregationLevels )
    {
        this.aggregationLevels = aggregationLevels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isZeroIsSignificant()
    {
        return zeroIsSignificant;
    }

    public void setZeroIsSignificant( boolean zeroIsSignificant )
    {
        this.zeroIsSignificant = zeroIsSignificant;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OptionSet getOptionSet()
    {
        return optionSet;
    }

    public void setOptionSet( OptionSet optionSet )
    {
        this.optionSet = optionSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OptionSet getCommentOptionSet()
    {
        return commentOptionSet;
    }

    public void setCommentOptionSet( OptionSet commentOptionSet )
    {
        this.commentOptionSet = commentOptionSet;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DataElement dataElement = (DataElement) other;

            zeroIsSignificant = dataElement.isZeroIsSignificant();

            if ( mergeMode.isReplace() )
            {
                formName = dataElement.getFormName();
                domainType = dataElement.getDomainType();
                aggregationType = dataElement.getAggregationType();
                valueType = dataElement.getValueType();
                categoryCombo = dataElement.getCategoryCombo();
                url = dataElement.getUrl();
                optionSet = dataElement.getOptionSet();
                commentOptionSet = dataElement.getCommentOptionSet();
            }
            else if ( mergeMode.isMerge() )
            {
                formName = dataElement.getFormName() == null ? formName : dataElement.getFormName();
                domainType = dataElement.getDomainType() == null ? domainType : dataElement.getDomainType();
                aggregationType = dataElement.getAggregationType() == null ? aggregationType : dataElement.getAggregationType();
                valueType = dataElement.getValueType() == null ? valueType : dataElement.getValueType();
                categoryCombo = dataElement.getCategoryCombo() == null ? categoryCombo : dataElement.getCategoryCombo();
                url = dataElement.getUrl() == null ? url : dataElement.getUrl();
                optionSet = dataElement.getOptionSet() == null ? optionSet : dataElement.getOptionSet();
                commentOptionSet = dataElement.getCommentOptionSet() == null ? commentOptionSet : dataElement.getCommentOptionSet();
            }

            groups.clear();
            dataSets.clear();

            aggregationLevels.clear();
            aggregationLevels.addAll( dataElement.getAggregationLevels() );
        }
    }
}

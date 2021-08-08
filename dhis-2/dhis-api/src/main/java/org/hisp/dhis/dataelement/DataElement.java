/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataelement;

import static org.hisp.dhis.dataset.DataSet.NO_EXPIRY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.comparator.DataSetApprovalFrequencyComparator;
import org.hisp.dhis.dataset.comparator.DataSetFrequencyComparator;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * A DataElement is a definition (meta-information about) of the entities that
 * are captured in the system. An example from public health care is a
 * DataElement representing the number BCG doses; A DataElement with "BCG dose"
 * as name, with type DataElement.TYPE_INT.
 * <p>
 * DataElement acts as a DimensionSet in the dynamic dimensional model, and as a
 * DimensionOption in the static DataElement dimension.
 *
 * @author Kristian Nordal
 */
@JacksonXmlRootElement( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
public class DataElement extends BaseDimensionalItemObject
    implements MetadataObject, ValueTypedDimensionalItemObject
{
    /**
     * Data element value type (int, boolean, etc)
     */
    private ValueType valueType;

    private ValueTypeOptions valueTypeOptions;

    /**
     * The name to appear in forms.
     */
    private String formName;

    /**
     * The domain of this DataElement; e.g. DataElementDomainType.AGGREGATE or
     * DataElementDomainType.TRACKER.
     */
    private DataElementDomain domainType;

    /**
     * A combination of categories to capture data for this data element. Note
     * that this category combination could be overridden by data set elements
     * which this data element is part of, see {@link DataSetElement}.
     */
    private CategoryCombo categoryCombo;

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
    private Set<DataSetElement> dataSetElements = new HashSet<>();

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
    @AuditAttribute
    private OptionSet optionSet;

    /**
     * The option set for comments linked to this data element, can be null.
     */
    private OptionSet commentOptionSet;

    /**
     * The style defines how the DataElement should be represented on clients
     */
    private ObjectStyle style;

    /**
     * Field mask represent how the value should be formatted during input. This
     * string will be validated as a TextPatternSegment of type TEXT.
     */
    private String fieldMask;

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

    public boolean removeDataSetElement( DataSetElement element )
    {
        dataSetElements.remove( element );
        return element.getDataSet().getDataSetElements().remove( element );
    }

    /**
     * Returns the resolved category combinations by joining the category
     * combinations of the data set elements of which this data element is part
     * of and the category combination linked directly with this data element.
     * The returned set is immutable, will never be null and will contain at
     * least one item.
     */
    public Set<CategoryCombo> getCategoryCombos()
    {
        return ImmutableSet.<CategoryCombo> builder()
            .addAll( dataSetElements.stream()
                .filter( DataSetElement::hasCategoryCombo )
                .map( DataSetElement::getCategoryCombo )
                .collect( Collectors.toSet() ) )
            .add( categoryCombo ).build();
    }

    /**
     * Returns the category combination of the data set element matching the
     * given data set for this data element. If not present, returns the
     * category combination for this data element.
     */
    public CategoryCombo getDataElementCategoryCombo( DataSet dataSet )
    {
        for ( DataSetElement element : dataSetElements )
        {
            if ( dataSet.typedEquals( element.getDataSet() ) && element.hasCategoryCombo() )
            {
                return element.getCategoryCombo();
            }
        }

        return categoryCombo;
    }

    /**
     * Returns the category option combinations of the resolved category
     * combinations of this data element. The returned set is immutable, will
     * never be null and will contain at least one item.
     */
    public Set<CategoryOptionCombo> getCategoryOptionCombos()
    {
        return getCategoryCombos().stream()
            .map( CategoryCombo::getOptionCombos )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
    }

    /**
     * Returns the sorted category option combinations of the resolved category
     * combinations of this data element. The returned list is immutable, will
     * never be null and will contain at least one item.
     */
    public List<CategoryOptionCombo> getSortedCategoryOptionCombos()
    {
        List<CategoryOptionCombo> optionCombos = Lists.newArrayList();
        getCategoryCombos().forEach( cc -> optionCombos.addAll( cc.getSortedOptionCombos() ) );
        return optionCombos;
    }

    /**
     * Indicates whether the value type of this data element is numeric.
     */
    public boolean isNumericType()
    {
        return getValueType().isNumeric();
    }

    /**
     * Indicates whether the value type of this data element is a file
     * (externally stored resource)
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
        List<DataSet> list = new ArrayList<>( getDataSets() );
        list.sort( DataSetFrequencyComparator.INSTANCE );
        return !list.isEmpty() ? list.get( 0 ) : null;
    }

    /**
     * Returns the data set of this data element. If this data element has
     * multiple data sets, the data set with approval enabled, then the highest
     * collection frequency, is returned.
     */
    public DataSet getApprovalDataSet()
    {
        List<DataSet> list = new ArrayList<>( getDataSets() );
        list.sort( DataSetApprovalFrequencyComparator.INSTANCE );
        return !list.isEmpty() ? list.get( 0 ) : null;
    }

    /**
     * Note that this method returns an immutable set and can not be used to
     * modify the model. Returns an immutable set of data sets associated with
     * this data element.
     */
    public Set<DataSet> getDataSets()
    {
        return ImmutableSet.copyOf( dataSetElements.stream().map( DataSetElement::getDataSet ).filter(
            Objects::nonNull ).collect( Collectors.toSet() ) );
    }

    /**
     * Returns the attribute category options combinations associated with the
     * data sets of this data element.
     */
    public Set<CategoryOptionCombo> getDataSetCategoryOptionCombos()
    {
        Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

        for ( DataSet dataSet : getDataSets() )
        {
            categoryOptionCombos.addAll( dataSet.getCategoryCombo().getOptionCombos() );
        }

        return categoryOptionCombos;
    }

    /**
     * Returns the PeriodType of the DataElement, based on the PeriodType of the
     * DataSet which the DataElement is associated with. If this data element
     * has multiple data sets, the data set with the highest collection
     * frequency is returned.
     */
    public PeriodType getPeriodType()
    {
        DataSet dataSet = getDataSet();

        return dataSet != null ? dataSet.getPeriodType() : null;
    }

    /**
     * Returns the PeriodTypes of the DataElement, based on the PeriodType of
     * the DataSets which the DataElement is associated with.
     */
    public Set<PeriodType> getPeriodTypes()
    {
        return getDataSets().stream().map( DataSet::getPeriodType ).collect( Collectors.toSet() );
    }

    /**
     * Indicates whether this data element requires approval of data. Returns
     * true if only one of the data sets associated with this data element
     * requires approval.
     */
    public boolean isApproveData()
    {
        for ( DataSet dataSet : getDataSets() )
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

        for ( DataSet dataSet : getDataSets() )
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

        for ( DataSet dataSet : getDataSets() )
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

    /**
     * Returns the maximum number of expiry days from the data sets of this data
     * element. Returns {@link DataSet#NO_EXPIRY} if any data set has no expiry.
     */
    public int getExpiryDays()
    {
        int expiryDays = Integer.MIN_VALUE;

        for ( DataSet dataSet : getDataSets() )
        {
            if ( dataSet.getExpiryDays() == NO_EXPIRY )
            {
                return NO_EXPIRY;
            }

            if ( dataSet.getExpiryDays() > expiryDays )
            {
                expiryDays = dataSet.getExpiryDays();
            }
        }

        return expiryDays == Integer.MIN_VALUE ? NO_EXPIRY : expiryDays;
    }

    /**
     * Indicates whether the given period is considered expired for the end date
     * of the given date based on the expiry days of the data sets associated
     * with this data element.
     *
     * @param period the period.
     * @param now the date used as basis.
     * @return true or false.
     */
    public boolean isExpired( Period period, Date now )
    {
        int expiryDays = getExpiryDays();

        return expiryDays != DataSet.NO_EXPIRY
            && new DateTime( period.getEndDate() ).plusDays( expiryDays ).isBefore( new DateTime( now ) );
    }

    /**
     * Indicates whether this data element has a description.
     *
     * @return true if this data element has a description.
     */
    public boolean hasDescription()
    {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Indicates whether this data element has a URL.
     *
     * @return true if this data element has a URL.
     */
    public boolean hasUrl()
    {
        return url != null && !url.trim().isEmpty();
    }

    /**
     * Indicates whether this data element has an option set.
     *
     * @return true if this data element has an option set.
     */
    @Override
    public boolean hasOptionSet()
    {
        return optionSet != null;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    // TODO can also be dimension

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

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        // TODO return optionSet != null ? optionSet.getValueType() : valueType;
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueTypeOptions getValueTypeOptions()
    {
        return valueTypeOptions;
    }

    public void setValueTypeOptions( ValueTypeOptions valueTypeOptions )
    {
        this.valueTypeOptions = valueTypeOptions;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
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
    public DataElementDomain getDomainType()
    {
        return domainType;
    }

    public void setDomainType( DataElementDomain domainType )
    {
        this.domainType = domainType;
    }

    @JsonProperty( value = "categoryCombo" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "categoryCombo", namespace = DxfNamespaces.DXF_2_0 )
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
    @JacksonXmlElementWrapper( localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataSetElements", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataSetElement> getDataSetElements()
    {
        return dataSetElements;
    }

    public void setDataSetElements( Set<DataSetElement> dataSetElements )
    {
        this.dataSetElements = dataSetElements;
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

    @Override
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

    /**
     * Checks if the combination of period and date is allowed for any of the
     * dataSets associated with the dataElement
     *
     * @param period period to check
     * @param date date to check
     * @return true if no dataSets exists, or at least one dataSet has a valid
     *         DataInputPeriod for the period and date.
     */
    public boolean isDataInputAllowedForPeriodAndDate( Period period, Date date )
    {
        return getDataSets().isEmpty() || getDataSets().stream()
            .anyMatch( dataSet -> dataSet.isDataInputPeriodAndDateAllowed( period, date ) );
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFieldMask()
    {
        return fieldMask;
    }

    public void setFieldMask( String fieldMask )
    {
        this.fieldMask = fieldMask;
    }
}

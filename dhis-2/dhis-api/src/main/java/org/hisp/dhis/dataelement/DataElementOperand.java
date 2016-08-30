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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.expression.ExpressionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;

/**
 * This object can act both as a hydrated persisted object and as a wrapper
 * object (but not both at the same time).
 * <p>
 * This object implements IdentifiableObject but does not have any UID. Instead
 * the UID is generated based on the data element and category option combo which
 * this object is based on.
 *
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "dataElementOperand", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementOperand
    extends BaseDimensionalItemObject
{
    public static final String SEPARATOR = COMPOSITE_DIM_OBJECT_PLAIN_SEP;
    public static final String NAME_TOTAL = "(Total)";

    private static final String TYPE_VALUE = "value";
    private static final String TYPE_TOTAL = "total";

    private static final String SPACE = " ";
    private static final String COLUMN_PREFIX = "de";
    private static final String COLUMN_SEPARATOR = "_";

    // -------------------------------------------------------------------------
    // Persisted properties
    // -------------------------------------------------------------------------

    private DataElement dataElement;

    private DataElementCategoryOptionCombo categoryOptionCombo;

    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    private String dataElementId;

    private String optionComboId;

    private String operandId;

    private String operandName;

    private List<Integer> aggregationLevels = new ArrayList<>();

    private ValueType valueType;

    private int frequencyOrder;

    private String operandType;

    private boolean hasAggregationLevels;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementOperand()
    {
    }

    public DataElementOperand( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public DataElementOperand( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        this.dataElement = dataElement;
        this.categoryOptionCombo = categoryOptionCombo;
    }

    public DataElementOperand( String dataElementId, String optionComboId )
    {
        this.dataElementId = dataElementId;
        this.optionComboId = optionComboId;
        this.operandId = dataElementId + SEPARATOR + optionComboId;
    }

    public DataElementOperand( String dataElementId, String optionComboId, String operandName )
    {
        this.dataElementId = dataElementId;
        this.optionComboId = optionComboId;
        this.operandId = dataElementId + SEPARATOR + optionComboId;
        this.operandName = operandName;
    }

    public DataElementOperand( String dataElementId, String optionComboId, String operandName,
        AggregationType aggregationType, List<Integer> aggregationLevels, int frequencyOrder )
    {
        this.dataElementId = dataElementId;
        this.optionComboId = optionComboId;
        this.operandId = dataElementId + SEPARATOR + optionComboId;
        this.operandName = operandName;
        this.aggregationType = aggregationType;
        this.aggregationLevels = aggregationLevels;
        this.frequencyOrder = frequencyOrder;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public String getDimensionItem()
    {
        String item = null;

        if ( dataElement != null )
        {
            item = dataElement.getUid() + (categoryOptionCombo != null ? (SEPARATOR + categoryOptionCombo.getUid()) : StringUtils.EMPTY);
        }
        else if ( dataElementId != null )
        {
            item = dataElementId + (optionComboId != null ? (SEPARATOR + optionComboId) : StringUtils.EMPTY);
        }

        return item;
    }

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.DATA_ELEMENT_OPERAND;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    @Override
    public String getName()
    {
        if ( name != null )
        {
            return name;
        }

        String name = null;

        if ( dataElement != null )
        {
            name = dataElement.getName();
        }

        if ( categoryOptionCombo != null && !categoryOptionCombo.isDefault() )
        {
            name += SPACE + categoryOptionCombo.getName();
        }

        return name;
    }

    @Override
    public String getShortName()
    {
        String shortName = null;

        if ( dataElement != null )
        {
            shortName = dataElement.getShortName();
        }

        if ( categoryOptionCombo != null )
        {
            shortName += SPACE + categoryOptionCombo.getName();
        }

        return shortName;
    }

    /**
     * Tests whether the operand has any aggregation levels.
     */
    public boolean hasAggregationLevels()
    {
        return aggregationLevels.size() > 0;
    }

    /**
     * Tests whether the hierarchy level of the OrganisationUnit associated with
     * the relevant DataValue is equal to or higher than the relevant
     * aggregation level. Returns true if no aggregation levels exist.
     *
     * @param organisationUnitLevel the hierarchy level of the aggregation
     *                              OrganisationUnit.
     * @param dataValueLevel        the hierarchy level of the OrganisationUnit
     *                              associated with the relevant DataValue.
     */
    public boolean aggregationLevelIsValid( int organisationUnitLevel, int dataValueLevel )
    {
        if ( aggregationLevels.size() == 0 )
        {
            return true;
        }

        final Integer aggregationLevel = getRelevantAggregationLevel( organisationUnitLevel );

        return aggregationLevel == null || dataValueLevel <= aggregationLevel;
    }

    /**
     * Returns the relevant aggregation level for the DataElement. The relevant
     * aggregation level will be the next in ascending order after the
     * organisation unit level. If no aggregation levels lower than the
     * organisation unit level exist, null is returned.
     *
     * @param organisationUnitLevel the hierarchy level of the relevant
     *                              OrganisationUnit.
     */
    public Integer getRelevantAggregationLevel( int organisationUnitLevel )
    {
        List<Integer> levels = new ArrayList<>( aggregationLevels );

        Collections.sort( levels );

        for ( final Integer aggregationLevel : levels )
        {
            if ( aggregationLevel >= organisationUnitLevel )
            {
                return aggregationLevel;
            }
        }

        return null;
    }

    /**
     * Returns the operand expression which is on the format #{de-uid.coc-uid} .
     *
     * @return the operand expression.
     */
    public String getOperandExpression()
    {
        String expression = null;

        if ( dataElementId != null )
        {
            String coc = optionComboId != null ? SEPARATOR + optionComboId : "";

            expression = "#{" + dataElementId + coc + "}";
        }
        else if ( dataElement != null )
        {
            String coc = categoryOptionCombo != null ? SEPARATOR + categoryOptionCombo.getUid() : "";

            expression = "#{" + dataElement.getUid() + coc + "}";
        }

        return expression;
    }

    /**
     * Returns a database-friendly name.
     *
     * @return the name.
     */
    public String getColumnName()
    {
        return COLUMN_PREFIX + dataElementId + COLUMN_SEPARATOR + optionComboId;
    }

    /**
     * Returns a pretty-print name based on the given data element and category
     * option combo.
     *
     * @param dataElement         the data element.
     * @param categoryOptionCombo the category option combo.
     * @return the name.
     */
    public static String getPrettyName( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        if ( dataElement == null ) // Invalid
        {
            return null;
        }

        if ( categoryOptionCombo == null ) // Total
        {
            return dataElement.getDisplayName() + SPACE + NAME_TOTAL;
        }

        return categoryOptionCombo.isDefault() ? dataElement.getDisplayName() : dataElement.getDisplayName() + SPACE + categoryOptionCombo.getName();
    }

    /**
     * Returns a pretty name, requires the operand to be in persistent mode.
     *
     * @return the name.
     */
    public String getPrettyName()
    {
        return getPrettyName( dataElement, categoryOptionCombo );
    }

    /**
     * Indicators whether this operand represents a total value or not.
     *
     * @return true or false.
     */
    public boolean isTotal()
    {
        return operandType != null && operandType.equals( TYPE_TOTAL );
    }

    /**
     * Updates all transient properties.
     *
     * @param dataElement
     * @param categoryOptionCombo
     */
    public void updateProperties( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        this.dataElementId = dataElement.getUid();
        this.optionComboId = categoryOptionCombo.getUid();
        this.operandId = dataElementId + SEPARATOR + optionComboId;
        this.operandName = getPrettyName( dataElement, categoryOptionCombo );
        this.legendSet = dataElement.getLegendSet();
        this.aggregationType = dataElement.getAggregationType();
        this.valueType = dataElement.getValueType();
        this.frequencyOrder = dataElement.getFrequencyOrder();
        this.aggregationLevels = new ArrayList<>( dataElement.getAggregationLevels() );

        this.uid = dataElementId + SEPARATOR + optionComboId;
        this.name = getPrettyName( dataElement, categoryOptionCombo );
    }

    /**
     * Updates all transient properties.
     *
     * @param dataElement
     */
    public void updateProperties( DataElement dataElement )
    {
        this.dataElementId = dataElement.getUid();
        this.operandId = String.valueOf( dataElementId );
        this.operandName = getPrettyName( dataElement, null );
        this.legendSet = dataElement.getLegendSet();
        this.aggregationType = dataElement.getAggregationType();
        this.valueType = dataElement.getValueType();
        this.frequencyOrder = dataElement.getFrequencyOrder();
        this.aggregationLevels = new ArrayList<>( dataElement.getAggregationLevels() );

        this.uid = dataElementId;
        this.name = getPrettyName( dataElement, null );
    }

    /**
     * Generates a DataElementOperand based on the given formula. The formula
     * needs to be on the form "#{<dataelementid>.<categoryoptioncomboid>}".
     *
     * @param expression the formula.
     * @return a DataElementOperand.
     */
    public static DataElementOperand getOperand( String expression )
        throws NumberFormatException
    {
        Matcher matcher = ExpressionService.OPERAND_PATTERN.matcher( expression );
        matcher.find();
        String dataElement = StringUtils.trimToNull( matcher.group( 1 ) );
        String categoryOptionCombo = StringUtils.trimToNull( matcher.group( 2 ) );
        String operandType = categoryOptionCombo != null ? TYPE_VALUE : TYPE_TOTAL;

        final DataElementOperand operand = new DataElementOperand( dataElement, categoryOptionCombo );
        operand.setOperandType( operandType );
        return operand;
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryOptionCombo getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public void setCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( String dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOptionComboId()
    {
        return optionComboId;
    }

    public void setOptionComboId( String optionComboId )
    {
        this.optionComboId = optionComboId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOperandId()
    {
        return operandId;
    }

    public void setOperandId( String operandId )
    {
        this.operandId = operandId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOperandName()
    {
        return operandName;
    }

    public void setOperandName( String operandName )
    {
        this.operandName = operandName;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "aggregationLevels", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "aggregationLevel", namespace = DxfNamespaces.DXF_2_0 )
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
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getFrequencyOrder()
    {
        return frequencyOrder;
    }

    public void setFrequencyOrder( int frequencyOrder )
    {
        this.frequencyOrder = frequencyOrder;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOperandType()
    {
        return operandType;
    }

    public void setOperandType( String operandType )
    {
        this.operandType = operandType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHasAggregationLevels()
    {
        return hasAggregationLevels;
    }

    public void setHasAggregationLevels( boolean hasAggregationLevels )
    {
        this.hasAggregationLevels = hasAggregationLevels;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals, toString, compareTo
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + id + "\", " +
            "\"uid\":\"" + uid + "\", " +
            "\"dataElement\":" + dataElement + ", " +
            "\"categoryOptionCombo\":" + categoryOptionCombo + ", " +
            "\"dataElementId\":\"" + dataElementId + "\", " +
            "\"optionComboId\":\"" + optionComboId + "\", " +
            "\"operandId\":\"" + operandId + "\", " +
            "\"operandName\":\"" + operandName + "\", " +
            "\"operandType\":\"" + operandType + "\" " +
            '}';
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        if ( dataElement != null && categoryOptionCombo != null )
        {
            updateProperties( dataElement, categoryOptionCombo );
        }

        result = prime * result + ((dataElement == null) ? 0 : dataElement.hashCode());
        result = prime * result + ((categoryOptionCombo == null) ? 0 : categoryOptionCombo.hashCode());
        result = prime * result + ((dataElementId == null) ? 0 : dataElementId.hashCode());
        result = prime * result + ((optionComboId == null) ? 0 : optionComboId.hashCode());

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        if ( dataElement != null && categoryOptionCombo != null )
        {
            updateProperties( dataElement, categoryOptionCombo );
        }

        DataElementOperand other = (DataElementOperand) object;

        if ( other.getDataElement() != null && other.getCategoryOptionCombo() != null )
        {
            other.updateProperties( other.getDataElement(), other.getCategoryOptionCombo() );
        }

        if ( dataElement == null )
        {
            if ( other.dataElement != null )
            {
                return false;
            }
        }
        else if ( !dataElement.equals( other.dataElement ) )
        {
            return false;
        }

        if ( categoryOptionCombo == null )
        {
            if ( other.categoryOptionCombo != null )
            {
                return false;
            }
        }
        else if ( !categoryOptionCombo.equals( other.categoryOptionCombo ) )
        {
            return false;
        }

        if ( dataElementId == null )
        {
            if ( other.dataElementId != null )
            {
                return false;
            }
        }
        else if ( !dataElementId.equals( other.dataElementId ) )
        {
            return false;
        }

        if ( optionComboId == null )
        {
            if ( other.optionComboId != null )
            {
                return false;
            }
        }
        else if ( !optionComboId.equals( other.optionComboId ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo( IdentifiableObject object )
    {
        DataElementOperand other = (DataElementOperand) object;

        if ( this.dataElementId.compareTo( other.dataElementId ) != 0 )
        {
            return this.dataElementId.compareTo( other.dataElementId );
        }

        return this.optionComboId.compareTo( other.optionComboId );
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementOperand dataElementOperand = (DataElementOperand) other;

            if ( mergeMode.isReplace() )
            {
                dataElement = dataElementOperand.getDataElement();
                categoryOptionCombo = dataElementOperand.getCategoryOptionCombo();
            }
            else if ( mergeMode.isMerge() )
            {
                dataElement = dataElementOperand.getDataElement() != null ? dataElementOperand.getDataElement() : dataElement;
                categoryOptionCombo = dataElementOperand.getCategoryOptionCombo() != null ? dataElementOperand.getCategoryOptionCombo() : categoryOptionCombo;
            }
        }
    }
}

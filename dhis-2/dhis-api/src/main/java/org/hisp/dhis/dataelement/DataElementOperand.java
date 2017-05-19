package org.hisp.dhis.dataelement;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;

import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;

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
    extends BaseDimensionalItemObject implements EmbeddedObject
{
    public static final String SEPARATOR = COMPOSITE_DIM_OBJECT_PLAIN_SEP;

    private static final String SPACE = " ";

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    private DataElement dataElement;

    private DataElementCategoryOptionCombo categoryOptionCombo;

    private DataElementCategoryOptionCombo attributeOptionCombo;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementOperand()
    {
        setAutoFields();
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

    public DataElementOperand( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.dataElement = dataElement;
        this.categoryOptionCombo = categoryOptionCombo;
        this.attributeOptionCombo = attributeOptionCombo;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public String getDimensionItem()
    {
        return getDimensionItem( IdScheme.UID );
    }

    @Override
    public String getDimensionItem( IdScheme idScheme )
    {
        String item = null;

        if ( dataElement != null )
        {
            item = dataElement.getPropertyValue( idScheme );

            if ( categoryOptionCombo != null )
            {
                item += SEPARATOR + categoryOptionCombo.getPropertyValue( idScheme );
            }
            else if ( attributeOptionCombo != null )
            {
                item += SEPARATOR + SYMBOL_WILDCARD;
            }

            if ( attributeOptionCombo != null )
            {
                item += SEPARATOR + attributeOptionCombo.getPropertyValue( idScheme );
            }
        }

        return item;
    }

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.DATA_ELEMENT_OPERAND;
    }

    // -------------------------------------------------------------------------
    // IdentifiableObject
    // -------------------------------------------------------------------------

    @Override
    public String getUid()
    {
        String uid = null;

        if ( dataElement != null )
        {
            uid = dataElement.getUid();
        }

        if ( categoryOptionCombo != null && !categoryOptionCombo.isDefault() )
        {
            uid += SEPARATOR + categoryOptionCombo.getUid();
        }

        return uid;
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

        if ( hasNonDefaultCategoryOptionCombo() )
        {
            name += SPACE + categoryOptionCombo.getName();
        }
        else if ( hasNonDefaultAttributeOptionCombo() )
        {
            name += SPACE + SYMBOL_WILDCARD;
        }

        if ( hasNonDefaultAttributeOptionCombo() )
        {
            name += SPACE + attributeOptionCombo.getName();
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

        if ( hasNonDefaultCategoryOptionCombo() )
        {
            shortName += SPACE + categoryOptionCombo.getShortName();
        }
        else if ( hasNonDefaultAttributeOptionCombo() )
        {
            name += SPACE + SYMBOL_WILDCARD;
        }

        if ( hasNonDefaultAttributeOptionCombo() )
        {
            name += SPACE + attributeOptionCombo.getName();
        }

        return shortName;
    }

    /**
     * Creates a {@link DataElementOperand} instance from the given identifiers.
     *
     * @param dataElementUid         the data element identifier.
     * @param categoryOptionComboUid the category option combo identifier.
     * @return a data element operand instance.
     */
    public static DataElementOperand instance( String dataElementUid, String categoryOptionComboUid )
    {
        DataElement de = new DataElement();
        de.setUid( dataElementUid );

        DataElementCategoryOptionCombo coc = null;

        if ( categoryOptionComboUid != null )
        {
            coc = new DataElementCategoryOptionCombo();
            coc.setUid( categoryOptionComboUid );
        }

        return new DataElementOperand( de, coc );
    }

    /**
     * Indicates whether this operand specifies a data element only
     * with no option combinations.
     *
     * @return true if operand specifies a data element only.
     */
    public boolean isTotal()
    {
        return categoryOptionCombo == null && attributeOptionCombo == null;
    }

    /**
     * Indicates whether a category option combination exists which is different
     * from default.
     */
    public boolean hasNonDefaultCategoryOptionCombo()
    {
        return categoryOptionCombo != null && !categoryOptionCombo.isDefault();
    }

    /**
     * Indicates whether an attribute option combination exists which is different
     * from default.
     */
    public boolean hasNonDefaultAttributeOptionCombo()
    {
        return attributeOptionCombo != null && !attributeOptionCombo.isDefault();
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryOptionCombo getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( DataElementCategoryOptionCombo attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    // -------------------------------------------------------------------------
    // toString, mergeWith
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + id + "\", " +
            "\"uid\":\"" + uid + "\", " +
            "\"dataElement\":" + dataElement + ", " +
            "\"categoryOptionCombo\":" + categoryOptionCombo +
            "\"attributeOptionCombo\":" + attributeOptionCombo +
            '}';
    }

    // -------------------------------------------------------------------------
    // Option combination type
    // -------------------------------------------------------------------------

    public enum TotalType
    {
        COC_ONLY( true, false, 1 ),
        AOC_ONLY( false, true, 1 ),
        COC_AND_AOC( true, true, 2 ),
        NONE( false, false, 0 );

        private boolean coc;
        private boolean aoc;
        private int propertyCount;

        TotalType()
        {
        }

        TotalType( boolean coc, boolean aoc, int propertyCount )
        {
            this.coc = coc;
            this.aoc = aoc;
            this.propertyCount = propertyCount;
        }

        public boolean isCategoryOptionCombo()
        {
            return coc;
        }

        public boolean isAttributeOptionCombo()
        {
            return aoc;
        }

        public int getPropertyCount()
        {
            return propertyCount;
        }
    }

    public TotalType getTotalType()
    {
        if ( categoryOptionCombo != null && attributeOptionCombo != null )
        {
            return TotalType.COC_AND_AOC;
        }
        else if ( categoryOptionCombo != null )
        {
            return TotalType.COC_ONLY;
        }
        else if ( attributeOptionCombo != null )
        {
            return TotalType.AOC_ONLY;
        }
        else
        {
            return TotalType.NONE;
        }
    }
}

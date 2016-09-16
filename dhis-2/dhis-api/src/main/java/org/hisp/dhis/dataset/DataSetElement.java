package org.hisp.dhis.dataset;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;

public class DataSetElement
    extends BaseIdentifiableObject
{
    private DataSet dataSet;
    
    private DataElement dataElement;

    private DataElementCategoryCombo categoryCombo;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataSetElement()
    {
    }
    
    public DataSetElement( DataSet dataSet, DataElement dataElement, DataElementCategoryCombo categoryCombo )
    {
        this.dataSet = dataSet;
        this.dataElement = dataElement;
        this.categoryCombo = categoryCombo;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public DataElementCategoryCombo getCategoryComboFallback()
    {
        return hasCategoryCombo() ? getCategoryCombo() : dataElement.getCategoryCombo();
    }
    
    public boolean hasCategoryCombo()
    {
        return categoryCombo != null;
    }

    public boolean hasCategoryComboFallback()
    {
        return hasCategoryCombo() || dataElement.hasCategoryCombo();
    }
    
    // -------------------------------------------------------------------------
    // Hash code and equals
    // -------------------------------------------------------------------------

    public int hashCode()
    {        
        return dataElement.hashCode() * 31 * dataSet.hashCode();
    }
    
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( other == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( other.getClass() ) )
        {
            return false;
        }
        
        DataSetElement element = (DataSetElement) other;
        
        return getDataSet().equals( element.getDataSet() ) && getDataElement().equals( element.getDataElement() );
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }
}

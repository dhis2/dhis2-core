package org.hisp.dhis.dataset;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;

public class DataSetElement
    extends BaseIdentifiableObject
{
    private DataElement dataElement;

    private DataSet dataSet;
    
    private DataElementCategoryCombo categoryCombo;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataSetElement()
    {
    }
    
    public DataSetElement( DataElement dataElement, DataSet dataSet, DataElementCategoryCombo categoryCombo )
    {
        this.dataElement = dataElement;
        this.dataSet = dataSet;
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
        
        return getDataElement().equals( element.getDataElement() ) && getDataSet().equals( element.getDataSet() );
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
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

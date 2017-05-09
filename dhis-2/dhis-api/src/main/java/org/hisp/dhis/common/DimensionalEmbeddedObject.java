package org.hisp.dhis.common;

import java.util.List;

public interface DimensionalEmbeddedObject
{
    int getId();
    
    DimensionalObject getDimension();
    
    List<? extends DimensionalItemObject> getItems();
}

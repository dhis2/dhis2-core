package org.hisp.dhis.outlierdetection;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.NameableObject;

import lombok.Data;

@Data
public class OutlierDetectionMetadata
{
    private Map<String, MetadataItem> items = new HashMap<>();

    public void addItem( NameableObject object )
    {
        MetadataItem item = new MetadataItem( object.getName() );
        item.setUid( object.getUid() );
        item.setCode( object.getCode() );

        this.items.put( object.getUid(), item );
    }
}

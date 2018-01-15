package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.system.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class DataElementObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object != null && object.getClass().isAssignableFrom( DataElement.class ) )
        {
            DataElement de = (DataElement) object;
            DeviceRenderTypeMap<ValueTypeRenderingObject> map = de.getRenderType();

            if ( map != null )
            {
                for ( RenderDevice device : map.keySet() )
                {
                    if ( map.get( device).getType() == null )
                    {
                        errorReports.add( new ErrorReport( DataElement.class, ErrorCode.E4011, "renderType.type" ) );
                    }

                    if ( !ValidationUtils
                        .validateRenderingType( DataElement.class, de.getValueType(), de.hasOptionSet(),
                            map.get( device ).getType() ) )
                    {
                        errorReports.add( new ErrorReport( DataElement.class, ErrorCode.E4017,
                            map.get( device ).getType(), de.getValueType() ) );
                    }

                }
            }
        }

        return errorReports;

    }
}

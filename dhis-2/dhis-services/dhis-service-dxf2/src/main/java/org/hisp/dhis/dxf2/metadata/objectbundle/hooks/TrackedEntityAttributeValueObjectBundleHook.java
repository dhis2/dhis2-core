package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;

import java.util.ArrayList;
import java.util.List;

public class TrackedEntityAttributeValueObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {

        if ( object instanceof TrackedEntityAttributeValue )
        {
            TrackedEntityAttributeValue value = (TrackedEntityAttributeValue) object;
            TrackedEntityAttribute attribute = value.getAttribute();

            if ( attribute.isGenerated() && attribute.getPattern() != null && !attribute.getPattern().isEmpty() )
            {
                if ( !attribute.getTextPattern().validateText( value.getValue() ) )
                {
                    return Lists.newArrayList(
                        new ErrorReport( TrackedEntityAttributeValue.class, ErrorCode.E4017, value.getValue(),
                            attribute.getPattern() )
                    );
                }
            }
        }

        return new ArrayList<>();
    }

}

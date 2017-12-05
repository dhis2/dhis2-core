package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.ArrayList;
import java.util.List;

public class TrackedEntityAttributeObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {

        if ( object instanceof TrackedEntityAttribute )
        {
            TrackedEntityAttribute attribute = (TrackedEntityAttribute) object;

            if ( ((TrackedEntityAttribute) object).isGenerated() )
            {
                if ( attribute.getPattern() == null || attribute.getPattern().isEmpty() )
                {
                    return Lists.newArrayList(
                        new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E4015, "pattern", "generated",
                            true ) );
                }
                try
                {
                    TextPatternParser.parse( attribute.getPattern() );
                }
                catch ( TextPatternParser.TextPatternParsingException e )
                {
                    return Lists.newArrayList(
                        new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E4016, attribute.getPattern(), e.getMessage() )
                    );
                }
            }
        }

        return new ArrayList<>();
    }

}

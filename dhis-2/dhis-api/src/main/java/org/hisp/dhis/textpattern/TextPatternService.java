package org.hisp.dhis.textpattern;

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.List;
import java.util.Map;

public interface TextPatternService
{
    /**
     * Resolves a pattern by injecting values into the TextPattern and returning a fully resolved pattern
     *
     * @param pattern the pattern to inject values into
     * @param values  the values to inject
     * @return a string represeting the pattern with values
     */
    String resolvePattern( TextPattern pattern, Map<String, String> values );

    /**
     * Returns a list of values that are required to resolve the pattern
     *
     * @param pattern the pattern to check
     * @return a list of method names, or an empty list if no values are rquired
     */
    Map<MethodType.RequiredStatus, List<String>> getRequiredValues( TextPattern pattern );

    boolean validate( TextPattern pattern, String text );

    TextPattern getTextPattern( TrackedEntityAttribute attribute )
        throws Exception;
}

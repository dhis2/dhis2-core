package org.hisp.dhis.textpattern;

import com.google.common.collect.ImmutableMap;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultTextPatternService
    implements TextPatternService
{
    @Override
    public String resolvePattern( TextPattern pattern, Map<String, String> values )
    {
        StringBuilder resolvedPattern = new StringBuilder();

        for ( TextPattern.Segment segment : pattern.getSegments() )
        {
            TextPatternMethod method = segment.getMethod();
            String value = segment.getFormat();

            if ( !method.isText() )
            {
                if ( method.hasTextFormat() )
                {
                    resolvedPattern.append(
                        TextPatternMethodUtils
                            .formatText( method.getType().getParam( value ),
                                values.getOrDefault( method.name(), "" ) ) );
                }
                else if ( method.hasDateFormat() )
                {
                    if ( method.equals( TextPatternMethod.CURRENT_DATE ) )
                    {
                        resolvedPattern.append(
                            (new SimpleDateFormat( Objects.requireNonNull( method.getType().getParam( value ) ) ))
                                .format( new Date() )
                        );
                    }
                }
                else if ( method.equals( TextPatternMethod.RANDOM ) )
                {
                    resolvedPattern.append( TextPatternMethodUtils.generateRandom(
                        Objects.requireNonNull( method.getType().getParam( value ) ) ) );
                }
                else
                {
                    resolvedPattern.append( values.getOrDefault( method.name(), "" ) );
                }
            }
            else
            {
                resolvedPattern.append( method.getType().getParam( value ) );
            }

        }

        return resolvedPattern.toString();
    }

    @Override
    public Map<String, List<String>> getRequiredValues( TextPattern pattern )
    {
        return ImmutableMap.<String, List<String>>builder()
            .put( REQUIRED, pattern.getSegments().stream()
                .filter( ( segment ) -> segment.getType().isRequired() )
                .map( ( segment ) -> segment.getMethod().name() )
                .collect( Collectors.toList() ) )
            .put( OPTIONAL, pattern.getSegments().stream()
                .filter( ( segment ) -> segment.getType().isOptional() )
                .map( ( segment ) -> segment.getMethod().name() )
                .collect( Collectors.toList() ) )
            .build();
    }
}

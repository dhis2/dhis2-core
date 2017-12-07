package org.hisp.dhis.textpattern;

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hisp.dhis.textpattern.MethodType.RequiredStatus;

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
    public Map<RequiredStatus, List<String>> getRequiredValues( TextPattern pattern )
    {
        List<String> required = pattern
            .getSegments()
            .stream()
            .filter( ( segment ) -> segment
                .getType()
                .isRequired() )
            .map( ( segment ) -> segment
                .getMethod()
                .name() )
            .collect( Collectors.toList() );

        List<String> optional = pattern
            .getSegments()
            .stream()
            .filter( ( segment ) -> segment
                .getType()
                .isOptional() )
            .map( ( segment ) -> segment
                .getMethod()
                .name() )
            .collect( Collectors.toList() );

        return ImmutableMap.<RequiredStatus, List<String>>builder()
            .put( RequiredStatus.REQUIRED, required )
            .put( RequiredStatus.OPTIONAL, optional )
            .build();
    }

    @Override
    public boolean validate( TextPattern pattern, String text )
    {
        return pattern.validateText( text );
    }

    @Override
    public TextPattern getTextPattern( TrackedEntityAttribute attribute )
        throws TextPatternParser.TextPatternParsingException
    {
        if ( attribute.getTextPattern() == null && attribute.isGenerated() )
        {
            attribute.setTextPattern( TextPatternParser.parse( attribute.getPattern() ) );
        }

        return attribute.getTextPattern();
    }
}

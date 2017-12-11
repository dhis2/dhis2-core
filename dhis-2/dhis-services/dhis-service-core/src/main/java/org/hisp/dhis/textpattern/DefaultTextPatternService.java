package org.hisp.dhis.textpattern;

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hisp.dhis.textpattern.MethodType.RequiredStatus;

public class DefaultTextPatternService
    implements TextPatternService
{
    @Override
    public String resolvePattern( TextPattern pattern, Map<String, String> values )
        throws Exception
    {
        StringBuilder resolvedPattern = new StringBuilder();

        for ( TextPattern.Segment segment : pattern.getSegments() )
        {
            if ( segment.isRequired() )
            {
                resolvedPattern.append( handleRequiredValue( segment, values.get( segment.getSegment() ) ) );
            }
            else if ( segment.isOptional() )
            {
                resolvedPattern.append( handleOptionalValue( segment, values.get( segment.getSegment() ) ) );
            }
            else
            {
                resolvedPattern.append( handleFixedValues( segment ) );
            }
        }

        return resolvedPattern.toString();
    }

    private String handleFixedValues( TextPattern.Segment segment )
    {
        if ( TextPatternMethod.CURRENT_DATE.getType().validatePattern( segment.getSegment() ) )
        {
            return new SimpleDateFormat( segment.getParameter() ).format( new Date() );
        }
        else
        {
            return segment.getParameter();
        }
    }

    @Override
    public Map<RequiredStatus, List<String>> getRequiredValues( TextPattern pattern )
    {
        return ImmutableMap.<RequiredStatus, List<String>>builder()
            .put( RequiredStatus.REQUIRED, pattern.getSegments()
                .stream()
                .filter( TextPattern.Segment::isRequired )
                .map( TextPattern.Segment::getSegment )
                .collect( Collectors.toList() ) )
            .put( RequiredStatus.OPTIONAL, pattern.getSegments()
                .stream()
                .filter( TextPattern.Segment::isOptional )
                .map( TextPattern.Segment::getSegment )
                .collect( Collectors.toList() ) )
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

    private String handleOptionalValue( TextPattern.Segment segment, String value )
        throws Exception
    {
        if ( value != null && !segment.validateValue( value ) )
        {
            throw new Exception( "Supplied optional value is invalid" );
        }
        else if ( value != null )
        {
            return getFormattedValue( segment, value );
        }
        else
        {
            if ( segment.getType() instanceof GeneratedMethodType )
            {
                // Put parameter as placeholder, this will be handled after the rest have been resolved
                return segment.getParameter();
            }
            else
            {
                throw new Exception( "Trying to generate unknown segment: '" + segment.getSegment() + "'" );
            }
        }
    }

    private String handleRequiredValue( TextPattern.Segment segment, String value )
        throws Exception
    {
        if ( value == null )
        {
            throw new Exception( "Missing required value" );
        }

        String res = getFormattedValue( segment, value );

        if ( res == null || !segment.validateValue( res ) )
        {
            throw new Exception( "Value is invalid" );
        }

        return res;
    }

    private String getFormattedValue( TextPattern.Segment segment, String value )
    {
        MethodType methodType = segment.getType();

        return methodType.getFormattedText( segment.getParameter(), value );
    }
}

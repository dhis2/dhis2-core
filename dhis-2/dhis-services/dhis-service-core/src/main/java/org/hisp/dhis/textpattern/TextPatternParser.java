package org.hisp.dhis.textpattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextPatternParser
{
    private static final String METHOD_REGEX = "(?<MethodName>[A-Z_]+?)\\(.*?\\)";

    private static final String JOIN_REGEX = "(?<Join>[\\s]*(?<JoinValue>\\+)[\\s]*)";

    private static final String TEXT_REGEX = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"";

    private static final Pattern EXPRESSION_REGEX = Pattern.compile(
        String.format( "[\\s]*(?<Segment>(?<Method>%s|%s)|%s)+?[\\s]*", TEXT_REGEX, METHOD_REGEX, JOIN_REGEX )
    );

    /**
     * Parses an expression, identifying segments and builds an IDExpression.
     * throws exception if syntax is invalid
     *
     * @param pattern the expression to parse
     * @return IDExpression representing the expression
     */
    public static TextPattern parse( String pattern )
        throws TextPatternParsingException
    {
        List<TextPatternSegment> segments = new ArrayList<>();

        // True if we just parsed a Segment, False if we parsed a join or haven't parsed anything.
        boolean segment = false;

        // You can have multiple random segments, but not combined with sequence
        // You can only have a single sequence and no random
        boolean sequenceSegmentPresent = false;
        boolean randomSegmentPresent = false;

        boolean invalidExpression = true;

        Matcher m;

        if ( pattern != null && !pattern.isEmpty() )
        {
            m = EXPRESSION_REGEX.matcher( pattern );
        }
        else
        {
            throw new TextPatternParsingException( "Supplied expression was null or empty.", -1 );
        }

        /*
         * We go trough all matches. Matches can be one of the following:
         * a TEXT method ("..")
         * any TextPatternMethod (Exluding TEXT) (method(param))
         * a join ( + )
         *
         * Matches that are invalid includes methods with unknown method names
         */
        while ( m.find() )
        {
            invalidExpression = false;

            // This returns the entire method syntax, including params
            String method = m.group( "Method" );

            // This means we found a match for method syntax
            if ( method != null )
            {

                // This returns only the name of the method (see TextPatternMethod for valid names)
                String methodName = m.group( "MethodName" );

                // This means we encountered the syntax for TEXT method
                if ( methodName == null ) // Text
                {

                    // Only add if valid syntax, else it will throw exception after if-else.
                    if ( TextPatternMethod.TEXT.getType().validatePattern( method ) )
                    {
                        segment = true;
                        segments.add( new TextPatternSegment( TextPatternMethod.TEXT, method ) );
                        continue;
                    }

                }

                // Catch all other methods
                else
                {

                    // Attempt to find a matching method name in TextPatternMethod
                    try
                    {
                        TextPatternMethod textPatternMethod = TextPatternMethod.valueOf( methodName );
                        // Only add if valid syntax, else it will throw exception after if-else.
                        if ( textPatternMethod.getType().validatePattern( method ) )
                        {

                            if ( textPatternMethod.getType() instanceof GeneratedMethodType )
                            {

                                // Sequence method can only appear once, and only when no Random method is present
                                if ( textPatternMethod.equals( TextPatternMethod.SEQUENTIAL ) &&
                                    !randomSegmentPresent &&
                                    !sequenceSegmentPresent )
                                {
                                    sequenceSegmentPresent = true;
                                }
                                else if ( textPatternMethod.equals( TextPatternMethod.RANDOM ) &&
                                    !sequenceSegmentPresent )
                                {
                                    randomSegmentPresent = true;
                                }
                                else
                                {
                                    throw new TextPatternParsingException(
                                        "Pattern can not contain multiple sequence methods or both sequence and random methods: '" +
                                            pattern + "'", m.start( "Segment" ) );
                                }
                            }

                            segment = true;
                            segments.add( new TextPatternSegment( textPatternMethod, method ) );
                            continue;
                        }

                    }
                    catch ( Exception e )
                    {
                        // Ignore, throw exception after if-else if we get here.
                    }
                }

                // If we are here, that means we found no matching methods, so throw an exception
                throw new TextPatternParsingException( "Failed to parse the following method: '" + method + "'",
                    m.start( "Method" ) );

            }

            // Handle Join
            else if ( m.group( "Join" ) != null )
            {
                // Join should only be after a Segment
                if ( !segment )
                {
                    throw new TextPatternParsingException( "Unexpected '+'", m.start( "JoinValue" ) );
                }
                else
                {
                    segment = false;
                }

            }
            else
            {
                throw new TextPatternParsingException( "Unknown input: '" + m.group( "Segment" ) + "'", -1 );
            }
        }

        // If the matcher had no matches
        if ( invalidExpression )
        {
            throw new TextPatternParsingException( "The expression is invalid", -1 );
        }

        // An expression should not end on a Join
        if ( !segment )
        {
            throw new TextPatternParsingException( "Unexpected '+' at the end of the expression", -1 );
        }

        return new TextPattern( segments );
    }

    public static class TextPatternParsingException
        extends Exception
    {
        TextPatternParsingException( String message, int position )
        {
            super(
                "Could not parse expression: " + message + (position != -1 ? " at position " + (position + 1) : "") );
        }
    }

}

package org.hisp.dhis.idgenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDExpressionParser
{
    private static final String METHOD_REGEX = "^\\s*(?<Method>(?<MethodName>[A-Z_]+?\\((?<MethodParam>.*?)\\)))";

    private static final String JOIN_REGEX = "(?<Join>[\\s]*(?<JoinValue>\\+)[\\s]*)";

    private static final String TEXT_REGEX = "(?<TextSegment>\"(?<TextSegmentValue>[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\")";

    private static final String RANDOM_REGEX = "(?<RandomSegment>RANDOM\\((?<RandomSegmentValue>[#Xx]+?)\\))";

    private static final String SEQUENTIAL_REGEX = "(?<SequentialSegment>SEQUENTIAL\\((?<SequentialSegmentValue>#+?)\\))";

    private static final Pattern EXPRESSION_REGEX = Pattern.compile(
        String.format( "(?:(?<Segment>(?:%s|%s|%s))| %s)+?", TEXT_REGEX, RANDOM_REGEX, SEQUENTIAL_REGEX, JOIN_REGEX )
    );

    /**
     * Parses an expression, identifying segments and builds an IDExpression.
     * throws exception if syntax is invalid
     *
     * @param expression the expression to parse
     * @return IDExpression representing the expression
     */
    public static IDExpression parse( String expression )
        throws IDExpressionParseException
    {
        IDExpression result = new IDExpression();

        // True if we just parsed a Segment, False if we parsed a join or haven't parsed anything.
        boolean segment = false;

        // You can have multiple random segments, but not combined with sequence
        // You can only have a single sequence and no random
        boolean sequenceSegmentPresent = false;
        boolean randomSegmentPresent = false;

        boolean invalidExpression = true;

        Matcher m;

        if ( expression != null && !expression.isEmpty() )
        {
            m = EXPRESSION_REGEX.matcher( expression );
        }
        else
        {
            throw new IDExpressionParseException( "Supplied expression was null or empty.", -1 );
        }

        while ( m.find() )
        {
            invalidExpression = false;

            // Handle Segment
            if ( m.group( "Segment" ) != null )
            {
                segment = true;

                if ( m.group( "TextSegment" ) != null )
                {
                    result.addSimpleSegment( m.group( "TextSegment" ), IDExpressionType.TEXT );
                }
                else if ( m.group( "RandomSegment" ) != null )
                {
                    if ( !sequenceSegmentPresent )
                    {
                        randomSegmentPresent = true;
                        result.addSimpleSegment( m.group( "RandomSegment" ), IDExpressionType.RANDOM );
                    }
                    else
                    {
                        throw new IDExpressionParseException(
                            "You can't have both RANDOM and SEQUENCE in the same expression", m.start() );
                    }
                }
                else if ( m.group( "SequentialSegment" ) != null )
                {
                    if ( !sequenceSegmentPresent && !randomSegmentPresent )
                    {
                        sequenceSegmentPresent = true;
                        result.addSimpleSegment( m.group( "SequentialSegment" ), IDExpressionType.SEQUENTIAL );
                    }
                    else
                    {
                        throw new IDExpressionParseException(
                            "You can only have random or one sequence segment in your expression", m.start() );
                    }
                }
                else
                {
                    throw new IDExpressionParseException( "Unknown input: " + m.start() + ", " + m.end(), -1 );
                }

            }

            // Handle Join
            else if ( m.group( "Join" ) != null )
            {
                // Join should only be after a Segment
                if ( !segment )
                {
                    throw new IDExpressionParseException( "Unexpected '+'", m.start( "JoinValue" ) );
                }
                else
                {
                    segment = false;
                }

            }
            else
            {
                throw new IDExpressionParseException( "Unknown input: " + m.start() + ", " + m.end(), -1 );
            }
        }

        // If the matcher had no matches
        if ( invalidExpression )
        {
            throw new IDExpressionParseException( "The expression is invalid", -1 );
        }

        // An expression should not end on a Join
        if ( !segment )
        {
            throw new IDExpressionParseException( "Unexpected '+' at the end of the expression", -1 );
        }

        return result;
    }

    static class IDExpressionParseException
        extends Exception
    {
        IDExpressionParseException( String message, int position )
        {
            super(
                "Could not parse expression: " + message + (position != -1 ? " at position " + (position + 1) : "") );
        }
    }

}

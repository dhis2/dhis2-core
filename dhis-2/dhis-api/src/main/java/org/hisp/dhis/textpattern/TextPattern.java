package org.hisp.dhis.textpattern;

import com.sun.javafx.binding.StringFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a TextPattern - A String that is used to generate and validate a user-defined patterns.
 * Example pattern:
 * "Current date: " + CURRENT_DATE("DD-MM-yyyy")
 * <p>
 * Please read more about patterns in TextPatternMethod.
 *
 * @author Stian Sandvold
 */
public class TextPattern
{
    private ArrayList<Segment> segments;

    private Pattern valueRegex;

    TextPattern()
    {
        this.segments = new ArrayList<>();
    }

    /**
     * This class represents a segment of a pattern, which is equivalent to a TextPatternMethod.
     * It holds information about the format (param from the original String) and the type of method
     * Includes some helper methods for matching a value against the format and type of the segment.
     */
    class Segment
    {
        private String format;

        private TextPatternMethod method;

        Segment( String format, TextPatternMethod method )
        {
            this.format = format;
            this.method = method;
        }

        public String getFormat()
        {
            return format;
        }

        public MethodType getType()
        {
            return method.getType();
        }

        public TextPatternMethod getMethod()
        {
            return method;
        }

        /**
         * Returns a String that matches against text that conforms to the format.
         *
         * @return a regex String
         */
        public String getValueRegex()
        {
            return getType().getValueRegex( format );
        }

        /**
         * Returns true if the text matches the segments format
         *
         * @param text the text to match against
         * @return true if it matches, false if not.
         */
        public boolean validateValue( String text )
        {
            return getType().validateText( format, text );
        }
    }

    /**
     * Adds a new Segment to the TextPattern.
     *
     * @param format the format of the segment
     * @param method the method of the segment
     */
    public void addSegment( String format, TextPatternMethod method )
    {
        this.segments.add( new Segment( format, method ) );
    }

    /**
     * Uses a Pattern based on all the segments of the TextPattern to validate the syntax of the
     * input text
     *
     * @param text the text to validate
     * @return true if the text is valid, false if it does not match the same syntax
     */
    public boolean validateText( String text )
    {
        if ( valueRegex == null )
        {
            valueRegex = createValueRegex();
        }

        Matcher m = valueRegex.matcher( text );

        if ( m.matches() )
        {
            for ( int i = 0; i < segments.size(); i++ )
            {
                if ( !segments.get( i ).validateValue( m.group( i ) ) )
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }

        return true;

    }

    /**
     * @return the Segments of the TextPattern
     */
    public List<Segment> getSegments()
    {
        return segments;
    }

    // Helper methods

    /**
     * Creates a new ValueRegex (A Regex that will match the entire TextPattern against a text)
     *
     * @return a Pattern representing the TextPattern
     */
    private Pattern createValueRegex()
    {
        StringBuilder regex = new StringBuilder( "^" );

        for ( int i = 0; i < segments.size(); i++ )
        {
            regex.append( StringFormatter.format( "(%s)", segments.get( 0 ).getValueRegex() ) );
        }

        regex.append( "$" );

        return Pattern.compile( regex.toString() );
    }

}

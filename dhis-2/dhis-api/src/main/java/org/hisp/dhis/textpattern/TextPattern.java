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

    private String ownerUID;

    private Pattern valueRegex;

    TextPattern()
    {
        this.segments = new ArrayList<>();
    }

    public void setOwnerUID( String ownerUID )
    {
        this.ownerUID = ownerUID;
    }

    public String getOwnerUID()
    {
        return ownerUID;
    }

    /**
     * This class represents a segment of a pattern, which is equivalent to a TextPatternMethod.
     * It holds information about the format (param from the original String) and the type of method
     * Includes some helper methods for matching a value against the format and type of the segment.
     */
    class Segment
    {
        /* The full String of the segment */
        private String segment;

        /* The method type */
        private MethodType methodType;

        /* The parameter */
        private String parameter;

        Segment( String segment, MethodType methodType )
        {
            this.segment = segment;
            this.methodType = methodType;
            this.parameter = methodType.getParam( segment );
        }

        public String getSegment()
        {
            return segment;
        }

        public MethodType getType()
        {
            return methodType;
        }

        public String getParameter()
        {
            return parameter;
        }

        /**
         * Returns a String that matches against text that conforms to the format.
         *
         * @return a regex String
         */
        private String getValueRegex()
        {
            return methodType.getValueRegex( parameter );
        }

        /**
         * Returns true if the text matches the segments format
         *
         * @param text the text to match against
         * @return true if it matches, false if not.
         */
        public boolean validateValue( String text )
        {
            return methodType.validateText( getValueRegex(), text );
        }

        public boolean isRequired()
        {
            return methodType.isRequired();
        }

        public boolean isOptional()
        {
            return methodType.isOptional();
        }
    }

    /**
     * Adds a new Segment to the TextPattern.
     *
     * @param segment    the format of the segment
     * @param methodType the method type of the segment
     */
    public void addSegment( String segment, MethodType methodType )
    {
        this.segments.add( new Segment( segment, methodType ) );
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

        for ( Segment segment : segments )
        {
            regex.append( StringFormatter.format( "(%s)", segment.getValueRegex() ) );
        }

        regex.append( "$" );

        return Pattern.compile( regex.toString() );
    }

}

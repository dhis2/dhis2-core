package org.hisp.dhis.textpattern;

public class TextPatternValidationUtils
{

    public static boolean validateSegmentValue( TextPatternSegment segment, String value )
    {
        return segment.getMethod().getType().validateText( segment.getParameter(), value );
    }

    public static boolean validateTextPatternValue( TextPattern textPattern, String value )
    {
        // TODO!
        return false;
    }

    public static int getTotalValuesPotential( TextPatternSegment generatedSegment )
    {

        if ( generatedSegment != null )
        {

            if ( generatedSegment.getMethod().equals( TextPatternMethod.SEQUENTIAL ) )
            {
                return (int) Math.pow( 10, generatedSegment.getParameter().length() );
            }
            else if ( generatedSegment.getMethod().equals( TextPatternMethod.RANDOM ) )
            {
                int res = 1;

                for ( char c : generatedSegment.getParameter().toCharArray() )
                {
                    switch ( c )
                    {
                    case '#':
                        res = res * 10;
                        break;
                    case 'X':
                        res = res * 26;
                        break;
                    case 'x':
                        res = res * 26;
                        break;
                    default:
                        break;
                    }
                }

                return res;
            }
        }

        return 1;
    }
}

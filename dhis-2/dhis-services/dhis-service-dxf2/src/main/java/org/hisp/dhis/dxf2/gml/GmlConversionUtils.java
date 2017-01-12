package org.hisp.dhis.dxf2.gml;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.math3.util.Precision;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Methods to parse various GML coordinate formats and output GeoJSON strings.
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
public class GmlConversionUtils
{
    private static final NumberFormat NF = NumberFormat.getInstance( Locale.ENGLISH );

    /**
     * Parses a gml:coordinates element and outputs a GeoJSON string.
     *
     * @param coordinates contents of gml:coordinates element to parse.
     * @param precision decimal precision to use in output.
     * @return a string representation of the coordinates.
     * @throws ParseException
     */
    public static String gmlCoordinatesToString( String coordinates, String precision )
        throws ParseException
    {
        int nDecimals = Integer.parseInt( precision );

        StringBuilder sb = new StringBuilder();

        for ( String coordinate : coordinates.trim().split( "\\s" ) )
        {
            String[] point = coordinate.split( "," );

            String lat = parseCoordinate( point[0], nDecimals, NF ),
                   lon = parseCoordinate( point[1], nDecimals, NF );

            sb.append( "[" ).append( lat ).append( "," ).append( lon ).append( "]," );
        }

        return sb.length() > 0 ? sb.deleteCharAt( sb.length() - 1 ).toString() : "";
    }

    /**
     * Parses a gml:pos element and outputs a GeoJSON string.
     *
     * @param pos contents of gml:pos element to parse.
     * @param precision decimal precision to use in output.
     * @return a string representation of the point.
     * @throws ParseException
     */
    public static String gmlPosToString( String pos, String precision )
        throws ParseException
    {
        int nDecimals = Integer.parseInt( precision );

        String[] c = pos.trim().split( "\\s", 2 );

        if( c.length != 2 )
        {
            return "";
        }

        String lat = parseCoordinate( c[0], nDecimals, NF ),
               lon = parseCoordinate( c[1], nDecimals, NF );

        return "[" + lat + "," + lon + "]";
    }

    /**
     * Parses a gml:posList element and outputs a GeoJSON string.
     *
     * @param posList contents of gml:posList element to parse.
     * @param precision decimal precision to use in output.
     * @return a string representation of the posList.
     * @throws ParseException
     */
    public static String gmlPosListToString( String posList, String precision )
        throws ParseException
    {
        int nDecimals = Integer.parseInt( precision );

        StringBuilder sb = new StringBuilder();

        String[] c = posList.trim().split( "\\s" );

        if( c.length % 2 != 0 )
        {
            return ""; // Badly formed gml:posList
        }

        for( int i = 0 ; i <  c.length ; i += 2 )
        {
            String lat = parseCoordinate( c[i], nDecimals, NF ),
                   lon = parseCoordinate( c[i + 1], nDecimals, NF );

            sb.append( "[" ).append( lat ).append(",").append( lon ).append( "]," );
        }

        return sb.length() > 0 ? sb.deleteCharAt( sb.length() - 1 ).toString() : "";
    }

    private static String parseCoordinate( String number, int precision, NumberFormat nf )
        throws ParseException
    {
        return Double.toString( Precision.round(nf.parse(number).doubleValue(), precision ) );
    }
}

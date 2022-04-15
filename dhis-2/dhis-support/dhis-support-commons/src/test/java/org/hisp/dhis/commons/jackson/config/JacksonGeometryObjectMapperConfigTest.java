/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.commons.jackson.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonGeometryObjectMapperConfigTest
{
    private static final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    private static final GeometryFactory gf = new GeometryFactory();

    @ParameterizedTest
    @MethodSource( "data" )
    void testLineStringSerialization( String expectedString, Geometry geometry )
        throws JsonProcessingException
    {
        String actualString = jsonMapper.writeValueAsString( geometry );
        assertEquals( expectedString, actualString );
    }

    @ParameterizedTest
    @MethodSource( "data" )
    void testLineStringDeserialization( String geometryJson, Geometry expectedGeometry )
        throws JsonProcessingException
    {
        Geometry actualGeometry = jsonMapper.readValue( geometryJson, Geometry.class );
        assertEquals( expectedGeometry, actualGeometry );
    }

    private static Stream<Arguments> data()
    {
        return Stream.of( arguments( lineString(), lineStringGeometry() ),
            arguments( multiLineString(), multiLineStringGeometry() ),
            arguments( point(), pointGeometry() ),
            arguments( multiPoint(), multiPointGeometry() ),
            arguments( polygon(), polygonGeometry() ),
            arguments( polygonWithHoles(), polygonWithHolesGeometry() ),
            arguments( multiPolygon(), multiPolygonGeometry() ) );
    }

    private static String lineString()
    {
        return "{\"type\":\"LineString\",\"coordinates\":[[100.0,0.0],[101.0,1.0]]}";
    }

    private static LineString lineStringGeometry()
    {
        return gf.createLineString( new Coordinate[] { new Coordinate( 100.0, 0.0 ), new Coordinate( 101.0, 1.0 ) } );
    }

    private static String multiLineString()
    {
        return "{\"type\":\"MultiLineString\",\"coordinates\":[[[100.0,0.0],[101.0,1.0]],[[102.0,2.0],[103.0,3.0]]]}";
    }

    private static MultiLineString multiLineStringGeometry()
    {
        return gf
            .createMultiLineString( new LineString[] {
                gf.createLineString( new Coordinate[] {
                    new Coordinate( 100.0, 0.0 ),
                    new Coordinate( 101.0, 1.0 ) } ),
                gf.createLineString( new Coordinate[] {
                    new Coordinate( 102.0, 2.0 ),
                    new Coordinate( 103.0, 3.0 ) } ) } );
    }

    private static String multiPoint()
    {
        return "{\"type\":\"MultiPoint\",\"coordinates\":[[1.2345678,2.3456789]]}";
    }

    private static MultiPoint multiPointGeometry()
    {
        return gf.createMultiPoint( new Point[] { gf
            .createPoint( new Coordinate( 1.2345678, 2.3456789 ) ) } );
    }

    private static String multiPolygon()
    {
        return "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]]]]}";
    }

    private static MultiPolygon multiPolygonGeometry()
    {
        LinearRing shell = gf.createLinearRing( new Coordinate[] {
            new Coordinate( 102.0, 2.0 ), new Coordinate( 103.0, 2.0 ),
            new Coordinate( 103.0, 3.0 ), new Coordinate( 102.0, 3.0 ),
            new Coordinate( 102.0, 2.0 ) } );
        return gf.createMultiPolygon( new Polygon[] { gf
            .createPolygon( shell, null ) } );
    }

    private static String point()
    {
        return "{\"type\":\"Point\",\"coordinates\":[1.2345678,2.3456789]}";
    }

    private static Point pointGeometry()
    {
        return gf.createPoint( new Coordinate( 1.2345678, 2.3456789 ) );
    }

    private static String polygon()
    {
        return "{\"type\":\"Polygon\",\"coordinates\":[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]]]}";
    }

    private static Polygon polygonGeometry()
    {
        LinearRing shell = gf.createLinearRing( new Coordinate[] {
            new Coordinate( 102.0, 2.0 ), new Coordinate( 103.0, 2.0 ),
            new Coordinate( 103.0, 3.0 ), new Coordinate( 102.0, 3.0 ),
            new Coordinate( 102.0, 2.0 ) } );
        LinearRing[] holes = new LinearRing[0];
        return gf.createPolygon( shell, holes );
    }

    private static String polygonWithHoles()
    {
        return "{\"type\":\"Polygon\",\"coordinates\":[[[102.0,2.0],[103.0,2.0],[103.0,3.0],[102.0,3.0],[102.0,2.0]],[[100.2,0.2],[100.8,0.2],[100.8,0.8],[100.2,0.8],[100.2,0.2]]]}";
    }

    private static Polygon polygonWithHolesGeometry()
    {
        LinearRing shell = gf.createLinearRing( new Coordinate[] {
            new Coordinate( 102.0, 2.0 ), new Coordinate( 103.0, 2.0 ),
            new Coordinate( 103.0, 3.0 ), new Coordinate( 102.0, 3.0 ),
            new Coordinate( 102.0, 2.0 ) } );
        LinearRing[] holes = new LinearRing[] { gf
            .createLinearRing( new Coordinate[] {
                new Coordinate( 100.2, 0.2 ), new Coordinate( 100.8, 0.2 ),
                new Coordinate( 100.8, 0.8 ), new Coordinate( 100.2, 0.8 ),
                new Coordinate( 100.2, 0.2 ) } ) };
        return gf.createPolygon( shell, holes );
    }
}

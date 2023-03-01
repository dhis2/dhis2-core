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
package org.hisp.dhis.organisationunit;

import static org.hisp.dhis.organisationunit.FeatureType.MULTI_POLYGON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.geotools.geojson.geom.GeometryJSON;
import org.hisp.dhis.common.coordinate.CoordinateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Lars Helge Overland
 */
class OrganisationUnitTest
{

    private List<CoordinatesTuple> multiPolygonCoordinatesList = new ArrayList<>();

    private List<CoordinatesTuple> pointCoordinatesList = new ArrayList<>();

    private String multiPolygonCoordinates = "[[[[11.11,22.22],[33.33,44.44],[55.55,66.66],[11.11,22.22]]],"
        + "[[[77.77,88.88],[99.99,11.11],[22.22,33.33],[77.77,88.88]]],"
        + "[[[44.44,55.55],[66.66,77.77],[88.88,99.99],[44.44,55.55]]]]";

    private CoordinatesTuple tupleA;

    private CoordinatesTuple tupleB;

    private CoordinatesTuple tupleC;

    private CoordinatesTuple tupleD;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private OrganisationUnit unitD;

    private GeometryJSON geometryJSON = new GeometryJSON();

    @BeforeEach
    void before()
    {
        tupleA = new CoordinatesTuple();
        tupleA.addCoordinates( "11.11,22.22" );
        tupleA.addCoordinates( "33.33,44.44" );
        tupleA.addCoordinates( "55.55,66.66" );
        tupleB = new CoordinatesTuple();
        tupleB.addCoordinates( "77.77,88.88" );
        tupleB.addCoordinates( "99.99,11.11" );
        tupleB.addCoordinates( "22.22,33.33" );
        tupleC = new CoordinatesTuple();
        tupleC.addCoordinates( "44.44,55.55" );
        tupleC.addCoordinates( "66.66,77.77" );
        tupleC.addCoordinates( "88.88,99.99" );
        tupleD = new CoordinatesTuple();
        tupleD.addCoordinates( "11.11,22.22" );
        multiPolygonCoordinatesList.add( tupleA );
        multiPolygonCoordinatesList.add( tupleB );
        multiPolygonCoordinatesList.add( tupleC );
        pointCoordinatesList.add( tupleD );
        unitA = new OrganisationUnit( "OrgUnitA" );
        unitB = new OrganisationUnit( "OrgUnitB" );
        unitC = new OrganisationUnit( "OrgUnitC" );
        unitD = new OrganisationUnit( "OrgUnitD" );
        unitA.setUid( "e8iHpRVptzA" );
        unitB.setUid( "e8iHpRVptzB" );
        unitC.setUid( "e8iHpRVptzC" );
        unitD.setUid( "e8iHpRVptzD" );
    }

    @Test
    void testGetAncestors()
    {
        unitD.setParent( unitC );
        unitC.setParent( unitB );
        unitB.setParent( unitA );
        List<OrganisationUnit> expected = new ArrayList<>( Arrays.asList( unitA, unitB, unitC ) );
        assertEquals( expected, unitD.getAncestors() );
    }

    @Test
    void testGetAncestorNames()
    {
        unitD.setParent( unitC );
        unitC.setParent( unitB );
        unitB.setParent( unitA );
        List<String> expected = new ArrayList<>(
            Arrays.asList( unitA.getDisplayName(), unitB.getDisplayName(), unitC.getDisplayName() ) );
        assertEquals( expected, unitD.getAncestorNames( null, false ) );
        expected = new ArrayList<>( Arrays.asList( unitA.getDisplayName(), unitB.getDisplayName(),
            unitC.getDisplayName(), unitD.getDisplayName() ) );
        assertEquals( expected, unitD.getAncestorNames( null, true ) );
    }

    @Test
    void testGetAncestorsWithRoots()
    {
        unitD.setParent( unitC );
        unitC.setParent( unitB );
        unitB.setParent( unitA );
        List<OrganisationUnit> roots = new ArrayList<>( Arrays.asList( unitB ) );
        List<OrganisationUnit> expected = new ArrayList<>( Arrays.asList( unitB, unitC ) );
        assertEquals( expected, unitD.getAncestors( roots ) );
    }

    @Test
    void testGetPath()
    {
        unitD.setParent( unitC );
        unitC.setParent( unitB );
        unitB.setParent( unitA );
        String expected = "/e8iHpRVptzA/e8iHpRVptzB/e8iHpRVptzC/e8iHpRVptzD";
        assertEquals( expected, unitD.getPath() );
    }

    @Test
    void testIsDescendant()
    {
        unitB.setParent( unitA );
        unitC.setParent( unitB );
        unitD.setParent( unitA );

        // Set path property directly to emulate persistence layer

        unitA.getPath();
        unitB.getPath();
        unitC.getPath();
        unitD.getPath();

        assertTrue( unitC.isDescendant( Set.of( unitB ) ) );
        assertTrue( unitC.isDescendant( Set.of( unitA ) ) );
        assertTrue( unitB.isDescendant( Set.of( unitB ) ) );
        assertTrue( unitC.isDescendant( Set.of( unitA, unitD ) ) );
        assertTrue( unitB.isDescendant( Set.of( unitA ) ) );
        assertTrue( unitB.isDescendant( Set.of( unitA, unitD ) ) );

        assertFalse( unitC.isDescendant( Set.of( unitD ) ) );
        assertFalse( unitB.isDescendant( Set.of( unitD ) ) );
        assertFalse( unitB.isDescendant( Set.of( unitC ) ) );
    }

    @Test
    void testGetParentNameGraph()
    {
        unitD.setParent( unitC );
        unitC.setParent( unitB );
        unitB.setParent( unitA );
        List<OrganisationUnit> roots = new ArrayList<>( Arrays.asList( unitB ) );
        String expected = "/OrgUnitB/OrgUnitC";
        assertEquals( expected, unitD.getParentNameGraph( roots, false ) );
        expected = "/OrgUnitA/OrgUnitB/OrgUnitC";
        assertEquals( expected, unitD.getParentNameGraph( null, false ) );
    }

    @Test
    void testGetCoordinatesAsCollection()
        throws IOException
    {
        OrganisationUnit unit = new OrganisationUnit();
        Geometry geometry = geometryJSON
            .read( "{\"type\":\"MultiPolygon\", \"coordinates\":" + multiPolygonCoordinates + "}" );
        unit.setGeometry( geometry );
        assertEquals( 3, CoordinateUtils.getCoordinatesAsList( unit.getCoordinates(), MULTI_POLYGON ).size() );
    }
}

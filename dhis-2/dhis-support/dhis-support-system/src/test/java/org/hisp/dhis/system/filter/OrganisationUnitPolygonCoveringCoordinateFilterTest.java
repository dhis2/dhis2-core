/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.system.filter;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.geotools.geojson.geom.GeometryJSON;
import org.hisp.dhis.TestResourceUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class OrganisationUnitPolygonCoveringCoordinateFilterTest
{

    private OrganisationUnit organisationUnit;

    private OrganisationUnitPolygonCoveringCoordinateFilter filter;

    private String DOWNTOWN_OSLO;

    @Before
    public void setUp()
        throws IOException
    {

        organisationUnit = new OrganisationUnit();

        DOWNTOWN_OSLO = TestResourceUtils.getFileContent( "gis/downtownOslo.json" );

    }

    @Test
    public void verifyFilterExcludesOrgUnitWithoutGeometry()
    {
        filter = new OrganisationUnitPolygonCoveringCoordinateFilter( 1, 1.1 );

        organisationUnit.setGeometry( null );

        assertFalse( filter.retain( organisationUnit ) );
    }

    @Test
    public void verifyFilterExcludesOrgUnitFallingOutsidePolygon()
        throws IOException
    {
        filter = new OrganisationUnitPolygonCoveringCoordinateFilter( 10.758833885192871, 59.91530028312405 );

        Geometry ouGeometry = new GeometryJSON().read( DOWNTOWN_OSLO );

        organisationUnit.setGeometry( ouGeometry );

        assertFalse( filter.retain( organisationUnit ) );
    }

    @Test
    public void verifyFilterIncludesOrgUnitFallingInsidePolygon()
        throws IOException
    {
        filter = new OrganisationUnitPolygonCoveringCoordinateFilter( 10.746517181396484, 59.91080384720672 );

        Geometry ouGeometry = new GeometryJSON().read( DOWNTOWN_OSLO );

        organisationUnit.setGeometry( ouGeometry );

        assertTrue( filter.retain( organisationUnit ) );
    }

}
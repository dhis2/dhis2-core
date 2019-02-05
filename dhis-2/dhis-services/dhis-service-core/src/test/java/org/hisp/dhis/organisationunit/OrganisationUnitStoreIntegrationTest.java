/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.IntegrationTestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;

/**
 * @author Luciano Fiandesio
 */
@org.junit.experimental.categories.Category( IntegrationTest.class )
public class OrganisationUnitStoreIntegrationTest extends IntegrationTestBase {
    @Autowired
    private OrganisationUnitStore organisationUnitStore;

    @Override
    public boolean emptyDatabaseAfterTest() {
        return false;
    }

    @Test
    public void verifyGetOrgUnitsWithinAGeoBox() throws IOException {

//        OrganisationUnit ouA = createOrganisationUnit( 'A', GeoUtils
//                .getGeometryFromCoordinatesAndType( FeatureType.POINT, "[27.685546874999996, 25.819671943904044]" ) );
//        OrganisationUnit ouB = createOrganisationUnit( 'B',
//                GeoUtils.getGeometryFromCoordinatesAndType( FeatureType.POINT, "[31.4813232421875, 12.345368032463298]" ) );
//        OrganisationUnit ouC = createOrganisationUnit( 'C',
//                GeoUtils.getGeometryFromCoordinatesAndType( FeatureType.POINT, "[22.67578125, 19.394067895396613]" ) );
//        OrganisationUnit ouD = createOrganisationUnit( 'D',
//                GeoUtils.getGeometryFromCoordinatesAndType( FeatureType.POINT, "[22.148437499999996, 12.345368032463298]" ) );
//
//        organisationUnitStore.save(ouA);
//        organisationUnitStore.save(ouB);
//        organisationUnitStore.save(ouC);
//        organisationUnitStore.save(ouD);
        double[] box = new double[]{9.449225866622845,-11.791776182569619,9.087572311212298,-12.155823817430383};

        List<OrganisationUnit> a = organisationUnitStore.getWithinCoordinateArea(box);
        System.out.println(a.size());
        OrganisationUnit ooo = a.get(0);
        System.out.println(getCoordinatesFromGeometry(ooo.getGeometry()));

    }
}

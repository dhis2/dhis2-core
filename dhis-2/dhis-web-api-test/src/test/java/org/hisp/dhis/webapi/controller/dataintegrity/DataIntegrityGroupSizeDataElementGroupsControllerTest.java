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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import java.util.Set;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for metadata integrity check for data element groups which have less
 * than two members.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_data_element_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeDataElementGroupsControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "data_element_groups_scarce";

    private static final String detailsIdType = "dataElementGroups";

    private String dataElementA;

    private String dataElementB;

    @Test
    void testDataElementGroupSizeTooLow()
    {

        setUpDataElements();

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name' : 'MCH', 'shortName': 'MCH' , " +
                    "'dataElements':[{'id':'" + dataElementA + "'},{'id': '" + dataElementB + "'}]}" ) );

        String dataElementGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name': 'ANC', 'shortName': 'ANC' , 'dataElements' : [{'id' : '" + dataElementB + "'}]}" ) );

        String dataElementGroupC = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name': 'Morbidity', 'shortName': 'Morbidity' }" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 66,
            Set.of( dataElementGroupB, dataElementGroupC ), Set.of( "ANC", "Morbidity" ), Set.of( "0", "1" ),
            true );

    }

    @Test
    void testDataElementGroupSizeOK()
    {
        setUpDataElements();

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name' : 'MCH', 'shortName': 'MCH' , " +
                    "'dataElements':[{'id':'" + dataElementA + "'},{'id': '" + dataElementB + "'}]}" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testDataElementGroupSizeRuns()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    void setUpDataElements()
    {
        dataElementA = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );

        dataElementB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER'," +
                    "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }" ) );
    }
}
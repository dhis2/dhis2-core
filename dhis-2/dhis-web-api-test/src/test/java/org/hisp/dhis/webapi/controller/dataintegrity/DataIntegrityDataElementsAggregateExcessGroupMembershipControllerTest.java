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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for metadata check which identifies data element group sets which have
 * duplicative data elements within one or more of their constituent groups.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_excess_groupset_membership.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsAggregateExcessGroupMembershipControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "data_elements_excess_groupset_membership";

    private String dataElementA;

    private String dataElementB;

    private String dataElementGroupB;

    private String dataElementGroupA;

    @Test
    void testDataElementNotInGroup()
    {

        setUpDataElements();

        dataElementGroupA = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name' : 'MCH', 'shortName': 'MCH' , " +
                    "'dataElements':[{'id':'" + dataElementA + "'},{'id': '" + dataElementB + "'}]}" ) );

        dataElementGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name': 'ANC', 'shortName': 'ANC' , 'dataElements' : [{'id' : '" + dataElementB + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroupSets",
                "{'name' : 'Maternal Health' , 'shortName': 'Maternal Health', 'dataDimension' : true, " +
                    "'dataElementGroups': [{'id': '" + dataElementGroupA + "'},{'id': '" +
                    dataElementGroupB + "'}]}" ) );

        assertHasDataIntegrityIssues( "data_elements_aggregate", check, 50, dataElementB, "ANC2", null,
            true );

    }

    @Test
    void testDataElementsInGroup()
    {
        setUpDataElements();

        dataElementGroupA = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name' : 'MCH', 'shortName': 'MCH' , " +
                    "'dataElements':[{'id':'" + dataElementA + "'}]}" ) );

        dataElementGroupB = assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroups",
                "{ 'name': 'ANC', 'shortName': 'ANC' , 'dataElements' : [{'id' : '" + dataElementB + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElementGroupSets",
                "{'name' : 'Maternal Health' , 'shortName': 'Maternal Health', 'dataDimension' : true, " +
                    "'dataElementGroups': [{'id': '" + dataElementGroupA + "'},{'id': '" +
                    dataElementGroupB + "'}]}" ) );

        assertHasNoDataIntegrityIssues( "data_elements_aggregate", check, true );

    }

    @Test
    void testDataElementsInGroupDivideByZero()
    {

        assertHasNoDataIntegrityIssues( "data_elements_aggregate", check, false );

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
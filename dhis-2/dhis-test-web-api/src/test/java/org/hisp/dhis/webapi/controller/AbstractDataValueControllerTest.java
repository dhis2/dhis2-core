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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;

abstract class AbstractDataValueControllerTest
    extends DhisControllerConvenienceTest
{
    protected String dataElementId;

    protected String orgUnitId;

    protected String categoryComboId;

    protected String categoryOptionComboId;

    @BeforeEach
    void setUp()
    {
        orgUnitId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        // add OU to users hierarchy
        assertStatus( HttpStatus.OK, POST( "/users/{id}/organisationUnits", getCurrentUser().getUid(),
            Body( "{'additions':[{'id':'" + orgUnitId + "'}]}" ) ) );
        JsonObject ccDefault = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 );
        categoryComboId = ccDefault.getString( "id" ).string();
        categoryOptionComboId = ccDefault.getArray( "categoryOptionCombos" ).getString( 0 ).string();
        dataElementId = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements/",
                "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', "
                    + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                    + "'categoryCombo': {'id': '" + categoryComboId + "'}}" ) );
    }

    /**
     * @return UID of the created {@link org.hisp.dhis.datavalue.DataValue}
     */
    protected final void addDataValue( String period, String value, String comment, boolean followup )
    {
        addDataValue( period, value, comment, followup, dataElementId, orgUnitId );
    }

    /**
     * @return UID of the created {@link org.hisp.dhis.datavalue.DataValue}
     */
    protected final void addDataValue( String period, String value, String comment, boolean followup,
        String dataElementId, String orgUnitId )
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
                dataElementId, period, orgUnitId, categoryOptionComboId, value, comment, followup ) );
    }
}

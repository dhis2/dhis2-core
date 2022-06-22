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

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClientUtils.substitutePlaceholders;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractDataValueControllerTest
    extends DhisControllerConvenienceTest
{
    protected String dataElementId;

    protected String orgUnitId;

    protected String categoryComboId;

    protected String categoryOptionComboId;

    @Autowired
    protected CurrentUserService currentUserService;

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
        dataElementId = addDataElement( "My data element", "DE1", ValueType.INTEGER, null );

        // Add the newly created org unit to the superuser's hierarchy
        OrganisationUnit unit = manager.get( orgUnitId );
        User user = userService.getUser( getSuperUser().getUid() );
        user.addOrganisationUnit( unit );
        userService.updateUser( user );

        switchToSuperuser();
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
            postNewDataValue( period, value, comment, followup, dataElementId, orgUnitId ) );
    }

    protected final String addDataElement( String name, String code, ValueType valueType, String optionSet )
    {
        return assertStatus( HttpStatus.CREATED, postNewDataElement( name, code, valueType, optionSet ) );
    }

    protected final HttpResponse postNewDataElement( String name, String code, ValueType valueType, String optionSet )
    {
        return POST( "/dataElements/",
            format( "{'name':'%s', 'shortName':'%s', 'code':'%s', 'valueType':'%s', "
                + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                + "'categoryCombo': {'id': '%s'},"
                + "'optionSet': %s"
                + "}", name, code, code, valueType, categoryComboId,
                optionSet == null ? "null" : "{'id':'" + optionSet + "'}" ) );
    }

    protected final HttpResponse postNewDataValue( String period, String value, String comment, boolean followup,
        String dataElementId, String orgUnitId )
    {
        return POST( "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
            dataElementId, period, orgUnitId, categoryOptionComboId, value, comment, followup );
    }

    protected final JsonArray getDataValues( String de, String pe, String ou )
    {
        return getDataValues( de, categoryOptionComboId, null, null, pe, ou );
    }

    protected final JsonArray getDataValues( String de, String co, String cc, String cp, String pe, String ou )
    {
        String url = substitutePlaceholders( "/dataValues?de={de}&co={co}&cc={cc}&cp={cp}&pe={pe}&ou={ou}",
            new Object[] { de, co, cc, cp, pe, ou } );
        return GET( url.replaceAll( "&[a-z]{2}=&", "&" ).replace( "&&", "&" ) ).content();
    }
}

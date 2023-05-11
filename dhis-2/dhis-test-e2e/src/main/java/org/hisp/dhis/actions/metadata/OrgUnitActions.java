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
package org.hisp.dhis.actions.metadata;

import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.JsonParserUtils;
import org.hisp.dhis.utils.DataGenerator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitActions
    extends RestApiActions
{
    public OrgUnitActions()
    {
        super( "/organisationUnits" );
    }

    /**
     * Sends POST request to create provided org unit
     *
     * @param orgUnit
     * @return
     */
    public ApiResponse post( final OrgUnit orgUnit )
    {
        JsonObject object = JsonParserUtils.toJsonObject( orgUnit );
        if ( orgUnit.getParent() != null )
        {
            JsonObject parent = new JsonObject();
            parent.addProperty( "id", orgUnit.getParent() );
            object.add( "parent", parent );
        }

        return super.post( object );
    }

    /***
     * Generates dummy org unit and sends POST request to create it.
     *
     * @return
     */
    public ApiResponse postDummyOrgUnit()
    {
        return post( generateDummy() );
    }

    public String create( final OrgUnit orgUnit )
    {
        ApiResponse response = post( orgUnit );

        response.validate().statusCode( 201 );

        return response.extractString( "response.uid" );
    }

    public OrgUnit generateDummy()
    {
        String randomString = DataGenerator.randomString();

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setName( "AutoTest OrgUnit" + randomString );
        orgUnit.setShortName( "AutoTest orgUnit short name " + randomString );
        orgUnit.setOpeningDate( "2017-09-11T00:00:00.000" );

        return orgUnit;
    }

    public JsonObject createOrgUnitBody()
    {
        return JsonParserUtils.toJsonObject( generateDummy() );
    }

    public String createOrgUnit()
    {
        return create( generateDummy() );
    }

    public String createOrgUnit( int level )
    {
        OrgUnit orgUnit = generateDummy();
        orgUnit.setLevel( level );

        return create( orgUnit );
    }

    public String createOrgUnitWithParent( String parentId )
    {
        OrgUnit orgUnit = generateDummy();

        orgUnit.setParent( parentId );

        return create( orgUnit );
    }

    public String createOrgUnitWithParent( String parentId, int level )
    {
        OrgUnit orgUnit = generateDummy();
        orgUnit.setLevel( level );
        orgUnit.setParent( parentId );

        return create( orgUnit );
    }

    public void addAttributeValue( String orgUnit, String attributeId, String attributeValue )
    {
        JsonObject orgUnitObj = this.get( orgUnit ).getBody();

        JsonObject attributeObj = new JsonObject();
        attributeObj.addProperty( "id", attributeId );

        JsonObject attributeValueObj = new JsonObject();
        attributeValueObj.addProperty( "value", attributeValue );
        attributeValueObj.add( "attribute", attributeObj );

        JsonArray attributeValues = orgUnitObj.getAsJsonArray( "attributeValues" );
        attributeValues.add( attributeValueObj );

        orgUnitObj.add( "attributeValue", attributeValues );

        this.update( orgUnit, orgUnitObj ).validate().statusCode( 200 );
    }
}

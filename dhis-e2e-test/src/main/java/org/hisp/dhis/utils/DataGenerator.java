/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis.utils;

import com.github.javafaker.Faker;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.schemas.SchemaProperty;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataGenerator
{
    private static Faker faker = new Faker();

    public static String randomString()
    {
        return RandomStringUtils.randomAlphabetic( 6 );
    }

    public static String randomEntityName()
    {
        return "AutoTest entity " + randomString();
    }

    public static String randomString( int length )
    {
        return RandomStringUtils.randomAlphabetic( length );
    }

    public static JsonElement generateRandomValueMatchingSchema( SchemaProperty property )
    {
        JsonElement jsonPrimitive;
        switch ( property.getPropertyType() )
        {
        case STRING:
            jsonPrimitive = new JsonPrimitive(
                generateStringByFieldName( property.getFieldName(), (int) property.getMin(), (int) property.getMax() ) );
            break;

        case DATE:
            Date date = faker.date().past( 1000, TimeUnit.DAYS );
            jsonPrimitive = new JsonPrimitive( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" ).format( date ) );
            break;

        case BOOLEAN:
            jsonPrimitive = new JsonPrimitive( String.valueOf( faker.bool().bool() ) );
            break;

        case CONSTANT:
            int randomConstant = faker.number().numberBetween( 0, property.getConstants().size() - 1 );
            jsonPrimitive = new JsonPrimitive( property.getConstants().get( randomConstant ) );
            break;

        case NUMBER:
            jsonPrimitive = new JsonPrimitive( faker.number().numberBetween( (int) property.getMin(), (int) property.getMax() ) );
            break;

        default:
            jsonPrimitive = new JsonPrimitive( "Conversion not defined." );
            break;

        }

        return jsonPrimitive;
    }

    private static String generateStringByFieldName( String fieldName, int minLength, int maxLength )
    {
        switch ( fieldName )
        {
        case "url":
            return faker.internet().url();

        case "cronExpression":
            return "* * * * * *";
        case "periodType":
            List<String> periodTypes = new RestApiActions( "/periodTypes" ).get().extractList( "periodTypes.name" );
            return periodTypes.get( faker.number().numberBetween( 0, periodTypes.size() - 1 ));
        }

        if ( minLength < 1 )
        {
            return faker.lorem().characters( 6 );
        }

        if (maxLength == minLength) {
            return faker.lorem().characters( maxLength );
        }

        return faker.lorem().characters( minLength, maxLength );
    }
}

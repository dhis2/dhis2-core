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
package org.hisp.dhis.helpers.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.JsonParserUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class JsonFileReader
    implements FileReader
{
    private JsonObject obj;

    public JsonFileReader( File file )
        throws IOException
    {
        byte[] content = Files.readAllBytes( Paths.get( file.getPath() ) );

        String json = new String( content );

        obj = new JsonParser().parse( json ).getAsJsonObject();
    }

    public JsonFileReader read( File file )
        throws IOException
    {
        return new JsonFileReader( file );
    }

    @Override
    public FileReader replacePropertyValuesWithIds( String propertyName )
    {
        return replacePropertyValuesWith( propertyName, "uniqueid" );
    }

    /***
     * Replaces all occurrences of a string with unique generated id
     *
     * @param strToReplace
     * @return
     */
    public JsonFileReader replaceStringsWithIds( String... strToReplace )
    {
        String replacedJson = obj.toString();
        for ( String s : strToReplace )
        {
            replacedJson = replacedJson.replaceAll( s, new IdGenerator().generateUniqueId() );
        }

        obj = JsonParserUtils.toJsonObject( replacedJson );

        return this;
    }

    @Override
    public JsonFileReader replacePropertyValuesWith( String propertyName, String replacedValue )
    {
        replace( p -> {
            JsonObject object = ((JsonElement) p).getAsJsonObject();
            if ( replacedValue.equalsIgnoreCase( "uniqueid" ) )
            {
                object.addProperty( propertyName, new IdGenerator().generateUniqueId() );
            }
            else
            {
                object.addProperty( propertyName, replacedValue );
            }

            return object;
        } );

        return this;
    }

    @Override
    public JsonFileReader replacePropertyValuesRecursivelyWith( String propertyName, String replacedValue )
    {
        replace( obj, jsonObject -> {
            if ( !jsonObject.has( propertyName ) )
            {
                return;
            }
            if ( replacedValue.equalsIgnoreCase( "uniqueid" ) )
            {
                jsonObject.addProperty( propertyName, new IdGenerator().generateUniqueId() );
            }
            else
            {
                jsonObject.addProperty( propertyName, replacedValue );
            }
        } );

        return this;
    }

    public JsonObject get()
    {
        return obj;
    }

    public JsonObjectBuilder getAsObjectBuilder()
    {
        return new JsonObjectBuilder( obj );
    }

    @Override
    public JsonFileReader replace( Function<Object, Object> function )
    {
        JsonObject newObj = new JsonObject();
        for ( String key : obj.keySet() )
        {
            JsonElement element = obj.get( key );
            if ( element.isJsonArray() )
            {
                JsonArray array = new JsonArray();
                for ( JsonElement e : element.getAsJsonArray() )
                {
                    array.add( (JsonElement) function.apply( e ) );
                }
                newObj.add( key, array );
            }

            else
            {
                newObj.add( key, (JsonElement) function.apply( element ) );
            }
        }

        obj = newObj;
        return this;
    }

    private void replace( JsonElement root, Consumer<JsonObject> function )
    {
        if ( root.isJsonArray() )
        {
            for ( JsonElement e : root.getAsJsonArray() )
            {
                replace( e, function );
            }
        }
        else if ( root.isJsonObject() )
        {
            JsonObject jsonObjRoot = root.getAsJsonObject();
            function.accept( jsonObjRoot );
            for ( String key : jsonObjRoot.keySet() )
            {
                JsonElement element = jsonObjRoot.get( key );
                if ( element.isJsonArray() )
                {
                    replace( element, function );
                }
            }
        }
    }
}

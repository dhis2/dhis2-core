package org.hisp.dhis.scriptlibrary;
/*
 * Copyright (c) 2016, IntraHealth International
 * All rights reserved.
 * Apache 2.0
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

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Boolean;
import java.lang.Long;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.script.SimpleScriptContext;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ExecutionContextSE extends ExecutionContext implements IExecutionContextSE
{


    @Autowired
    protected SessionFactory sessionFactory;

    public Session getCurrentSession()
    {
	return sessionFactory.getCurrentSession();
    }
    

    
    public JsonValue createJson ( ScriptObjectMirror som )
    {
        if ( som.isArray() )
        {
            JsonArrayBuilder json = Json.createArrayBuilder();
            createJsonArray ( som, json );
            return json.build();
        }

        else
        {
            JsonObjectBuilder json = Json.createObjectBuilder();
            createJsonObject ( som, json );
            return json.build();
        }
    }


    protected void createJsonObject ( ScriptObjectMirror som, JsonObjectBuilder json )
    {
        for ( String k : som.getOwnKeys ( false ) )
        {

            Object child = som.get ( k );

            if ( child == null )
            {
                json.addNull ( k );
            }

            else if ( child instanceof String )
            {

                json.add ( k, ( String ) child );
            }

            else if ( child instanceof Long )
            {
                json.add ( k, ( ( Long ) child ).longValue() );
            }

            else if ( child instanceof Integer )
            {
                json.add ( k, ( ( Integer ) child ).intValue() );
            }

            else if ( child instanceof Double )
            {
                json.add ( k, ( ( Integer ) child ).doubleValue() );
            }

            else if ( child instanceof Float )
            {
                json.add ( k, ( ( Integer ) child ).intValue() );
            }

            else if ( child instanceof Boolean )
            {
                json.add ( k, ( ( Boolean ) child ).booleanValue() );
            }

            else if ( ! ( child instanceof ScriptObjectMirror ) )
            {
                // out+=  child.toString();
            }

            if ( child instanceof ScriptObjectMirror )
            {
                ScriptObjectMirror child_som  = ( ScriptObjectMirror ) child;

                if ( child_som.isArray() )
                {

                    JsonArrayBuilder child_builder = Json.createArrayBuilder();
                    createJsonArray ( child_som, child_builder );
                    json.add ( k, child_builder );

                }

                else
                {

                    JsonObjectBuilder child_builder = Json.createObjectBuilder();
                    createJsonObject ( child_som, child_builder );
                    json.add ( k, child_builder );

                }
            }
        }
    }



    protected void createJsonArray ( ScriptObjectMirror som, JsonArrayBuilder json )
    {
        for ( String k : som.getOwnKeys ( false ) )
        {
            Object child = som.get ( k );

            if ( child == null )
            {
                json.addNull();
            }

            else if ( child instanceof String )
            {
                json.add ( ( String ) child );
            }

            else if ( child instanceof Long )
            {
                json.add ( ( ( Long ) child ).longValue() );
            }

            else if ( child instanceof Integer )
            {
                json.add ( ( ( Integer ) child ).intValue() );
            }

            else if ( child instanceof Double )
            {
                json.add ( ( ( Integer ) child ).doubleValue() );
            }

            else if ( child instanceof Float )
            {
                json.add ( ( ( Integer ) child ).intValue() );
            }

            else if ( child instanceof Boolean )
            {
                json.add ( ( ( Boolean ) child ).booleanValue() );
            }

            else if ( ! ( child instanceof ScriptObjectMirror ) )
            {
                // out+=  child.toString();
            }

            if ( child instanceof ScriptObjectMirror )
            {
                ScriptObjectMirror child_som  = ( ScriptObjectMirror ) child;

                if ( child_som.isArray() )
                {
                    JsonArrayBuilder child_builder = Json.createArrayBuilder();
                    createJsonArray ( child_som, child_builder );
                    json.add ( child_builder );

                }

                else
                {
                    JsonObjectBuilder child_builder = Json.createObjectBuilder();
                    createJsonObject ( child_som, child_builder );
                    json.add ( child_builder );

                }
            }
        }
    }


}
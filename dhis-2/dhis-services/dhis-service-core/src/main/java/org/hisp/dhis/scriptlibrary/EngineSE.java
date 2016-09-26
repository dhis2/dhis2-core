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


import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.script.ScriptContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class EngineSE extends Engine
{

    protected static final Log log = LogFactory.getLog ( EngineSE.class );


    public ScriptEngine engine;

    public EngineSE(ScriptEngine engine){
        this.engine=engine;
    }

    @Override
    public Object evaluateScript ()
    throws ScriptException
    {
        Reader scriptReader = getScriptReader();
        log.info ( "EngineSE - eval start" );

        if ( execContext == null )
        {
            log.info ( "EngineSE - bad script context" );
            throw new ScriptExecutionException ( "Null script context" );
        }


        if ( engine == null )
        {
            log.info ( "EngineSE - bad SE" );
            throw new ScriptExecutionException ( "Bad ScriptEngine" );
        }


        try
        {
            log.info ( "EngineSE - putting context" );
            ScriptContext ctx = engine.getContext();
            ctx.setWriter ( execContext.getOut() );
            ctx.setErrorWriter ( execContext.getError() );
            ctx.setReader ( execContext.getIn() );
            log.info ( "EngineSE - setting execution context for "
                                 + execContext.getAppKey() + ":" + execContext.getScriptName() + " to:\n"
                                 + execContext.toString() );
            engine.put ( "dhisScriptContext", execContext );
            Object res  =  engine.eval ( scriptReader );
            log.info ( "EngineSE - eval done" );
            log.info ( "EngineSE - eval execution context=" + execContext.toString() );
            Object o = ctx.getAttribute ( "dhisScriptContext" );

            if ( o != null )
            {
                log.info ( "EngineSE - eval execution context=" + o.toString() );
            }

            return res;
        }

        catch ( Exception e )
        {
            log.info ("Error running script engine: "  + e.toString() + "\n" +
                        ExceptionUtils.getStackTrace(e));
            throw new ScriptExecutionException("Could not execute script:" + e.toString());
        }
    }


    public static JsonNode toJsonNode (ScriptObjectMirror som) {
        try {
            JsonNodeFactory nf = JsonNodeFactory.instance;
            return generateJsonNode(som,nf);
        } catch (Exception e) {
            log.info("Could not convert to json node:" + e.toString());
            return null;
        }

    }

    public static String toJsonString (ScriptObjectMirror som) {
        try {
            StringWriter out = new StringWriter();
            JsonGenerator gen = jsonFactory.createGenerator(out);
            generateJsonString(som,gen);
            gen.close();
            return out.toString();
        } catch (Exception e) {
            log.info("Could not convert to json string:" + e.toString());
            return null;
        }
    }

    



    private static JsonFactory jsonFactory = new JsonFactory();

    private static void addObject(Object o, JsonGenerator gen)
            throws IOException
    {
        if (o == null) {
            gen.writeNull();
        } else if (o instanceof JSObject) {
            generateJsonString((JSObject) o, gen);
        } else if (o instanceof Boolean) {
            gen.writeBoolean((Boolean) o);
        } else if (o instanceof Double) {
            gen.writeNumber((Double) o);
        } else if (o instanceof Float) {
            gen.writeNumber((Float) o);
        } else if (o instanceof Integer) {
            gen.writeNumber((Integer) o);
        } else if (o instanceof Long) {
            gen.writeNumber((Long) o);
        } else if (o instanceof Short) {
            gen.writeNumber((Short) o);
        } else if (o instanceof BigDecimal) {
            gen.writeNumber( (BigDecimal) o);
        } else if (o instanceof BigInteger) {
            gen.writeNumber( (BigInteger) o);
        } else {
            gen.writeRawValue(o.toString());
        }
    }
    private static void generateJsonString(JSObject obj, JsonGenerator gen)
            throws IOException
    {
        if (obj.isArray()) {
            gen.writeStartArray();
            Object len = obj.getMember("length");
            if (len instanceof Number) {
                int n = ((Number) len).intValue();
                for (int i = 0; i < n; i++) {
                    addObject(obj.getSlot(i),gen);
                }
            }
            gen.writeEndArray();
        } else {
            gen.writeStartObject();
            Set<String> members;
            if (obj instanceof ScriptObjectMirror) {
                String[] keys = ((ScriptObjectMirror) obj).getOwnKeys(true);
                members = Arrays.stream(keys).collect(Collectors.toSet());
            } else {
                members = obj.keySet();
            }
            for (String key : members) {
                addObject(obj.getMember(key),gen);
            }
            gen.writeEndObject();
        }

    }


    private static void addNodeToObject(String k, Object v, JsonNodeFactory nf, ObjectNode parent)
    {
        if (v == null) {
            parent.set(k,nf.nullNode());
        } else if (v instanceof JSObject ) {
            JsonNode child = generateJsonNode((JSObject) v,nf);
            parent.set(k,child);
        } else if (v instanceof Boolean) {
            parent.set(k,nf.booleanNode((Boolean) v));
        } else if (v instanceof Double) {
            parent.set(k, nf.numberNode((Double) v));
        } else if (v instanceof Float) {
            parent.set(k, nf.numberNode((Float) v));
        } else if (v instanceof Integer) {
            parent.set(k, nf.numberNode((Integer) v));
        } else if (v instanceof Long) {
            parent.set(k, nf.numberNode((Long) v));
        } else if (v instanceof Short) {
            parent.set(k, nf.numberNode((Short) v));
        } else if (v instanceof BigDecimal) {
            parent.set(k, nf.numberNode((BigDecimal) v));
        } else if (v instanceof BigInteger) {
            parent.set(k, nf.numberNode( (BigInteger) v));
        } else {
            parent.set(k, nf.textNode(v.toString()));
        }
    }

    private static void addNodeToArray(Integer i, Object v, JsonNodeFactory nf, ArrayNode parent)
    {
        if (v == null) {
            parent.set(i,nf.nullNode());
        } else if (v instanceof JSObject ) {
            JsonNode child = generateJsonNode((JSObject) v,nf);
            parent.set(i,child);
        } else if (v instanceof Boolean) {
            parent.set(i,nf.booleanNode((Boolean) v));
        } else if (v instanceof Double) {
            parent.set(i, nf.numberNode((Double) v));
        } else if (v instanceof Float) {
            parent.set(i, nf.numberNode((Float) v));
        } else if (v instanceof Integer) {
            parent.set(i, nf.numberNode((Integer) v));
        } else if (v instanceof Long) {
            parent.set(i, nf.numberNode((Long) v));
        } else if (v instanceof Short) {
            parent.set(i, nf.numberNode((Short) v));
        } else if (v instanceof BigDecimal) {
            parent.set(i, nf.numberNode((BigDecimal) v));
        } else if (v instanceof BigInteger) {
            parent.set(i, nf.numberNode( (BigInteger) v));
        } else {
            parent.set(i, nf.textNode(v.toString()));
        }
    }

    private static JsonNode generateJsonNode(JSObject obj, JsonNodeFactory nf ) {
        if (obj.isArray() ) {
            ArrayNode parent = nf.arrayNode();
            Object len = obj.getMember("length");
            if (len instanceof Number) {
                int n = ((Number) len).intValue();
                for (int i = 0; i < n; i++) {
                    addNodeToArray(i,obj.getSlot(i),nf,  parent);
                }
            }
            return parent;
        } else {
            ObjectNode parent = nf.objectNode();
            Set<String> members;
            if (obj instanceof ScriptObjectMirror) {
                String[] keys = ((ScriptObjectMirror) obj).getOwnKeys(true);
                members = Arrays.stream(keys).collect(Collectors.toSet());
            } else {
                members = obj.keySet();
            }
            for (String key : members) {
                addNodeToObject(key,obj.getMember(key),nf,  parent);
            }
            return parent;
        }
    }



}
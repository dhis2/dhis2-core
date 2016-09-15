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
import java.util.Map;
import java.lang.NullPointerException;
import javax.script.Bindings;
import javax.script.SimpleScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.ScriptContext;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.basex.core.*;
import org.basex.query.*;
import org.basex.query.value.*;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.scriptlibrary.Engine;
import org.hisp.dhis.scriptlibrary.IExecutionContext;
import org.hisp.dhis.scriptlibrary.IExecutionContextSE;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;
import org.springframework.beans.factory.annotation.Autowired;


public class EngineXQuery extends Engine
{
    @Autowired
    protected  AppManager appManager;

    public EngineXQuery ( App app, ScriptLibrary sl )
    {
        super ( app, sl );
    }


    @Override
    protected Object eval ( IExecutionContext execContext )
    throws ScriptException, ScriptNotFoundException, ScriptAccessException
    {
	try					
	{
	    Context context = new Context();
            String query  = IOUtils.toString(sl.retrieveScript ( execContext.getScriptName() ));
	    QueryProcessor qp = new QueryProcessor(query, context);
	    qp.context(execContext);

            StreamSource text = new StreamSource ( execContext.getIn() );
            StreamResult outXQ = new StreamResult ( execContext.getOut() );
	    
	    Value result = qp.value();
	    return result;
	    
	}
        catch ( ScriptNotFoundException ioe )
        {
            throw new ScriptException ( "Could not get source" + ioe.toString() );
        }

        catch ( Exception e )
        {
            runException = e;
            return null;
        }

    }

}

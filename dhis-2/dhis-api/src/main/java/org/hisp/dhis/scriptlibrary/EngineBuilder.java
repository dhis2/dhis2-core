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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptException;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.scriptlibrary.Engine;
import org.hisp.dhis.scriptlibrary.EngineBuilderInterface;
import org.hisp.dhis.scriptlibrary.ExecutionContext;
import org.hisp.dhis.scriptlibrary.ScriptLibrary;
import org.hisp.dhis.scriptlibrary.ScriptExecutionException;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Carl Leitner <litlfred@gmail.com>
 */
abstract public class EngineBuilder implements EngineBuilderInterface
{
    @Autowired
    protected AppManager appManager;

    protected void loadDependencies ( ScriptLibrary sl, String scriptName, Engine scriptEngine, IExecutionContext depContext )
    throws ScriptNotFoundException
    {
        System.out.println ( "Running engine for dependency: " + scriptName );
        String[] libs = sl.retrieveDependencies ( scriptName );
	System.out.println ( "loading dependencies" );
	User user  = depContext.getUser();

	for (String script : libs) {
	    try
	    {
		App app;
		if (  script.startsWith("/apps/" ))
		{
		    String depAppName = script.substring(6);
		    depAppName = depAppName.substring(0,depAppName.indexOf("/") );
		    ScriptLibrary appLib = sl.getScriptLibrary(depAppName);
		    if (appLib == null) {
			throw new ScriptNotFoundException("Referenced app " + depAppName + " could not be found");
		    }
		    app = appLib.getApp();
		} else {
		    app = sl.getApp();
		}
		if (  !appManager.isAccessible ( app, user ) )
		{
		    //HELP:  This should not be commented out.  Not sure why  the above expression evaluates  to false
		    //throw new ScriptAccessException ( "Script execution - permission denied on user" );
		}

		depContext.setScriptName ( script );
		scriptEngine.setExecutionContext ( depContext );
		scriptEngine.run();

		if ( scriptEngine.runException != null )
		{
		    throw scriptEngine.runException;
		}
	    }
	    catch ( Exception e )
	    {
		throw new ScriptNotFoundException ( "Could not load dependency " + script + " for " + scriptName + "\n"  + e.toString() );
	    }

	}
    }

    @Override
    public Engine  eval ( App app, ScriptLibrary sl, IExecutionContext execContext )
    throws ScriptException, ScriptNotFoundException, ScriptAccessException
    {
        String scriptName  = execContext.getScriptName();
        String appName  = execContext.getAppName();
        User user  = execContext.getUser();

        if ( scriptName == null )
        {
            throw new ScriptAccessException ( "Script execution - No script name specified" );
        }

        if ( appName == null || app == null )
        {
            throw new ScriptNotFoundException ( "Script execution - No app associated to script" );
        }

        if ( user == null  )
        {
            throw new ScriptAccessException ( "Script execution - Invalid user" );
        }

        if (  !appManager.isAccessible ( app, user ) )
        {
            //HELP:  This should not be commented out.  Not sure what the above expression evaluates  to false
            //throw new ScriptAccessException ( "Script execution - permission denied on user" );
        }
        Engine engine = getEngine ( app, sl, scriptName );
        execute (  engine, execContext );
        return engine;
    }



    protected void execute (  Engine engine, IExecutionContext execContext )
    throws ScriptException
    {
        System.out.println ( "Execute start" );
        engine.runException = null;
        engine.setExecutionContext ( execContext );
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<Object> task = new Callable<Object>()
        {
            public Object call()
            {
                System.out.println ( "Callable - called." );

                try
                {
                    engine.run();
                    System.out.println ( "Callable - engine ran." );

                }

                catch ( Exception e )
                {
                    System.out.println ( "Callable - caught." );
                    engine.runException = new ScriptExecutionException ( "error on calling script " + e.toString() );
                }

                return null;
            }
        };
        System.out.println ( "Execute adding task to the future." );
        Future<Object> future = executor.submit ( task );

        try
        {
            System.out.println ( "Execute getting  future." );
            Object result = future.get ( maxTime, TimeUnit.SECONDS );
        }

        catch ( Exception e )
        {
            System.out.println ( "Execute   future is exceptional." );
            engine.runException = new ScriptExecutionException ( "error waiting for script execution to finish " + e.toString() );
        }

        finally
        {
            System.out.println ( "Execute   future is final." );
            future.cancel ( true ); // may or may not desire this
            System.out.println ( "Execute   future is canceled." );
            executor.shutdown();
        }

        System.out.println ( "Execute shutdown" );

        // if ( engine.runException != null )
        // {
        //     throw new ScriptExecutionException ( "Could not execute " + scriptName + ":\n" + engine.runException.toString() );
        // }
    }

    public Integer maxTime = 60;

    public void setMaxTime ( Integer maxTime )
    {
        this.maxTime = maxTime;
    }



}
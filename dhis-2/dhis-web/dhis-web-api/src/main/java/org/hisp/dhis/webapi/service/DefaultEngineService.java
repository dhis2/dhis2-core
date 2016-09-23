package org.hisp.dhis.webapi.service;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.scriptlibrary.*;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttpInterface;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**                                                                                                                                                                                 
 * @author Carl Leitner <litlfred@gmail.com>
 */
@Service
public class DefaultEngineService implements EngineServiceInterface {

    protected static final Log log = LogFactory.getLog( DefaultDataValueService.class );    

    protected ScriptEngineManager engineManager = new ScriptEngineManager();

	@Autowired
	protected AppManager appManager;

    @Autowired 
    protected ApplicationContext applicationContext;
    protected Map<String,String> engines = new HashMap<String,String>();
    @Autowired
    protected CurrentUserService currentUserService;
    @Autowired
    protected JSClassFilter classFilter;
	@Autowired
	protected SessionFactory sessionFactory;

    protected Map<String,EngineInterface> scriptEngines = new HashMap<String,EngineInterface>();

	public void setEngines(Map<String, String> map) {
		engines = map;
	}

	public EngineInterface getEngine(App app, ScriptLibrary sl, String scriptName)
			throws ScriptException
	{
		String scriptKey = sl.getName() + ":" + scriptName;
		if (false)
//
//      SHOULD UNCOMMENT THIS LINE AND REPLACE ABOVE LINE TO CACHE ENGINES WHEN IN DEPLOYMENT MODE.  SHOULD REALLY BE A RUN-TIME/ADMIN USER CONTROLLED OPTION
//	if (scriptEngines.containsKey(scriptKey))   
		{
			//we really should check here if the engine is "stale" meaning for example it was generated before the last
			//time the manifest.webapp was changed or one of the dependecies changed
			//perhaps add:
			//   public boolean lastModified(String scriptname)
			//which gets the max modified time of a manifestwebapp, a script, and its dependenceis
			return scriptEngines.get(scriptKey);
		} else {
			String ext = FilenameUtils.getExtension(scriptName);
			EngineInterface engine = null;
			log.info("Creating engine on script type:" + ext);
			if (ext.equals("xsl") || ext.equals("xslt")) {
				log.info("Creating XSLT engine on script type");
				engine =  getEngineXSLT(app, scriptName);
			} else if (ext.equals("xq")) {
				log.info("Creating XQuery engine on script type");
				engine =  getEngineXQuery(app, scriptName);
			} else {
				log.info("Creating SE engine on script type");
				engine = getEngineSE(app, scriptName);
			}
			//if we make it here we are all good and we should put it into our script execution cache
			log.info("registering script engine with key: " + scriptKey + "\n");
			scriptEngines.put(scriptKey,  engine);
			return engine;
		}
	}

	protected EngineInterface getEngineXSLT(App app, String scriptName)
			throws ScriptException
	{
		EngineXSLT xsltEngine;
		xsltEngine = new EngineXSLT();
		log.info("Retrieving dependencies for " + scriptName);
		ExecutionContext depContext = new ExecutionContext();
		depContext.setApplicationContext(applicationContext);
		depContext.setUser(currentUserService.getCurrentUser());
		depContext.setAppKey(app.getKey());
		depContext.setScriptName(scriptName);
		loadDependencies(xsltEngine, depContext);
		return xsltEngine;
	}

	protected EngineInterface getEngineXQuery(App app, String scriptName)
			throws ScriptException
	{
		EngineXQuery xqueryEngine;
		xqueryEngine = new EngineXQuery();
		log.info("Retrieving dependencies for " + scriptName);
		ExecutionContext depContext = new ExecutionContext();
		depContext.setApplicationContext(applicationContext);
		depContext.setUser(currentUserService.getCurrentUser());
		depContext.setAppKey(app.getKey());
		depContext.setScriptName(scriptName);
		loadDependencies(xqueryEngine, depContext);
		return xqueryEngine;
	}

	protected EngineInterface getEngineSE(App app, String scriptName)
			throws ScriptException
	{
		log.info("Creating ScriptEngine engine for " );
		ScriptEngine engine;
		String ext = FilenameUtils.getExtension(scriptName);
		try {

			if (ext.equals("js")) {
				//special loading to allow for class filter
				log.info("Creating nashorn engine " );
				NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
				engine = factory.getScriptEngine(classFilter);
			} else {
				String engineName = null;
				engineName = engines.get(ext);
				log.info("Creating " + engineName + " engine ");
				engine = engineManager.getEngineByName(ext); //e.g "nashorn"
			}
			if (engine == null) {
				throw new ScriptException("Could not create " + ext + " ScriptEngine ");
			}
		} catch (Exception e) {
			throw new ScriptException("extension " + ext + ": cannot get engine:" + e.toString());
		}
		log.info("Instantiating SE engine for " + scriptName);
		EngineSE scriptEngine = new EngineSE( engine);
		log.info("Retrieving dependencies for " + scriptName);
		ExecutionContext depContext = new ExecutionContext();
		depContext.setApplicationContext(applicationContext);
		depContext.setAppKey(app.getKey());
		depContext.setScriptName(scriptName);
		depContext.setUser(currentUserService.getCurrentUser());
		SimpleScriptContext ctx = new SimpleScriptContext();
		ctx.setErrorWriter(depContext.getError());
		ctx.setWriter(depContext.getOut());
		loadDependencies(scriptEngine, depContext);
		return scriptEngine;
	}

	protected void loadDependencies ( EngineInterface scriptEngine, ExecutionContext depContext )
			throws ScriptException
	{
		String scriptName = null;
		String[] libs;
		try {
			scriptName = depContext.getScriptName();
			App app = appManager.getApp(depContext.getAppKey(), contextPath);
			ScriptLibrary sl = appManager.getScriptLibrary(app);
			log.info("Running engine for dependency: " + scriptName);
			libs = sl.retrieveDependencies(scriptName);
			log.info("loading dependencies");
		} catch (Exception e) {
			throw new ScriptNotFoundException("Could not retrieve dependencies for " + scriptName + "\n" + e.toString()
					+ "\n" + ExceptionUtils.getStackTrace(e));
		}
		User user = depContext.getUser();
		for (String script : libs) {
			try {
				String depAppKey = null;
				String depScriptName = null;
				if (script.startsWith("/apps/")) {
					depAppKey = script.substring(6);
					depScriptName = depAppKey.substring(depAppKey.indexOf("/") + 1);
					depAppKey = depAppKey.substring(0, depAppKey.indexOf("/"));
				} else {
					depAppKey= depContext.getAppKey();
					depScriptName = script;
				}
				App depApp = appManager.getApp(depAppKey, contextPath);

				if (!appManager.isAccessible(depApp, user)) {
					//HELP:  This should not be commented out.  Not sure why  the above expression evaluates  to false
					//throw new ScriptAccessException ( "Script execution - permission denied on user" );
				}
				log.info("Processing dependency " + depScriptName + " in " + depAppKey + "(" + script + ")");
				ScriptLibrary depSl = appManager.getScriptLibrary(depApp);
				depContext.setScriptName(depScriptName);
				depContext.setAppKey(depAppKey);
				scriptEngine.setExecutionContext(depContext);
				Reader depScriptReader = depSl.retrieveScript(depScriptName);
				scriptEngine.setScriptReader(depScriptReader);
				scriptEngine.call();
			} catch (ScriptException e) {
				throw e;
			} catch (Exception e) {
				throw new ScriptExecutionException("Could not load dependency " + script + " for " + scriptName + "\n" + e.toString()
						+ "\n" + ExceptionUtils.getStackTrace(e));
			}


		}

	}


	protected String contextPath;

	public Object  eval ( ExecutionContextInterface execContext )
			throws ScriptException
	{
		String scriptName  = execContext.getScriptName();
		if ( scriptName == null )
		{
			throw new ScriptAccessException ( "Script execution - No script name specified" );
		}
		String appKey = execContext.getAppKey();
		App app = null;
		if (execContext instanceof ExecutionContextHttpInterface) {
			HttpServletRequest hsr = ( (ExecutionContextHttpInterface) execContext).getHttpServletRequest();
			contextPath = ContextUtils.getContextPath(hsr);
			app = appManager.getApp(appKey, contextPath);
		}
		if ( appKey == null || app == null )
		{
			throw new ScriptNotFoundException ( "Script execution - No app associated to script" );
		}
		log.info("Attempting to retrieve " + execContext.getScriptName() + " for app " + execContext.getAppKey());
		ScriptLibrary sl = appManager.getScriptLibrary(app);
		Reader scriptReader = sl.retrieveScript(scriptName);
		User user  = execContext.getUser();

		if ( user == null  )
		{
			throw new ScriptAccessException ( "Script execution - Invalid user" );
		}

		if (  !appManager.isAccessible ( app, user ) )
		{
			//HELP:  This should not be commented out.  Not sure what the above expression evaluates  to false
			//throw new ScriptAccessException ( "Script execution - permission denied on user" );
		}

		EngineInterface engine = getEngine ( app, sl, scriptName );
		engine.setScriptReader(scriptReader);
		return execute (  engine, execContext );

	}


	protected Object execute (  EngineInterface engine, ExecutionContextInterface execContext )
			throws ScriptException
	{
		log.info ( "Execute start" );
		engine.setExecutionContext ( execContext );
		Object result = null;
		try {
			result = engine.call();
		} catch (ScriptException e) {
			throw e;
		} catch (Exception e) {
			throw new ScriptException("Could not execute: " + e.toString() + "\n" + ExceptionUtils.getStackTrace(e));
		}
		log.info ( "Execute done" );
		return result;
	}


	@Transactional
	protected Object executeThreaded (  EngineInterface engine, ExecutionContextInterface execContext )
			throws ScriptException
	{
		log.info ( "Execute start" );
		engine.setExecutionContext ( execContext );
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<Object> task = engine;

		Session session;
		try {
			session = sessionFactory.getCurrentSession();
		} catch (HibernateException e) {
			session = sessionFactory.openSession();
		}

		log.info ( "Execute adding task to the future." );
		Future<Object> future = executor.submit ( task );

		Object result =null;
		try
		{

			log.info ( "Execute getting  future." );
			result = future.get ( maxTime, TimeUnit.SECONDS );
		}
		catch ( Exception e )
		{
			log.info ( "Execute   future is exceptional." );
			throw  new ScriptExecutionException ( "error waiting for script execution to finish " + e.toString() );
		}
		finally
		{
			log.info ( "Execute   future is final." );
			future.cancel ( true ); // may or may not desire this
			log.info ( "Execute   future is canceled." );
			executor.shutdown();
			session.flush();
			session.clear();
			session.close();
		}

		log.info ( "Execute shutdown" );
		return result;
	}

	public Integer maxTime = 60;

	public void setMaxTime ( Integer maxTime )
	{
		this.maxTime = maxTime;
	}




}
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


import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.json.JsonStructure;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.DefaultAppManager;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.scriptlibrary.Engine;
import org.hisp.dhis.scriptlibrary.EngineBuilder;
import org.hisp.dhis.scriptlibrary.EngineSE;
import org.hisp.dhis.scriptlibrary.EngineXSLT;
import org.hisp.dhis.scriptlibrary.ExecutionContext;
import org.hisp.dhis.scriptlibrary.ExecutionContextSE;
import org.hisp.dhis.scriptlibrary.JSClassFilter;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.hisp.dhis.scriptlibrary.ScriptLibrary;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**                                                                                                                                                                                 
 * @author Carl Leitner <litlfred@gmail.com>
 */
@Service
public class DefaultEngineBuilder extends EngineBuilder{

    protected static final Log log = LogFactory.getLog( DefaultDataValueService.class );    

    protected ScriptEngineManager engineManager = new ScriptEngineManager();

    @Autowired 
    protected ApplicationContext applicationContext;
    protected Map<String,String> engines = new HashMap<String,String>();
    @Autowired
    protected CurrentUserService currentUserService;
    @Autowired
    protected JSClassFilter classFilter;

    protected Map<String,Engine> scriptEngines = new HashMap<String,Engine>();

    public void setEngines(Map<String,String> map) {
	engines = map;
    }

    @Override
    public Engine getEngine(App app, ScriptLibrary sl, String scriptName) 
	throws ScriptException, ScriptNotFoundException
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
	    return  scriptEngines.get(scriptKey);
	}  
	else 
	{
	    String ext = FilenameUtils.getExtension(scriptName);
	    log.info("Creating engine on script type:" + ext);
	    if (ext.equals("xsl") || ext.equals("xslt")) {
		log.info("Creating XSLT engine on script type");
		return getEngineXSLT(app,sl,scriptName);
	    if (ext.equals("xq")) {
		log.info("Creating XQuery engine on script type");
		return getEngineXQuery(app,sl,scriptName);
	    } else {
		log.info("Creating SE engine on script type");
		return getEngineSE(app,sl,scriptName);
	    }
	}
    }

    protected Engine getEngineXSLT(App app, ScriptLibrary sl, String scriptName) 
	throws ScriptException, ScriptNotFoundException
    {
	String scriptKey = sl.getName() + ":" + scriptName;
	EngineXSLT xsltEngine;
	xsltEngine = new EngineXSLT(app,sl);
	log.info("Retrieving dependencies for " + scriptName);
	ExecutionContext depContext = new ExecutionContext();
	depContext.setApplicationContext(applicationContext); 
	depContext.setUser ( currentUserService.getCurrentUser() );
	depContext.setAppName(app.getKey());
	loadDependencies(sl,scriptName,xsltEngine,depContext);
	log.info("registering xsl script engine with key: " + scriptKey);
	scriptEngines.put(scriptKey,xsltEngine);
	return xsltEngine;
    }

    protected Engine getEngineXQuery(App app, ScriptLibrary sl, String scriptName) 
	throws ScriptException, ScriptNotFoundException
    {
	String scriptKey = sl.getName() + ":" + scriptName;
	EngineXQuery xqueryEngine;
	xqueryEngine = new EngineXQuery(app,sl);
	log.info("Retrieving dependencies for " + scriptName);
	ExecutionContext depContext = new ExecutionContext();
	depContext.setApplicationContext(applicationContext); 
	depContext.setUser ( currentUserService.getCurrentUser() );
	depContext.setAppName(app.getKey());
	loadDependencies(sl,scriptName,xqueryEngine,depContext);
	log.info("registering xquery script engine with key: " + scriptKey);
	scriptEngines.put(scriptKey,xsltEngine);
	return xsltEngine;
    }

    protected Engine getEngineSE(App app, ScriptLibrary sl, String scriptName)  
	throws ScriptException, ScriptNotFoundException
    {
	String scriptKey = sl.getName() + ":" + scriptName;
	log.info("Creating ScriptEngine engine for " + scriptKey);
	ScriptEngine engine;
	String ext = FilenameUtils.getExtension(scriptName);
	try {

	    if (ext.equals("js")) {
		//special loading to allow for class filter
		log.info("Creating nashorn engine for " + scriptKey);
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		engine = factory.getScriptEngine( classFilter);
	    } else {
		String engineName = null;
		engineName = engines.get(ext);
		log.info("Creating " + engineName + " engine for " + scriptKey);
		engine = engineManager.getEngineByName(ext); //e.g "nashorn"		
	    }
	    if (engine == null) {
		throw new ScriptException("Could not create " + ext + " ScriptEngine for " + scriptKey);
	    }
	} catch (Exception e) {
	    throw new ScriptException("extension " + ext + ": cannot get engine:" + e.toString());
	}
	log.info("Instantiating SE engine for " + scriptName);
	EngineSE scriptEngine = new EngineSE(app,sl,engine);
	log.info("Retrieving dependencies for " + scriptName);
	ExecutionContext depContext = new ExecutionContextSE();
	depContext.setApplicationContext(applicationContext);
	depContext.setAppName(app.getKey());
	depContext.setUser ( currentUserService.getCurrentUser() );
	SimpleScriptContext ctx =new SimpleScriptContext();
	ctx.setErrorWriter(depContext.getError());
	ctx.setWriter(depContext.getOut());
	loadDependencies(sl,scriptName,scriptEngine,depContext);

	//if we make it here we are all good and we should put it into our script execution cache
	log.info("registering script engine with key: " + scriptKey + "\n" + scriptEngine.toString());
	scriptEngines.put(scriptKey,scriptEngine);
	return scriptEngine;
    }  



 


}
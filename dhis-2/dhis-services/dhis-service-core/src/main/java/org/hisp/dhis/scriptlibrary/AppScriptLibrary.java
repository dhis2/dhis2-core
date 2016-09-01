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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.scriptlibrary.ScriptLibrary;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Carl Leitner <litlfred@gmail.com>
 */
public class AppScriptLibrary implements ScriptLibrary
{

    protected static final Log log = LogFactory.getLog ( DefaultDataValueService.class );

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    protected  AppManager appManager;
    protected Iterable<Resource> locations = Lists.newArrayList();
    protected App app;
    protected String[] resources;
    protected String[] operations;

    @Override
    public String getName()
    {
        return app.getKey();
    }

    public AppScriptLibrary ( App app, AppManager appManager )
    {
        this.appManager = appManager;
        String appKey = app.getKey();
        log.info ( "Init library for " + appKey );
        this.app = app;
        this.resources = resources;
        this.operations = operations;

        try
        {
            locations = Lists.newArrayList (
                            resourceLoader.getResource ( "file:" + appManager.getAppFolderPath() + "/" + appKey   + "/" ),
                            resourceLoader.getResource ( "classpath*:/apps/" + appKey + "/" )
//                            resourceLoader.getResource ( "/apps/" + appKey + "/"   )
		);

            for ( Resource location : locations )
            {
                log.info ( "Adding " + location.toString() );
            }
        }

        catch ( Exception e )
        {
            try
            {
                log.info ( "Could not init AppScriptLibrary:" + e.toString() );

                if ( appManager == null )
                {
                    log.info ( "appManager not initialized" );
                }
            }

            catch ( Exception e2 ) {}
        }
    }


    public App getApp() {
	return app;
    }

    public App getApp(String appName) {
	return  appManager.getApp(appName,this.app.getContextPath());	    
    }

    protected Map<String, ScriptLibrary> scriptLibraries = new HashMap<String, ScriptLibrary>();
    protected Map<String, App> apps = new HashMap<String, App>();


    public ScriptLibrary getScriptLibrary ( String key )
    {

        if ( scriptLibraries.containsKey ( key ) )
        {
            return scriptLibraries.get ( key );
        }

        else
        {
            ScriptLibrary sl =  new AppScriptLibrary ( getApp(key), appManager );
            scriptLibraries.put ( key, sl );
            return sl;
        }
    }

    public boolean containsScript ( String name )
    {
        try
        {
	    if (  name.startsWith("/apps/" ))
	    {
		String depAppName = name.substring(6);
		String depResource = depAppName.substring(depAppName.indexOf("/") + 1);
		depAppName = depAppName.substring(0,depAppName.indexOf("/") - 1);
		ScriptLibrary depAppLib = getScriptLibrary(depAppName);
		Resource r = depAppLib.findResource(depResource);
		return ( r != null );
	    } 
	    else 
	    {
		Resource r = findResource (   name );
		return ( r != null );
	    }
        }

        catch ( IOException e )
        {
            return false;
        }
    }

    public  Reader retrieveScript ( String name )
    throws  ScriptNotFoundException
    {
        try
        {
	    if (  name.startsWith("/apps/" ))
	    {
		String depAppName = name.substring(6);
		String depResource = depAppName.substring(depAppName.indexOf("/") + 1);
		depAppName = depAppName.substring(0,depAppName.indexOf("/") - 1);
		ScriptLibrary depAppLib = getScriptLibrary(depAppName);
		Resource r = depAppLib.findResource(depResource);
		return new InputStreamReader ( r.getInputStream() );
		
	    } 
	    else 
	    {
		Resource r = findResource (  name );
		return new InputStreamReader ( r.getInputStream() );
	    }
        }

        catch ( Exception e )
        {
            throw new ScriptNotFoundException ( "Could not get " + name + " :\n" + e.toString() );
        }
    }


    public String[] retrieveDirectDependencies(String script) 
    {
        try
        {
            JsonObject deps = ( JsonObject )  retrieveManifestInfo (  "script-library/dependencies".split ( "/" ) );
	    ArrayList<String> sdeps  = new ArrayList();
	    for (Object o : deps.getJsonArray(script)) {
		String dep;
		if (o instanceof JsonString) {
		    dep = ((JsonString) o).getString();
		} else if (o instanceof String) {
		    dep = (String) o;
		} else {
		    continue;
		}
		sdeps.add(dep);
	    }
	    return sdeps.toArray ( new String[sdeps.size()] );
        }

        catch ( Exception e )
        {
            return new String[0];
        }

    }

    
    public String[] retrieveDependencies ( String name )
    {
	try {
	    Stack<String> deps = new Stack<String>();
	    deps.addAll ( Arrays.asList(retrieveDirectDependencies(name)));
	    ArrayList<String> seen = new ArrayList();
	    seen.add(name);

	    while ( ! deps.isEmpty() )
	    {
		String script = deps.pop();
		seen.add(script);
		System.out.println ( "Attempting to resolve dependency: " + script );
		ScriptLibrary depAppLib;
		String depAppName = null;
		String depResource = null;
		if (  name.startsWith("/apps/" ))
		{
		    depAppName = name.substring(6);
		    depResource = depAppName.substring(depAppName.indexOf("/") + 1);
		    depAppName = depAppName.substring(0,depAppName.indexOf("/") - 1);
		    depAppLib = getScriptLibrary(depAppName);		    
		} 
		else {
		    depAppLib = this;
		    depResource = name;
		}

		if (  ! depAppLib.containsScript ( depResource ) )
		{
		    throw new ScriptNotFoundException ( "Script " + depResource + " not found in script library [" + name + "]" );
		} 
		
		
		String[] newDeps = depAppLib.retrieveDirectDependencies(depResource);
		for (String newDep :  newDeps)  {
		    if ( (!newDep.startsWith("/apps/")) && depAppName != null) 
		    {
			newDep = "/apps/" + depAppName + "/" + newDep;
		    } 
		    if (seen.contains(newDep)) {
			continue;
		    } 
		    deps.add(newDep);

		}
	    }
	    Collections.reverse(seen);
	    seen.remove(seen.size()); //ger rid of the starting script	    
	    return seen.toArray ( new String[seen.size()] );
	}

        catch ( Exception e )
        {
            log.info ( "Could not retrieve  deps " + e.toString() );
            return new String[0];
        }
    }




    @Override
    public JsonValue  retrieveManifestInfo ( String[] path )
    {
        try
        {
            //it is slow to hit this all the time, should it cache based on file mtime?
            log.info ( "Looking for manifest" );
            Resource manifest = findResource ( "manifest.webapp" );
            JsonReader jsonReader = Json.createReader ( new InputStreamReader ( manifest.getInputStream() ) );
            log.info ( "reading manifest" );
            JsonValue info = jsonReader.readObject();
            jsonReader.close();
            log.info ( "Looking at manifest " + info.toString() );

            for ( int i = 0; i < path.length ; i++ ) //walk the binding path
            {
                log.info ( "retrieving " + path[i] + " from " + info.toString() );

                if ( info instanceof JsonArray )
                {
                    int num = Integer.parseInt ( path[i] );
                    info = ( JsonValue ) ( ( JsonArray ) info ).get ( num );
                }

                else  if ( info instanceof JsonObject )
                {
                    info = ( JsonValue ) ( ( JsonObject ) info ).get ( path[i] );
                }

                else
                {
                    log.info ( "Could not access path at " + path[i] + " in manifest for " + app.getKey()
                               + ": not an object/array (" + info.getValueType().toString() + ")" );
                    return Json.createObjectBuilder().build(); //return empty object
                }
            }

            return info;
        }

        catch ( Exception e )
        {
            log.info ( "Could not access manifest for " + app.getKey() + ":"  + e.toString() );
            return Json.createObjectBuilder().build(); //return empty object
        }
    }


    //partially stolen from dhis-web/dhis-web-api/src/main/java/org/hisp/dhis/webapi/controller/AppController.java
    public Resource findResource (  String resourceName )
    throws IOException
    {
        log.info ( "Looking for resource [" + resourceName + "]" );

        for ( Resource location : locations )
        {
            Resource resource = location.createRelative ( resourceName );
            log.info ( "Checking " + resource.toString() + " under " + location.toString() );

            if ( resource.exists() && resource.isReadable() )
            {
                log.info ( "Found " + resource.toString() );
                return resource;
            }
        }

        return null;
    }



}

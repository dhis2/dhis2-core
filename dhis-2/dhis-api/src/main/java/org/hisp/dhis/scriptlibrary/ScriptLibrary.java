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
import java.io.IOException;
import javax.json.JsonValue;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.springframework.core.io.Resource;

/**
 * @author Carl Leitner <litlfred@gmail.com>
 */
public interface ScriptLibrary
{
    abstract public boolean containsScript ( String name );
    abstract public String[] retrieveDirectDependencies(String scriptName);
    abstract public String[] retrieveDependencies ( String scriptName );
    abstract public Reader retrieveScript ( String name ) throws ScriptNotFoundException;
    abstract public JsonValue  retrieveManifestInfo ( String[] path );
    abstract public String getName();
    abstract public Resource findResource (  String resourceName )
	throws IOException;
    abstract public ScriptLibrary getScriptLibrary ( String key )
	throws ScriptNotFoundException;
    abstract public App getApp();
}
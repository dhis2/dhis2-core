package org.hisp.dhis.helpers.models;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Stian Sandvold
 */
public class User
{
    private String username;

    private String uid;

    private String password;

    private Map<String, List<String>> dataRead;

    private List<String> groups = new ArrayList<>();

    private List<String> searchScope = new ArrayList<>();

    private List<String> captureScope = new ArrayList<>();

    private boolean allAuthority;

    public User( String username, String uid, String password )
    {
        this.username = username;
        this.uid = uid;
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public String getUid()
    {
        return uid;
    }

    public String getPassword()
    {
        return password;
    }

    public Map<String, List<String>> getDataRead()
    {
        return dataRead;
    }

    public void setDataRead( Map<String, List<String>> dataRead )
    {
        this.dataRead = dataRead;
    }

    public List<String> getGroups()
    {
        return groups;
    }

    public void setGroups( List<String> groups )
    {
        this.groups = groups;
    }

    public void setCaptureScope( List<String> captureScope )
    {
        this.captureScope = captureScope;
    }

    public void setSearchScope( List<String> searchScope )
    {
        this.searchScope = searchScope;
    }

    public List<String> getScopes()
    {
        return ListUtils.union( searchScope, captureScope );
    }

    public void setAllAuthority( boolean allAuthority )
    {
        this.allAuthority = allAuthority;
    }

    public boolean hasAllAuthority()
    {
        return allAuthority;
    }
}

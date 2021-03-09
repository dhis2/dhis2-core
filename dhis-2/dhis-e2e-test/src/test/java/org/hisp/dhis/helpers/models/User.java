package org.hisp.dhis.helpers.models;



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

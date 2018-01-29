package org.hisp.dhis.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Henning Håkonsen
 */
public class SimpleUser
    extends User
    implements UserDetails
{
    @Override
    public Collection<GrantedAuthority> getAuthorities()
    {
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>( );

        getUserCredentials().getAllAuthorities().forEach( authority -> {
            grantedAuthorities.add( new SimpleGrantedAuthority( authority ) );
        } );

        return grantedAuthorities;
    }

    @Override
    public String getPassword()
    {
        return getUserCredentials().getPassword();
    }

    @Override
    public String getUsername()
    {
        return getUserCredentials().getUsername();
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return false;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return false;
    }

    @Override
    public boolean isEnabled()
    {
        return !getUserCredentials().isDisabled();
    }
}

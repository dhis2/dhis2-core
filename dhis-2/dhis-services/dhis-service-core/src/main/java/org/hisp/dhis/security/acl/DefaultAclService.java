package org.hisp.dhis.security.acl;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.AuthorityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.util.CollectionUtils.containsAny;

/**
 * Default ACL implementation that uses SchemaDescriptors to get authorities / sharing flags.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultAclService implements AclService
{
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public boolean isSupported( String type )
    {
        return schemaService.getSchemaBySingularName( type ) != null;
    }

    @Override
    public boolean isSupported( Class<?> klass )
    {
        return schemaService.getSchema( klass ) != null;
    }

    @Override
    public boolean isShareable( String type )
    {
        Schema schema = schemaService.getSchemaBySingularName( type );
        return schema != null && schema.isShareable();
    }

    @Override
    public boolean isShareable( Class<?> klass )
    {
        Schema schema = schemaService.getSchema( klass );
        return schema != null && schema.isShareable();
    }

    @Override
    public boolean canRead( User user, IdentifiableObject object )
    {
        if ( object == null || haveOverrideAuthority( user ) )
        {
            return true;
        }

        Schema schema = schemaService.getSchema( object.getClass() );

        if ( schema == null || object.getUser() == null || object.getPublicAccess() == null )
        {
            return true;
        }

        if ( canAccess( user, schema.getAuthorityByType( AuthorityType.READ ) ) )
        {
            if ( !schema.isShareable() )
            {
                return true;
            }

            if ( checkUser( user, object ) || checkSharingPermission( user, object, AccessStringHelper.Permission.READ ) )
            {
                return true;
            }
        }
        else
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean canWrite( User user, IdentifiableObject object )
    {
        Schema schema = schemaService.getSchema( object.getClass() );

        if ( schema == null || haveOverrideAuthority( user ) )
        {
            return true;
        }

        List<String> anyAuthorities = schema.getAuthorityByType( AuthorityType.CREATE );

        if ( anyAuthorities.isEmpty() )
        {
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE ) );
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PUBLIC ) );
        }

        if ( canAccess( user, anyAuthorities ) )
        {
            if ( !schema.isShareable() )
            {
                return true;
            }

            if ( checkSharingAccess( user, object ) &&
                (checkUser( user, object ) || checkSharingPermission( user, object, AccessStringHelper.Permission.WRITE )) )
            {
                return true;
            }
        }
        else if ( schema.isImplicitPrivateAuthority() && checkSharingAccess( user, object ) )
        {
            return true;
        }

        return false;
    }

    @Override
    public boolean canUpdate( User user, IdentifiableObject object )
    {
        Schema schema = schemaService.getSchema( object.getClass() );

        if ( schema == null || haveOverrideAuthority( user ) )
        {
            return true;
        }

        List<String> anyAuthorities = schema.getAuthorityByType( AuthorityType.UPDATE );

        if ( anyAuthorities.isEmpty() )
        {
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE ) );
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE ) );
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PUBLIC ) );
        }

        if ( canAccess( user, anyAuthorities ) )
        {
            if ( !schema.isShareable() )
            {
                return true;
            }

            if ( checkSharingAccess( user, object ) &&
                (checkUser( user, object ) || checkSharingPermission( user, object, AccessStringHelper.Permission.WRITE )) )
            {
                return true;
            }
        }
        else if ( schema.isImplicitPrivateAuthority() && checkUser( user, object ) && checkSharingAccess( user, object ) )
        {
            return true;
        }

        return false;
    }

    @Override
    public boolean canDelete( User user, IdentifiableObject object )
    {
        Schema schema = schemaService.getSchema( object.getClass() );

        if ( schema == null || haveOverrideAuthority( user ) )
        {
            return true;
        }

        List<String> anyAuthorities = schema.getAuthorityByType( AuthorityType.DELETE );

        if ( anyAuthorities.isEmpty() )
        {
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE ) );
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE ) );
            anyAuthorities.addAll( schema.getAuthorityByType( AuthorityType.CREATE_PUBLIC ) );
        }

        if ( canAccess( user, anyAuthorities ) )
        {
            if ( !schema.isShareable() )
            {
                return true;
            }

            if ( checkUser( user, object ) || checkSharingPermission( user, object, AccessStringHelper.Permission.WRITE ) )
            {
                return true;
            }
        }
        else if ( schema.isImplicitPrivateAuthority() && checkUser( user, object ) )
        {
            return true;
        }

        return false;
    }

    @Override
    public boolean canManage( User user, IdentifiableObject object )
    {
        return canUpdate( user, object );
    }

    @Override
    public <T extends IdentifiableObject> boolean canRead( User user, Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );

        return schema == null || schema.getAuthorityByType( AuthorityType.READ ) == null
            || canAccess( user, schema.getAuthorityByType( AuthorityType.READ ) );
    }

    @Override
    public <T extends IdentifiableObject> boolean canCreate( User user, Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );

        if ( schema == null )
        {
            return false;
        }

        if ( !schema.isShareable() )
        {
            return canAccess( user, schema.getAuthorityByType( AuthorityType.CREATE ) );
        }

        return canMakePublic( user, klass ) || canMakePrivate( user, klass );
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakePublic( User user, Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );
        return !(schema == null || !schema.isShareable())
            && canAccess( user, schema.getAuthorityByType( AuthorityType.CREATE_PUBLIC ) );
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakePrivate( User user, Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );
        return !(schema == null || !schema.isShareable())
            && canAccess( user, schema.getAuthorityByType( AuthorityType.CREATE_PRIVATE ) );
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakeExternal( User user, Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );
        return !(schema == null || !schema.isShareable())
            && ((!schema.getAuthorityByType( AuthorityType.EXTERNALIZE ).isEmpty() && haveOverrideAuthority( user ))
            || haveAuthority( user, schema.getAuthorityByType( AuthorityType.EXTERNALIZE ) ));
    }

    @Override
    public <T extends IdentifiableObject> boolean defaultPrivate( Class<T> klass )
    {
        Schema schema = schemaService.getSchema( klass );
        return schema != null && schema.isDefaultPrivate();
    }

    @Override
    public <T extends IdentifiableObject> boolean defaultPublic( Class<T> klass )
    {
        return !defaultPrivate( klass );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Class<? extends IdentifiableObject> classForType( String type )
    {
        Schema schema = schemaService.getSchemaBySingularName( type );

        if ( schema != null && schema.isIdentifiableObject() )
        {
            return (Class<? extends IdentifiableObject>) schema.getKlass();
        }

        return null;
    }

    private boolean haveOverrideAuthority( User user )
    {
        return user == null || user.isSuper();
    }

    private boolean canAccess( User user, Collection<String> anyAuthorities )
    {
        return haveOverrideAuthority( user ) || anyAuthorities.isEmpty() || haveAuthority( user, anyAuthorities );
    }

    private boolean haveAuthority( User user, Collection<String> anyAuthorities )
    {
        return containsAny( user.getUserCredentials().getAllAuthorities(), anyAuthorities );
    }

    @Override
    public <T extends IdentifiableObject> Access getAccess( T object )
    {
        return getAccess( object, currentUserService.getCurrentUser() );
    }

    @Override
    public <T extends IdentifiableObject> Access getAccess( T object, User user )
    {
        if ( user.isSuper() )
        {
            return new Access( true );
        }

        Access access = new Access();
        access.setManage( canManage( user, object ) );
        access.setExternalize( canMakeExternal( user, object.getClass() ) );
        access.setWrite( canWrite( user, object ) );
        access.setRead( canRead( user, object ) );
        access.setUpdate( canUpdate( user, object ) );
        access.setDelete( canDelete( user, object ) );

        return access;
    }

    @Override
    public <T extends IdentifiableObject> void resetSharing( T object, User user )
    {
        if ( object == null || !isShareable( object.getClass() ) || user == null )
        {
            return;
        }

        BaseIdentifiableObject baseIdentifiableObject = (BaseIdentifiableObject) object;

        baseIdentifiableObject.setPublicAccess( AccessStringHelper.DEFAULT );
        baseIdentifiableObject.setExternalAccess( false );

        if ( object.getUser() == null )
        {
            baseIdentifiableObject.setUser( user );
        }

        if ( canMakePublic( user, object.getClass() ) )
        {
            if ( defaultPublic( object.getClass() ) )
            {
                baseIdentifiableObject.setPublicAccess( AccessStringHelper.READ_WRITE );
            }
        }

        object.getUserAccesses().clear();
        object.getUserGroupAccesses().clear();
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> verifySharing( T object, User user )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null || !isShareable( object.getClass() ) || user == null )
        {
            return errorReports;
        }

        if ( !AccessStringHelper.isValid( object.getPublicAccess() ) )
        {
            errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E3010, object.getPublicAccess() ) );
            return errorReports;
        }

        boolean canMakePublic = canMakePublic( user, object.getClass() );
        boolean canMakePrivate = canMakePrivate( user, object.getClass() );
        boolean canMakeExternal = canMakeExternal( user, object.getClass() );

        if ( object.getExternalAccess() )
        {
            if ( !canMakeExternal )
            {
                errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E3006, user.getUsername(), object.getClass() ) );
            }
        }

        if ( AccessStringHelper.DEFAULT.equals( object.getPublicAccess() ) )
        {
            if ( canMakePublic || canMakePrivate )
            {
                return errorReports;
            }

            errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E3009, user.getUsername(), object.getClass() ) );
        }
        else
        {
            if ( canMakePublic )
            {
                return errorReports;
            }

            errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E3008, user.getUsername(), object.getClass() ) );
        }

        return errorReports;
    }

    private boolean checkUser( User user, IdentifiableObject object )
    {
        if ( user == null || object.getUser() == null )
        {
            return false;
        }

        return user.equals( object.getUser() );
    }

    private boolean checkSharingAccess( User user, IdentifiableObject object )
    {
        boolean canMakePublic = canMakePublic( user, object.getClass() );
        boolean canMakePrivate = canMakePrivate( user, object.getClass() );
        boolean canMakeExternal = canMakeExternal( user, object.getClass() );

        if ( AccessStringHelper.DEFAULT.equals( object.getPublicAccess() ) )
        {
            if ( !(canMakePublic || canMakePrivate) )
            {
                return false;
            }
        }
        else
        {
            if ( !canMakePublic )
            {
                return false;
            }
        }

        if ( object.getExternalAccess() && !canMakeExternal )
        {
            return false;
        }

        return true;
    }

    private boolean checkSharingPermission( User user, IdentifiableObject object, AccessStringHelper.Permission permission )
    {
        if ( AccessStringHelper.isEnabled( object.getPublicAccess(), permission ) )
        {
            return true;
        }

        for ( UserGroupAccess userGroupAccess : object.getUserGroupAccesses() )
        {
            /* Is the user allowed to read this object through group access? */
            if ( AccessStringHelper.isEnabled( userGroupAccess.getAccess(), permission )
                && userGroupAccess.getUserGroup().getMembers().contains( user ) )
            {
                return true;
            }
        }

        for ( UserAccess userAccess : object.getUserAccesses() )
        {
            /* Is the user allowed to read to this object through user access? */
            if ( AccessStringHelper.isEnabled( userAccess.getAccess(), permission )
                && user.equals( userAccess.getUser() ) )
            {
                return true;
            }
        }

        return false;
    }
}

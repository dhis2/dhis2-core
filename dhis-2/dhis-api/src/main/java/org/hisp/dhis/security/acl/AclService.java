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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.User;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AclService
{
    /**
     * Is type supported for acl?
     *
     * @param type Type to check
     * @return true if type is supported
     */
    boolean isSupported( String type );

    /**
     * Is class supported for acl?
     *
     * @param klass Class to check
     * @return true if type is supported
     */
    boolean isSupported( Class<?> klass );

    /**
     * Is type supported for sharing?
     *
     * @param type Type to check
     * @return true if type is supported
     */
    boolean isShareable( String type );

    /**
     * Is class supported for sharing?
     *
     * @param klass Class to check
     * @return true if type is supported
     */
    boolean isShareable( Class<?> klass );

    /**
     * Can user read this object
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Is the user for the object null?
     * 3. Is the user of the object equal to current user?
     * 4. Is the object public read?
     * 5. Does any of the userGroupAccesses contain public read and the current user is in that group
     *
     * @param user   User to check against
     * @param object Object to check
     * @return Result of test
     */
    boolean canRead( User user, IdentifiableObject object );

    /**
     * Can user write to this object (create)
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Is the user for the object null?
     * 3. Is the user of the object equal to current user?
     * 4. Is the object public write?
     * 5. Does any of the userGroupAccesses contain public write and the current user is in that group
     *
     * @param user   User to check against
     * @param object Object to check
     * @return Result of test
     */
    boolean canWrite( User user, IdentifiableObject object );

    /**
     * Can user update this object
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Can user write to this object?
     *
     * @param user   User to check against
     * @param object Object to check
     * @return Result of test
     */
    boolean canUpdate( User user, IdentifiableObject object );

    /**
     * Can user delete this object
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Can user write to this object?
     *
     * @param user   User to check against
     * @param object Object to check
     * @return Result of test
     */
    boolean canDelete( User user, IdentifiableObject object );

    /**
     * Can user manage (make public) this object
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Can user write to this object?
     *
     * @param user   User to check against
     * @param object Object to check
     * @return Result of test
     */
    boolean canManage( User user, IdentifiableObject object );

    /**
     * Can read an objects of this type.
     *
     * @param user  User to User to check against
     * @param klass Type to check against
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean canRead( User user, Class<T> klass );

    /**
     * Can create an object of this type.
     *
     * @param user  User to User to check against
     * @param klass Type to check against
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean canCreate( User user, Class<T> klass );

    /**
     * Checks if a user can create a public instance of a certain object.
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Does user have the authority to create public instances of that object
     *
     * @param user  User to check against
     * @param klass Class to check
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean canMakePublic( User user, Class<T> klass );

    /**
     * Checks if a user can create a private instance of a certain object.
     * <p/>
     * 1. Does user have ACL_OVERRIDE_AUTHORITIES authority?
     * 2. Does user have the authority to create private instances of that object
     *
     * @param user  User to check against
     * @param klass Class to check
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean canMakePrivate( User user, Class<T> klass );

    /**
     * Can user make this object external? (read with no login)
     *
     * @param user  User to check against
     * @param klass Type to check
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean canMakeExternal( User user, Class<T> klass );

    /**
     * Is the default for this type to be private?
     *
     * @param klass Type to check
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean defaultPrivate( Class<T> klass );

    /**
     * Is the default for this type to be public?
     *
     * @param klass Type to check
     * @return Result of test
     */
    <T extends IdentifiableObject> boolean defaultPublic( Class<T> klass );

    Class<? extends IdentifiableObject> classForType( String type );

    /**
     * Return the access object for a object.
     *
     * @param object Object to check for access
     * @return Populated access instance
     */
    <T extends IdentifiableObject> Access getAccess( T object );

    /**
     * Return the access object for a object for a specific user.
     *
     * @param object Object to check for access
     * @param user   User to check against
     * @return Populated access instance
     */
    <T extends IdentifiableObject> Access getAccess( T object, User user );

    /**
     * Sets default sharing props on object, disregarding what is already there.
     *
     * @param object Object to update
     * @param user   User to base ACL on
     */
    <T extends IdentifiableObject> void resetSharing( T object, User user );

    /**
     * Verify that sharing props are correctly set according to user.
     *
     * @param object Object to update
     * @param user   User to base ACL on
     * @return List of error reports (if any)
     */
    <T extends IdentifiableObject> List<ErrorReport> verifySharing( T object, User user );
}

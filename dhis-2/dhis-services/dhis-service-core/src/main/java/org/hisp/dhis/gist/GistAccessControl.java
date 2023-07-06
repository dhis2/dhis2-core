/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.gist;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Access for the current user within the currently processed gist API request.
 *
 * <p>This encapsulates all access related logic of gist API request processing.
 *
 * @author Jan Bernitt
 */
public interface GistAccessControl {
  /**
   * @return ID of the current user
   */
  String getCurrentUserUid();

  /**
   * Whether or not the current user is a superuser.
   *
   * @return true, if the current user is a superuser, otherwise false
   */
  boolean isSuperuser();

  /**
   * Whether or not the current user is allowed to use Gist API {@code describe} to view the HQL
   * query.
   *
   * @return true, if the current user is allowed, otherwise false
   */
  boolean canReadHQL();

  boolean canRead(Class<? extends PrimaryKeyObject> type);

  boolean canReadObject(Class<? extends PrimaryKeyObject> type, String uid);

  /**
   * Whether or not the current user can read the field {@link Property} belonging to objects of the
   * {@link Schema} type.
   *
   * <p>This will be called to determine which fields to include in "all" fields and similar presets
   * and also to check that only accessible fields are shown to the current user.
   *
   * @param type of the owner object, e.g. a {@link org.hisp.dhis.organisationunit.OrganisationUnit}
   * @param path {@link Property} which is a member of the owner type and should be checked
   * @return true, if the current user can generally read the field value for objects of the
   *     provided type (sharing may still disallow this for individual values which are filtered by
   *     added sharing based filters to gist queries)
   */
  boolean canRead(Class<? extends PrimaryKeyObject> type, String path);

  boolean canFilterByAccessOfUser(String userUid);

  /**
   * The {@link Access} capabilities of the current user given the {@link Sharing} and type of
   * object the sharing belongs to.
   *
   * <p>This is called for every result row to convert the {@link Sharing} information as stored in
   * the database into the {@link Access} if that field were requested explicitly.
   *
   * @param type of the owner object, e.g. a {@link org.hisp.dhis.organisationunit.OrganisationUnit}
   * @param value actual {@link Sharing} value of the {@code sharing} property of an object of the
   *     provided type to use to compute the {@link Access} for the current user
   * @return {@link Access} for the current user given the provided {@link Sharing} value
   */
  Access asAccess(Class<? extends IdentifiableObject> type, Sharing value);

  String createAccessFilterHQL(String tableName);
}

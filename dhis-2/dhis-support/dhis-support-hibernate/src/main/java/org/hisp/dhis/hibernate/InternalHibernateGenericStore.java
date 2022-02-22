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
package org.hisp.dhis.hibernate;

import java.util.List;
import java.util.function.Function;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.User;

/**
 * Interface which extends GenericStore and exposes support methods for
 * retrieving criteria.
 *
 * @author Lars Helge Overland
 */
public interface InternalHibernateGenericStore<T>
    extends GenericStore<T>
{
    List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder );

    List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User userInfo );

    List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, String access );

    List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User user, String access );

    List<Function<Root<T>, Predicate>> getSharingPredicates( CriteriaBuilder builder, User userInfo,
        CurrentUserGroupInfo groupInfo, String access );

    List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder );

    List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User userInfo );

    List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, String access );

    List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User userInfo,
        CurrentUserGroupInfo groupInfo, String access );

    List<Function<Root<T>, Predicate>> getDataSharingPredicates( CriteriaBuilder builder, User user, String access );
}
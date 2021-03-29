/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.query.member;

import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;

/**
 * Gives convenient access to (potentially large) member collections of an owner
 * object.
 *
 * The preferred method should be to {@link #queryMemberItems(MemberQuery)}
 * which returns a raw data row for each match only containing the values of the
 * {@link MemberQuery#getFields()} in form of an {@link Object[]}.
 *
 * @author Jan Bernitt
 */
public interface MemberQueryService
{
    /**
     * Before running one of the query methods a {@link MemberQuery} should be
     * rectified. This gives the service a change to correct the query and set
     * some defaults.
     *
     * The result is exposed again so that the caller can use the modified
     * {@link MemberQuery} in subsequent processing steps.
     *
     * @param query A {@link MemberQuery} as build by the caller
     * @param <T> type of member collection elements
     * @return the rectified {@link MemberQuery} that should be used to query
     *         results. This might be the same instance as the provided one or a
     *         modified "copy".
     */
    <T extends IdentifiableObject> MemberQuery<T> rectifyQuery( MemberQuery<T> query );

    List<?> queryMemberItems( MemberQuery<?> query );

    <T extends IdentifiableObject> List<T> queryMemberItemsAsObjects( MemberQuery<T> query );
}

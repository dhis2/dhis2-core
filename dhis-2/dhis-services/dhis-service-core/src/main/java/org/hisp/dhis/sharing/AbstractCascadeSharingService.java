/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.sharing;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

public abstract class AbstractCascadeSharingService
{
    /**
     * Merge UserAccesses and UserGroupAccess of source object's sharing to
     * target object's sharing. Do nothing if target sharing object has
     * publicAccess enabled.
     */
    protected <S extends IdentifiableObject, T extends IdentifiableObject> T mergeSharing( S source, T target,
        CascadeSharingParameters parameters )
    {
        if ( AccessStringHelper.canRead( target.getSharing().getPublicAccess() ) )
        {
            return target;
        }

        mergeAccessObject( source, target, UserAccess.class, source.getSharing().getUsers(),
            target.getSharing().getUsers(), parameters );
        mergeAccessObject( source, target, UserGroupAccess.class, source.getSharing().getUserGroups(),
            target.getSharing().getUserGroups(), parameters );

        return target;
    }

    /**
     * Merge {@link AccessObject} from source to target
     * {@code Map<String,AccessObject>}
     */
    private <T extends AccessObject> Map<String, T> mergeAccessObject( IdentifiableObject sourceObject,
        IdentifiableObject targetObject, Class<T> accessClass, Map<String, T> source,
        Map<String, T> target, CascadeSharingParameters parameters )
    {
        if ( MapUtils.isEmpty( source ) )
        {
            return target;
        }

        source.values().forEach( sourceAccess -> {

            if ( !AccessStringHelper.canRead( sourceAccess.getAccess() ) )
            {
                parameters.getReport().getErrorReports()
                    .add( new ErrorReport( sourceObject.getClass(), ErrorCode.E3019, accessClass,
                        sourceAccess.getId(), sourceObject.getClass(), sourceObject.getId() ) );
                return;
            }

            T targetAccess = target.get( sourceAccess.getId() );

            if ( targetAccess == null )
            {
                targetAccess = sourceAccess;
                targetAccess.setAccess( AccessStringHelper.READ );
            }
            else if ( !AccessStringHelper.canRead( targetAccess.getAccess() ) )
            {
                targetAccess.setAccess( AccessStringHelper.READ );
            }

            target.put( targetAccess.getId(), targetAccess );

            updateReport( parameters.getReport(), targetObject.getClass(), targetAccess, accessClass );
        } );

        return target;
    }

    protected boolean canUpdate( CascadeSharingParameters parameters )
    {
        return !parameters.isDryRun() || (parameters.isAtomic() || parameters.getReport().getErrorReports().isEmpty());
    }

    protected void updateReport( CascadeSharingReport report, Class clazz, AccessObject accessObject,
        Class accessClass )
    {
        Map<Class, AccessObject> clazzReport = report.getUpdatedObjects().get( clazz );

        if ( clazzReport == null )
        {
            clazzReport = new HashMap<>();
        }

        clazzReport.put( accessClass, accessObject );
    }
}

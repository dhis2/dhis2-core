package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

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

import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.addObjectReports;

import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.user.User;

/**
 * @author Luciano Fiandesio
 */
public class SecurityCheck
    implements
    ValidationCheck
{
    @Override
    public TypeReport check( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext context )
    {

        if ( importStrategy.isCreateAndUpdate() )
        {
            TypeReport report = runValidationCheck( bundle, klass, persistedObjects, ImportStrategy.UPDATE, context );
            report.merge( runValidationCheck( bundle, klass, nonPersistedObjects, ImportStrategy.CREATE, context ) );
            return report;
        }
        else if ( importStrategy.isCreate() )
        {
            return runValidationCheck( bundle, klass, nonPersistedObjects, ImportStrategy.CREATE, context );
        }
        else if ( importStrategy.isUpdate() )
        {
            return runValidationCheck( bundle, klass, persistedObjects, ImportStrategy.UPDATE, context );
        }
        else
        {
            return new TypeReport( klass );
        }

    }

    private TypeReport runValidationCheck( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> objects, ImportStrategy importMode, ValidationContext ctx )
    {

        TypeReport typeReport = new TypeReport( klass );

        if ( objects == null || objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();
        PreheatIdentifier identifier = bundle.getPreheatIdentifier();

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();

            if ( importMode.isCreate() )
            {
                if ( !ctx.getAclService().canCreate( bundle.getUser(), klass ) )
                {
                    ErrorReport errorReport = new ErrorReport( klass, ErrorCode.E3000,
                        identifier.getIdentifiersWithName( bundle.getUser() ),
                        identifier.getIdentifiersWithName( object ) );

                    ValidationUtils.addObjectReport( errorReport, typeReport, object, bundle);
                    ctx.markForRemoval( object );
                    continue;
                }
            }
            else
            {
                IdentifiableObject persistedObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );

                if ( importMode.isUpdate() )
                {
                    if ( !ctx.getAclService().canUpdate( bundle.getUser(), persistedObject ) )
                    {
                        ErrorReport errorReport = new ErrorReport( klass, ErrorCode.E3001,
                            identifier.getIdentifiersWithName( bundle.getUser() ),
                            identifier.getIdentifiersWithName( object ) );

                        ValidationUtils.addObjectReport( errorReport, typeReport, object, bundle);
                        ctx.markForRemoval( object );
                        continue;
                    }
                }
                else if ( importMode.isDelete() )
                {
                    if ( !ctx.getAclService().canDelete( bundle.getUser(), persistedObject ) )
                    {
                        ErrorReport errorReport = new ErrorReport( klass, ErrorCode.E3002,
                            identifier.getIdentifiersWithName( bundle.getUser() ),
                            identifier.getIdentifiersWithName( object ) );

                        ValidationUtils.addObjectReport( errorReport, typeReport, object, bundle);

                        ctx.markForRemoval( object );
                        continue;
                    }
                }
            }

            if ( object instanceof User )
            {
                User user = (User) object;
                List<ErrorReport> errorReports = ctx.getUserService().validateUser( user, bundle.getUser() );

                if ( !errorReports.isEmpty() ) {

                    addObjectReports( errorReports, typeReport, object, bundle );
                    ctx.markForRemoval( object );
                }
            }

            List<ErrorReport> sharingErrorReports = ctx.getAclService().verifySharing( object, bundle.getUser() );
            if ( !sharingErrorReports.isEmpty() )
            {
                addObjectReports( sharingErrorReports, typeReport, object, bundle );
                ctx.markForRemoval( object );

            }
        }

        return typeReport;
    }


}

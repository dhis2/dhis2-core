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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleHook;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserService;

/**
 * @author Luciano Fiandesio
 */
public class ValidationContext
{

    private List<ObjectBundleHook> objectBundleHooks;

    private SchemaValidator schemaValidator;

    private AclService aclService;

    private UserService userService;

    private SchemaService schemaService;

    private List<IdentifiableObject> markedForRemoval = new ArrayList<>();

    public ValidationContext( List<ObjectBundleHook> objectBundleHooks, SchemaValidator schemaValidator,
        AclService aclService, UserService userService, SchemaService schemaService )
    {
        this.objectBundleHooks = objectBundleHooks;
        this.schemaValidator = schemaValidator;
        this.aclService = aclService;
        this.userService = userService;
        this.schemaService = schemaService;
    }

    public List<ObjectBundleHook> getObjectBundleHooks()
    {
        return objectBundleHooks;
    }

    public SchemaValidator getSchemaValidator()
    {
        return schemaValidator;
    }

    public AclService getAclService()
    {
        return aclService;
    }

    public UserService getUserService()
    {
        return userService;
    }

    public SchemaService getSchemaService()
    {
        return schemaService;
    }

    public void markForRemoval( IdentifiableObject object )
    {
        this.markedForRemoval.add( object );
    }

    public List<IdentifiableObject> getMarkedForRemoval()
    {
        return markedForRemoval;
    }
}

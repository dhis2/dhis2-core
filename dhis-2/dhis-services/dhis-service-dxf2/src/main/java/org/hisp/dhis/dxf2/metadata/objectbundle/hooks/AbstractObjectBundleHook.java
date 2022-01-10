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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleHook;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AbstractObjectBundleHook<T> implements ObjectBundleHook<T>
{
    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected PreheatService preheatService;

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected MergeService mergeService;

    @Override
    public void validate( T object, ObjectBundle bundle,
        Consumer<ErrorReport> addReports )
    {
        // by default nothing to validate
    }

    @Override
    public void preCommit( ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void postCommit( ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public <E extends T> void preTypeImport( Class<E> klass, List<E> objects, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public <E extends T> void postTypeImport( Class<E> klass, List<E> objects, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void preCreate( T object, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void postCreate( T persistedObject, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        // by default nothing to do
    }

    @Override
    public void preDelete( T persistedObject, ObjectBundle bundle )
    {
        // by default nothing to do
    }
}

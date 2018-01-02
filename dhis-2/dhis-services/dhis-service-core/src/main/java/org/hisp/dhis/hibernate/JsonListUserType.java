package org.hisp.dhis.hibernate;

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Adrian Quintana
 */
public class JsonListUserType extends JsonUserType implements UserCollectionType {

    @Override
    public JavaType createJavaType(ObjectMapper mapper) {
        return mapper.getTypeFactory().constructCollectionType(List.class, returnedClass());
    }

    @Override
    public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
            throws HibernateException {
        return new PersistentList(session);
    }

    private PersistentList cast(Object collection) {
        return (PersistentList) collection;
    }

    @Override
    public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
        return new PersistentList(session, (List<?>) collection);
    }

    @Override
    public Iterator<?> getElementsIterator(Object collection) {
        return cast(collection).iterator();
    }

    @Override
    public boolean contains(Object collection, Object entity) {
        return cast(collection).contains(entity);
    }

    @Override
    public Object indexOf(Object collection, Object entity) {
        return cast(collection).indexOf(entity);
    }

    @Override
    public Object replaceElements(Object original, Object target, CollectionPersister persister, Object owner,
            @SuppressWarnings("rawtypes") Map copyCache, SharedSessionContractImplementor session) throws HibernateException {

        PersistentList originalList = cast(original);
        PersistentList targetList = cast(target);
        targetList.clear();
        targetList.addAll(originalList);

        return target;
    }

    @Override
    public Object instantiate(int anticipatedSize) {
        return new PersistentList();
    }
}
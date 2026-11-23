/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.cache.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hibernate.cache.spi.support.AbstractCachedDomainDataAccess;
import org.hibernate.cache.spi.support.AbstractCollectionDataAccess;
import org.hibernate.cache.spi.support.AbstractEntityDataAccess;
import org.hibernate.cache.spi.support.CollectionNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.EntityNonStrictReadWriteAccess;
import org.junit.jupiter.api.Test;

/**
 * Pins the declared method surface of the five Hibernate superclasses of {@link
 * GuardedEntityNonStrictReadWriteAccess} and {@link GuardedCollectionNonStrictReadWriteAccess}.
 *
 * <p>The guard's invariant is that every inherited path which writes or clears region storage is
 * overridden and records into the {@link EvictionGuard} first. If a Hibernate upgrade adds a
 * storage-touching method to a superclass, the subclasses still compile, every other test passes,
 * and the stale-put window silently reopens for that path. This test turns that one silent failure
 * mode into a loud one.
 *
 * <p>A failure here means the inherited surface changed, which is not by itself a defect: read the
 * new entry, decide whether it can reach storage, extend the guarded subclasses if it can, and only
 * then update the expected set. Signatures use simple type names, enough to separate the existing
 * overloads.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GuardedAccessSuperclassSurfaceTest {

  @Test
  void abstractCachedDomainDataAccessSurfaceIsUnchanged() {
    assertSurface(
        AbstractCachedDomainDataAccess.class,
        "getRegion()",
        "getStorageAccess()",
        "clearCache()",
        "contains(Object)",
        "get(SharedSessionContractImplementor,Object)",
        "putFromLoad(SharedSessionContractImplementor,Object,Object,Object)",
        "putFromLoad(SharedSessionContractImplementor,Object,Object,Object,boolean)",
        "lockRegion()",
        "unlockRegion(SoftLock)",
        "remove(SharedSessionContractImplementor,Object)",
        "removeAll(SharedSessionContractImplementor)",
        "evict(Object)",
        "evictAll()",
        "destroy()");
  }

  @Test
  void abstractEntityDataAccessSurfaceIsUnchanged() {
    assertSurface(
        AbstractEntityDataAccess.class,
        "generateCacheKey(Object,EntityPersister,SessionFactoryImplementor,String)",
        "getCacheKeyId(Object)",
        "lockRegion()",
        "unlockRegion(SoftLock)",
        "lockItem(SharedSessionContractImplementor,Object,Object)",
        "unlockItem(SharedSessionContractImplementor,Object,SoftLock)");
  }

  @Test
  void abstractCollectionDataAccessSurfaceIsUnchanged() {
    assertSurface(
        AbstractCollectionDataAccess.class,
        "generateCacheKey(Object,CollectionPersister,SessionFactoryImplementor,String)",
        "getCacheKeyId(Object)",
        "lockItem(SharedSessionContractImplementor,Object,Object)",
        "unlockItem(SharedSessionContractImplementor,Object,SoftLock)",
        "lockRegion()",
        "unlockRegion(SoftLock)");
  }

  @Test
  void entityNonStrictReadWriteAccessSurfaceIsUnchanged() {
    assertSurface(
        EntityNonStrictReadWriteAccess.class,
        "getAccessType()",
        "insert(SharedSessionContractImplementor,Object,Object,Object)",
        "afterInsert(SharedSessionContractImplementor,Object,Object,Object)",
        "update(SharedSessionContractImplementor,Object,Object,Object,Object)",
        "afterUpdate(SharedSessionContractImplementor,Object,Object,Object,Object,SoftLock)",
        "unlockItem(SharedSessionContractImplementor,Object,SoftLock)",
        "remove(SharedSessionContractImplementor,Object)");
  }

  @Test
  void collectionNonStrictReadWriteAccessSurfaceIsUnchanged() {
    assertSurface(
        CollectionNonStrictReadWriteAccess.class,
        "getAccessType()",
        "unlockItem(SharedSessionContractImplementor,Object,SoftLock)");
  }

  private static void assertSurface(Class<?> type, String... expected) {
    assertEquals(
        new TreeSet<>(Set.of(expected)),
        declaredSurfaceOf(type),
        () ->
            "the declared method surface of "
                + type.getName()
                + " changed; see this test class's javadoc before updating the expected set");
  }

  /** The methods a subclass in another package can override: the public and protected ones. */
  private static Set<String> declaredSurfaceOf(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        .filter(method -> !method.isSynthetic())
        .filter(
            method ->
                Modifier.isPublic(method.getModifiers())
                    || Modifier.isProtected(method.getModifiers()))
        .map(GuardedAccessSuperclassSurfaceTest::signatureOf)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String signatureOf(Method method) {
    return Arrays.stream(method.getParameterTypes())
        .map(Class::getSimpleName)
        .collect(Collectors.joining(",", method.getName() + "(", ")"));
  }
}

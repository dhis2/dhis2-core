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

import java.util.Objects;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class HibernateProxyUtils {
  private HibernateProxyUtils() {
    throw new IllegalStateException("Utility class");
  }

  @SuppressWarnings("rawtypes")
  public static Class getRealClass(Object object) {
    Objects.requireNonNull(object);

    if (object instanceof Class) {
      throw new IllegalArgumentException("Input can't be a Class instance!");
    }

    return getClassWithoutInitializingProxy(object);
  }

  /**
   * Get the class of an instance or the underlying class of a proxy (without initializing the
   * proxy!). It is almost always better to use the entity name!
   */
  public static Class getClassWithoutInitializingProxy(Object object) {
    if (object instanceof HibernateProxy) {
      HibernateProxy proxy = (HibernateProxy) object;
      LazyInitializer li = proxy.getHibernateLazyInitializer();
      return li.getPersistentClass();
    } else {
      return object.getClass();
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T unproxy(T proxy) {
    return (T) Hibernate.unproxy(proxy);
  }

  public static <T> void initializeAndUnproxy(T entity) {
    if (entity == null) {
      return;
    }

    Hibernate.initialize(entity);
    if (entity instanceof HibernateProxy) {
      ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
    }
  }
}

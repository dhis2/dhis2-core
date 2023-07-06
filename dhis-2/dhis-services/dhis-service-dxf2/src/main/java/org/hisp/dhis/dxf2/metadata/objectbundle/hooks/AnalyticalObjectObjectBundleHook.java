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

import lombok.AllArgsConstructor;
import org.hibernate.Session;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.dxf2.metadata.AnalyticalObjectImportHandler;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Schema;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Order(10)
@AllArgsConstructor
public class AnalyticalObjectObjectBundleHook extends AbstractObjectBundleHook<AnalyticalObject> {
  private final AnalyticalObjectImportHandler analyticalObjectImportHandler;

  @Override
  public void preCreate(AnalyticalObject object, ObjectBundle bundle) {
    BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;
    Schema schema =
        schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(analyticalObject));
    Session session = sessionFactory.getCurrentSession();

    analyticalObjectImportHandler.handleAnalyticalObject(session, schema, analyticalObject, bundle);
  }

  @Override
  public void preUpdate(
      AnalyticalObject object, AnalyticalObject persistedObject, ObjectBundle bundle) {
    BaseAnalyticalObject analyticalObject = (BaseAnalyticalObject) object;

    Schema schema =
        schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(analyticalObject));
    Session session = sessionFactory.getCurrentSession();

    analyticalObjectImportHandler.handleAnalyticalObject(session, schema, analyticalObject, bundle);
  }
}

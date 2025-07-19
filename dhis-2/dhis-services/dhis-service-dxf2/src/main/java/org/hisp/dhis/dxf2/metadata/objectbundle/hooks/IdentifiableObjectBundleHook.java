/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.SortableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Order(0)
@AllArgsConstructor
public class IdentifiableObjectBundleHook extends AbstractObjectBundleHook<IdentifiableObject> {
  private final AclService aclService;

  @Override
  public void preCreate(IdentifiableObject identifiableObject, ObjectBundle bundle) {
    identifiableObject.setAutoFields();
    identifiableObject.setLastUpdatedBy(
        getSession().getReference(User.class, bundle.getUserDetails().getId()));
    identifiableObject.setCreatedBy(
        getSession().getReference(User.class, bundle.getUserDetails().getId()));

    if (identifiableObject.getSharing().getOwner() == null) {
      identifiableObject.getSharing().setOwner(identifiableObject.getCreatedBy());
    }

    Schema schema =
        schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(identifiableObject));
    handleAttributeValues(identifiableObject, schema);
    handleSkipSharing(identifiableObject, bundle);
    handleSkipTranslation(identifiableObject, bundle);
    handleSortOrder(identifiableObject, bundle, schema);
  }

  /**
   * This method loops through all sortable List of given object and sets the sortOrder value for
   * each item in the List if it is not already set.
   *
   * @param identifiableObject Object to set sortOrder on
   * @param bundle {@link ObjectBundle}
   * @param schema Schema of given object
   */
  private void handleSortOrder(
      IdentifiableObject identifiableObject, ObjectBundle bundle, Schema schema) {
    findSortableProperty(schema)
        .forEach(
            property -> {
              List<IdentifiableObject> collection =
                  ListUtils.emptyIfNull(
                      ReflectionUtils.invokeGetterMethod(
                          property.getFieldName(), identifiableObject));
              for (int i = 0; i < collection.size(); i++) {
                IdentifiableObject item = collection.get(i);
                IdentifiableObject preheatedItem =
                    bundle.getPreheat().get(bundle.getPreheatIdentifier(), item);
                if (preheatedItem == null) {
                  continue;
                }
                SortableObject sortableObject = (SortableObject) preheatedItem;
                if (sortableObject.getSortOrder() == null || sortableObject.getSortOrder() != i) {
                  sortableObject.setSortOrder(i);
                  bundle.getPreheat().put(bundle.getPreheatIdentifier(), preheatedItem);
                }
              }
              bundle.getPreheat().put(bundle.getPreheatIdentifier(), identifiableObject);
            });
  }

  /**
   * Find all properties of given Schema class that are of type List which contains SortableObjects
   * interface. The property must also have a property called sortOrder.
   *
   * @param schema Schema class to search for sortable properties
   * @return List of properties that are sortable
   */
  private List<Property> findSortableProperty(Schema schema) {
    return schema.getPersistedProperties().values().stream()
        .filter(
            p ->
                p.getKlass().isAssignableFrom(List.class)
                    && p.getItemKlass() != null
                    && SortableObject.class.isAssignableFrom(p.getItemKlass()))
        .toList();
  }

  @Override
  public void preUpdate(
      IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle) {
    object.setAutoFields();
    object.setLastUpdatedBy(getSession().getReference(User.class, bundle.getUserDetails().getId()));

    handleCreatedByProperty(object, persistedObject, bundle);

    Schema schema = schemaService.getDynamicSchema(HibernateProxyUtils.getRealClass(object));
    handleAttributeValues(object, schema);
    handleSortOrder(object, bundle, schema);
  }

  private void handleAttributeValues(IdentifiableObject object, Schema schema) {
    if (!schema.hasPersistedProperty("attributeValues")) return;

    object.setAttributeValues(
        object.getAttributeValues().mapValues(v -> v.replaceAll("\\s{2,}", StringUtils.SPACE)));
  }

  /**
   * Make sure createdBy is immutable unless persistedObject.createdBy is null.
   *
   * @param object object from payload.
   * @param persistedObject persistedObject.
   * @param bundle The {@link ObjectBundle} of the importing service.
   */
  private void handleCreatedByProperty(
      IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle) {
    if (persistedObject.getCreatedBy() == null) {
      object.setCreatedBy(
          object.getCreatedBy() != null
              ? object.getCreatedBy()
              : getSession().getReference(User.class, bundle.getUserDetails().getId()));
    } else if (!IdentifiableObjectUtils.equalsByUid(
        object.getCreatedBy(), persistedObject.getCreatedBy())) {
      object.setCreatedBy(persistedObject.getCreatedBy());
      bundle.getPreheat().put(PreheatIdentifier.UID, object.getCreatedBy());
    }
  }

  private void handleSkipSharing(IdentifiableObject identifiableObject, ObjectBundle bundle) {
    if (!bundle.isSkipSharing()) return;

    aclService.clearSharing(identifiableObject, bundle.getUserDetails());
  }

  private void handleSkipTranslation(IdentifiableObject identifiableObject, ObjectBundle bundle) {
    if (bundle.isSkipTranslation()
        && !CollectionUtils.isEmpty(identifiableObject.getTranslations())) {
      identifiableObject.getTranslations().clear();
    }
  }
}

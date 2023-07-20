package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hisp.dhis.common.SortableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.springframework.stereotype.Component;

@Component
public class SortableObjectBundleHook extends AbstractObjectBundleHook<SortableObject>{

  @Override
  public void preCreate(SortableObject sortableObject, ObjectBundle bundle) {
      if (sortableObject.getSortOrder() == null) {
        sortableObject.setSortOrder(0);
      }
  }

  @Override
  public void preUpdate(
      SortableObject sortableObject, SortableObject persistedObject, ObjectBundle bundle) {
      if (sortableObject.getSortOrder() == null) {
        sortableObject.setSortOrder(0);
      }
  }
}

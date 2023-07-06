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
package org.hisp.dhis.sharing;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.sharing.Sharing;

abstract class CascadeSharingTest extends TransactionalIntegrationTest {
  protected DimensionalItemObject baseDimensionalItemObject(
      final String dimensionItem, DimensionItemType type) {
    final BaseDimensionalItemObject baseDimensionalItemObject =
        new BaseDimensionalItemObject(dimensionItem);
    baseDimensionalItemObject.setDimensionItemType(type);
    return baseDimensionalItemObject;
  }

  protected DataElement createDEWithDefaultSharing(char name) {
    DataElement dataElement = createDataElement(name);
    dataElement.setSharing(Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build());
    return dataElement;
  }

  protected Sharing defaultSharing() {
    return Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build();
  }

  protected Dashboard createDashboard(String name, Sharing sharing) {
    Dashboard dashboard = new Dashboard();
    dashboard.setName("dashboard" + name);
    dashboard.setSharing(sharing);
    return dashboard;
  }

  protected Map createMap(String name) {
    MapView mapView = createMapView("Test");
    Map map = new Map();
    map.setName("map" + name);
    map.setMapViews(Lists.newArrayList(mapView));
    map.setAutoFields();
    return map;
  }
}

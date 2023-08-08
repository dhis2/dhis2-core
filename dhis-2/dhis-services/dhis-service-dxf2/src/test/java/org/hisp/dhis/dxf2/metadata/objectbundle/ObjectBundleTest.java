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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ObjectBundle}.
 *
 * @author Volker Schmidt
 */
class ObjectBundleTest {

  private ObjectBundle objectBundle;

  private ObjectBundleParams objectBundleParams;

  private Preheat preheat;

  private Attribute attribute1 = new Attribute();

  private Attribute attribute2 = new Attribute();

  private Attribute attribute3 = new Attribute();

  private Category category1 = new Category();

  private Category category2 = new Category();

  @BeforeEach
  void setUp() {
    attribute1.setUid("u1");
    attribute2.setUid("u2");
    attribute2.setUid("u3");
    category1.setUid("u7");
    category2.setUid("u8");
    objectBundleParams = new ObjectBundleParams();
    preheat = new Preheat();
    final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap =
        new HashMap<>();
    objectMap.put(Attribute.class, new ArrayList<>());
    objectMap.put(Category.class, new ArrayList<>());
    objectMap.get(Attribute.class).add(attribute1);
    objectMap.get(Attribute.class).add(attribute2);
    objectMap.get(Attribute.class).add(attribute3);
    objectMap.get(Category.class).add(category1);
    objectMap.get(Category.class).add(category2);
    preheat.put(PreheatIdentifier.UID, attribute1);
    preheat.put(PreheatIdentifier.UID, attribute3);
    preheat.put(PreheatIdentifier.UID, category1);
    objectBundle = new ObjectBundle(objectBundleParams, preheat, objectMap);
  }

  @Test
  void objectIndex() {
    Assertions.assertEquals((Integer) 2, objectBundle.mergeObjectIndex(attribute3));
    Assertions.assertEquals((Integer) 0, objectBundle.mergeObjectIndex(attribute1));
    Assertions.assertEquals((Integer) 1, objectBundle.mergeObjectIndex(attribute2));
    Assertions.assertEquals((Integer) 1, objectBundle.mergeObjectIndex(category2));
    Assertions.assertEquals((Integer) 0, objectBundle.mergeObjectIndex(category1));
  }

  @Test
  void containsObject() {
    Assertions.assertTrue(objectBundle.containsObject(attribute2));
  }

  @Test
  void containsObjectNot() {
    Assertions.assertFalse(objectBundle.containsObject(new Attribute()));
  }
}

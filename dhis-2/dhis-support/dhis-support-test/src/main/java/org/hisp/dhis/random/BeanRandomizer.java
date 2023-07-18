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
package org.hisp.dhis.random;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.FileTypeValueOptions;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.period.PeriodType;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class BeanRandomizer {
  private final EasyRandom rnd;

  private BeanRandomizer(EasyRandom rnd) {
    this.rnd = rnd;
  }

  private static EasyRandomParameters baseParameters() {
    return new EasyRandomParameters()
        .randomize(ValueTypeOptions.class, FileTypeValueOptions::new)
        .randomize(PeriodType.class, new PeriodTypeRandomizer())
        .randomize(Geometry.class, new GeometryRandomizer())
        .randomize(
            FieldPredicates.named("uid").and(FieldPredicates.ofType(String.class)),
            new UidRandomizer())
        .randomize(
            FieldPredicates.named("id").and(FieldPredicates.ofType(long.class)),
            new IdRandomizer());
  }

  /**
   * Creates a BeanRandomizer instance for you to generate random Java beans. It will be
   * pre-configured with DHIS2 specific randomizers (for uid, id, ... fields).
   *
   * @return an instance of BeanRandomizer
   */
  public static BeanRandomizer create() {
    return new BeanRandomizer(new EasyRandom(baseParameters()));
  }

  /**
   * Creates a BeanRandomizer instance for you to generate random Java beans. It will be
   * pre-configured with DHIS2 specific randomizers (for uid, id, ... fields).
   *
   * @param inClass exclude fields in given class
   * @param excludeFields exclude fields from being filled with random data
   * @return an instance of BeanRandomizer
   */
  public static BeanRandomizer create(final Class<?> inClass, final String... excludeFields) {
    EasyRandomParameters params = baseParameters();
    for (String field : excludeFields) {
      params.excludeField(FieldPredicates.named(field).and(FieldPredicates.inClass(inClass)));
    }
    return new BeanRandomizer(new EasyRandom(params));
  }

  /**
   * Creates a BeanRandomizer instance for you to generate random Java beans. It will be
   * pre-configured with DHIS2 specific randomizers (for uid, id, ... fields).
   *
   * @param excludeFields exclude fields in given class from being filled with random data
   * @return an instance of BeanRandomizer
   */
  public static BeanRandomizer create(final Map<Class<?>, Set<String>> excludeFields) {
    EasyRandomParameters params = baseParameters();
    for (Map.Entry<Class<?>, Set<String>> field : excludeFields.entrySet()) {
      for (String name : field.getValue()) {
        params.excludeField(
            FieldPredicates.named(name).and(FieldPredicates.inClass(field.getKey())));
      }
    }
    return new BeanRandomizer(new EasyRandom(params));
  }

  public <T> T nextObject(final Class<T> type) {
    return rnd.nextObject(type);
  }

  public <T> Stream<T> objects(final Class<T> type, final int streamSize) {
    return rnd.objects(type, streamSize);
  }
}

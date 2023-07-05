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
package org.hisp.dhis.parser.expression.function;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;

/**
 * Vector function: percentile (base class)
 *
 * <p>All percentile functions take two arguments:
 *
 * <p>percentile... ( values, fraction )
 *
 * <p>The percentile is computed according to the EstimationType of the subclass.
 *
 * @author Jim Grace
 */
public abstract class VectorPercentileBase extends VectorFunction {
  private final Percentile percentile = new Percentile().withEstimationType(getEstimationType());

  @Override
  public Object aggregate(List<Double> values, List<Double> args) {
    Double fraction = args.get(0);

    if (values.size() == 0 || fraction == null || fraction < 0d || fraction > 1d) {
      return null;
    }

    Collections.sort(values);

    if (fraction == 0d) {
      return values.get(0);
    }

    double[] vals = ArrayUtils.toPrimitive(values.toArray(new Double[0]));

    return percentile.evaluate(vals, fraction * 100.);
  }

  /**
   * Each subclass defines its percentile estimation type.
   *
   * @return the percentile estimation type.
   */
  protected abstract EstimationType getEstimationType();
}

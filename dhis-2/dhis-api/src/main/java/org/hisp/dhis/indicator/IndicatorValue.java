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
package org.hisp.dhis.indicator;

/**
 * Non-persisted class for representing the various components of an aggregated indicator value.
 *
 * @author Lars Helge Overland
 */
public class IndicatorValue {
  private double numeratorValue;

  private double denominatorValue;

  private int multiplier;

  private int divisor;

  public IndicatorValue() {}

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  /** Returns the calculated indicator value. */
  public double getValue() {
    return (numeratorValue * multiplier) / (denominatorValue * divisor);
  }

  /** Returns the ratio of the multiplier and divisor. */
  public double getFactor() {
    return ((double) multiplier) / ((double) divisor);
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  public double getNumeratorValue() {
    return numeratorValue;
  }

  public IndicatorValue setNumeratorValue(double numeratorValue) {
    this.numeratorValue = numeratorValue;
    return this;
  }

  public double getDenominatorValue() {
    return denominatorValue;
  }

  public IndicatorValue setDenominatorValue(double denominatorValue) {
    this.denominatorValue = denominatorValue;
    return this;
  }

  public int getMultiplier() {
    return multiplier;
  }

  public IndicatorValue setMultiplier(int multiplier) {
    this.multiplier = multiplier;
    return this;
  }

  public int getDivisor() {
    return divisor;
  }

  public IndicatorValue setDivisor(int divisor) {
    this.divisor = divisor;
    return this;
  }
}

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
package org.hisp.dhis.analytics;

import java.util.HashSet;
import java.util.Set;

/**
 * Class representing analytics table partitions.
 *
 * @author Lars Helge Overland
 */
public class Partitions {
  /** Yearly partitions containing four-digit years. */
  private Set<Integer> partitions = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Partitions() {}

  public Partitions(Set<Integer> partitions) {
    this.partitions = partitions;
  }

  public Partitions(Partitions partitions) {
    this.partitions =
        partitions != null ? new HashSet<>(partitions.getPartitions()) : new HashSet<Integer>();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Adds a partition. */
  public Partitions add(Integer partition) {
    partitions.add(partition);
    return this;
  }

  /** Indicates whether this instance contains multiple partitions. */
  public boolean isMultiple() {
    return partitions != null && partitions.size() > 1;
  }

  /** Indicates whether this instance has any partitions. */
  public boolean hasAny() {
    return partitions != null && !partitions.isEmpty();
  }

  /** Indicates whether this instance has exactly one partition. */
  public boolean hasOne() {
    return partitions != null && partitions.size() == 1;
  }

  /** Indicates whether this instance has more than one partition. */
  public boolean hasMultiple() {
    return partitions != null && partitions.size() > 1;
  }

  /** Returns a partition. */
  public Integer getAny() {
    return hasAny() ? partitions.iterator().next() : null;
  }

  // -------------------------------------------------------------------------
  // toString, hashCode, equals
  // -------------------------------------------------------------------------

  @Override
  public String toString() {
    return partitions.toString();
  }

  @Override
  public int hashCode() {
    return partitions.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (getClass() != object.getClass()) {
      return false;
    }

    Partitions other = (Partitions) object;

    return partitions.equals(other.partitions);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public Set<Integer> getPartitions() {
    return partitions;
  }

  public void setPartitions(Set<Integer> partitions) {
    this.partitions = partitions;
  }
}

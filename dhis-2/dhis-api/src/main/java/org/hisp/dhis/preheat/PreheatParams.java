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
package org.hisp.dhis.preheat;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PreheatParams {
  /** User to use for database queries. */
  private UserDetails userDetails;

  /** Mode to use for preheating. */
  private PreheatMode preheatMode = PreheatMode.REFERENCE;

  /** Identifiers to match on. */
  private PreheatIdentifier preheatIdentifier = PreheatIdentifier.UID;

  /** If preheat mode is ALL, only do full preheating on these classes. */
  private Set<Class<? extends IdentifiableObject>> classes = new HashSet<>();

  /** Objects to scan (if preheat mode is REFERENCE). */
  private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
      new HashMap<>();

  public PreheatParams() {}

  public UserDetails getUserDetails() {
    return userDetails;
  }

  public void setUserDetails(UserDetails userDetails) {
    this.userDetails = userDetails;
  }

  public PreheatMode getPreheatMode() {
    return preheatMode;
  }

  public PreheatParams setPreheatMode(PreheatMode preheatMode) {
    this.preheatMode = preheatMode;
    return this;
  }

  public PreheatIdentifier getPreheatIdentifier() {
    return preheatIdentifier;
  }

  public PreheatParams setPreheatIdentifier(PreheatIdentifier preheatIdentifier) {
    this.preheatIdentifier = preheatIdentifier;
    return this;
  }

  public Set<Class<? extends IdentifiableObject>> getClasses() {
    return classes;
  }

  public PreheatParams setClasses(Set<Class<? extends IdentifiableObject>> classes) {
    this.classes = classes;
    return this;
  }

  public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects() {
    return objects;
  }

  public void setObjects(
      Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects) {
    this.objects = objects;
  }

  @SuppressWarnings("unchecked")
  public PreheatParams addObject(IdentifiableObject object) {
    if (object == null) {
      return this;
    }

    objects
        .computeIfAbsent(HibernateProxyUtils.getRealClass(object), k -> new ArrayList<>())
        .add(object);

    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("user", userDetails)
        .add("preheatMode", preheatMode)
        .add("preheatIdentifier", preheatIdentifier)
        .add("classes", classes)
        .add("objects", objects)
        .toString();
  }
}

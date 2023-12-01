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
package org.hisp.dhis.interpretation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

public final class MentionUtils {
  public static List<Mention> convertUsersToMentions(Set<User> users) {
    List<Mention> mentions = new ArrayList<Mention>();
    for (User user : users) {
      Mention mention = new Mention();
      mention.setCreated(new Date());
      mention.setUsername(user.getUsername());
      mentions.add(mention);
    }
    return mentions;
  }

  public static Set<User> getMentionedUsers(String text, UserService userService) {
    Set<User> users = new HashSet<>();
    Matcher matcher = Pattern.compile("(?:\\s|^)@([\\w+._-]+)").matcher(text);
    while (matcher.find()) {
      String username = matcher.group(1);
      User user = userService.getUserByUsername(username);
      if (user != null) {
        users.add(user);
      }
    }
    return users;
  }

  public static List<String> removeCustomFilters(List<String> filters) {
    List<String> mentions = new ArrayList<String>();
    ListIterator<String> filterIterator = filters.listIterator();
    while (filterIterator.hasNext()) {
      String[] filterSplit = filterIterator.next().split(":");
      if (filterSplit[1].equals("in") && filterSplit[0].equals("mentions")) {
        mentions.add(filterSplit[2]);
        filterIterator.remove();
      }
    }
    return mentions;
  }
}

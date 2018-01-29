package org.hisp.dhis.interpretation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.user.User;

public class MentionUtils
{
    public static List<Mention> convertUsersToMentions(Set<User> users)
    {
        List<Mention> mentions = new ArrayList<Mention>();
        for ( User user : users )
        {
            Mention mention = new Mention();
            mention.setCreated( new Date() );
            mention.setUsername( user.getUsername() );
            mentions.add( mention );
        }
        return (mentions.size() > 0) ? mentions : null;
    }

}

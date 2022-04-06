package org.hisp.dhis.utils;

import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;

import com.google.common.collect.Lists;

public class UserTestUtils
{
    protected static final String BASE_USER_UID = "userabcdef";

    public static User makeUser( String uniqueCharacter )
    {
        return makeUser( uniqueCharacter, Lists.newArrayList() );
    }

    public static User makeUser( String uniqueCharacter, List<String> auths )
    {
        User user = new User();
        user.setUid( BASE_USER_UID + uniqueCharacter );

        user.setCreatedBy( user );

        user.setUsername( ("username" + uniqueCharacter).toLowerCase() );
        user.setPassword( "password" + uniqueCharacter );

        if ( auths != null && !auths.isEmpty() )
        {
            UserRole role = new UserRole();
            role.setName( "Role_" + CodeGenerator.generateCode( 5 ) );
            auths.forEach( auth -> role.getAuthorities().add( auth ) );
            user.getUserRoles().add( role );
        }

        user.setFirstName( "FirstName" + uniqueCharacter );
        user.setSurname( "Surname" + uniqueCharacter );
        user.setEmail( ("Email" + uniqueCharacter).toLowerCase() );
        user.setPhoneNumber( "PhoneNumber" + uniqueCharacter );
        user.setCode( "UserCode" + uniqueCharacter );
        user.setAutoFields();

        return user;
    }



}

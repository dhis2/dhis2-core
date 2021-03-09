package org.hisp.dhis.helpers.config;



/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@org.aeonbits.owner.Config.LoadPolicy( org.aeonbits.owner.Config.LoadType.MERGE )
@Config.Sources( { "system:properties", "system:env", "classpath:config.properties" } )
public interface Config
    extends org.aeonbits.owner.Config
{
    @Key( "instance.url" )
    String baseUrl();

    @Key( "user.super.password" )
    String superUserPassword();

    @Key( "user.super.username" )
    String superUserUsername();

    @Key( "user.default.username" )
    String defaultUserUsername();

    @Key( "user.default.password" )
    String defaultUSerPassword();

    @Key( "user.admin.username" )
    String adminUserUsername();

    @Key( "user.admin.password" )
    String adminUserPassword();
}

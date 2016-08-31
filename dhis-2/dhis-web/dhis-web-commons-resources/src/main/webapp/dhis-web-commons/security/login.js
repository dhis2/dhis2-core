
var login = {};
login.localeKey = "dhis2.locale.ui";

$( document ).ready( function() 
{
    $( '#j_username' ).focus();

    $( '#loginForm').bind( 'submit', function() 
    {
        $( '#submit' ).attr( 'disabled', 'disabled' );

        sessionStorage.removeItem( 'ouSelected' );
        sessionStorage.removeItem( 'USER_PROFILE' );
        sessionStorage.removeItem( 'USER_ROLES' );
    } );
    
    var locale = localStorage[login.localeKey];
    
    if ( undefined !== locale && locale )
    {
    	login.changeLocale( locale );
    	$( '#localeSelect option[value="' + locale + '"]' ).attr( 'selected', 'selected' );
    }
} );

login.localeChanged = function()
{
	var locale = $( '#localeSelect :selected' ).val();
	
	if ( locale )
	{
		login.changeLocale( locale );
		localStorage[login.localeKey] = locale;
	}
}

login.changeLocale = function( locale )
{	
	$.get( 'loginStrings.action?keyApplication=Y&loc=' + locale, function( json ) {
		$( '#createAccountButton' ).html( json.create_an_account );
		$( '#signInLabel' ).html( json.sign_in );
		$( '#j_username' ).attr( 'placeholder', json.login_username );
		$( '#j_password' ).attr( 'placeholder', json.login_password );
		$( '#forgotPasswordLink' ).html( json.forgot_password );
		$( '#createAccountLink' ).html( json.create_an_account );
		$( '#loginMessage' ).html( json.wrong_username_or_password );
		$( '#poweredByLabel' ).html( json.powered_by );
		$( '#submit' ).val( json.sign_in );
		
		$( '#titleArea' ).html( json.applicationTitle );		
		$( '#introArea' ).html( json.keyApplicationIntro );
		$( '#notificationArea' ).html( json.keyApplicationNotification );
		$( '#applicationFooter' ).html( json.keyApplicationFooter );	
	} );	
}


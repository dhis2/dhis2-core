
var login = {};
login.localeKey = "dhis2.locale.ui";

$( document ).ready( function() 
{
    $( '#j_username' ).focus();

    var checked = document.getElementById( '2fa' ).checked;

    $( '#2fa' ).click( function () {
        $( '#2fa_code' ).attr("hidden", checked);
        $( '#2fa_code' ).attr("readonly", checked);
        document.getElementById( '2fa' ).checked = !checked;

        checked = !checked;
    });

    $( '#loginForm').bind( 'submit', function() 
    {
		if ( window.location.hash )
		{	
			$(this).prop('action', $(this).prop('action') + window.location.hash );
		}
		
        $( '#submit' ).attr( 'disabled', 'disabled' );

        sessionStorage.removeItem( 'ouSelected' );
        sessionStorage.removeItem( 'USER_PROFILE' );
        sessionStorage.removeItem( 'USER_SETTING' );
		sessionStorage.removeItem( 'eventCaptureGridColumns');
		sessionStorage.removeItem( 'trackerCaptureGridColumns');
		sessionStorage.removeItem( 'trackerCaptureCategoryOptions');
		sessionStorage.removeItem( 'eventCaptureCategoryOptions');
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
		$( '#2fa_code' ).attr( 'placeholder', json.login_2fa_code );
		$( '#2FaLabel' ).html( json.login_using_two_factor_authentication );
		$( '#forgotPasswordLink' ).html( json.forgot_password );
		$( '#createAccountLink' ).html( json.create_an_account );

		var loginMsg = $( '#lockedMessage' ).html();

        if ( undefined !== loginMsg && loginMsg.indexOf( "locked" ) != -1 )
        {
            loginMsg = json.account_locked;
        }
        else
        {
            loginMsg = json.wrong_username_or_password;
        }

		$( '#loginMessage' ).html( loginMsg );

		$( '#poweredByLabel' ).html( json.powered_by );
		$( '#submit' ).val( json.sign_in );
		
		$( '#titleArea' ).html( json.applicationTitle );		
		$( '#introArea' ).html( json.keyApplicationIntro );
		$( '#notificationArea' ).html( json.keyApplicationNotification );
		$( '#applicationFooter' ).html( json.keyApplicationFooter );
		$( '#applicationRightFooter' ).html( json.keyApplicationRightFooter );
	} );
}


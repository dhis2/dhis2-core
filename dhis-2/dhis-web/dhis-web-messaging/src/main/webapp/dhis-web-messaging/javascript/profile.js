
function showProfileIntro()
{
	$( ".userRow" ).each( function( index, element ) 
	{
		var fromTop = $( element ).position().top;
		
		if ( fromTop > 420 )
		{
			$( element ).hide();
		}
	} );
}

function showFullProfile()
{
	$( ".userRow" ).show();
	$( "#userInfoLink" ).hide();
}
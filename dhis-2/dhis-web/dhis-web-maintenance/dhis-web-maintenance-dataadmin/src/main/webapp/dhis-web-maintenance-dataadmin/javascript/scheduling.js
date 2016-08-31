
$( document ).ready( function() 
{
	if ( $( '#isRunning' ).val() == 'true' )
	{
		$( '.scheduling' ).attr( 'disabled', 'disabled' );
	}
	else
	{
		$( '.scheduling' ).removeAttr( 'disabled' );
	}
} );

function submitSchedulingForm()
{
	$( '.scheduling' ).removeAttr( 'disabled' );
	$( '#schedulingForm' ).submit();
}

function toggleMoreOptions()
{
	$( "#moreOptionsLink" ).toggle();
	$( "#moreOptionsDiv" ).toggle();
}

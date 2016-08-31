
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
	
	if ( $( '#dataMartStrategy' ).val() == 'disabled' )
	{
		$( '.dataMart' ).attr( 'disabled', 'disabled' );
	}
} );

function submitSchedulingForm()
{
	$( '.scheduling' ).removeAttr( 'disabled' );
	$( '#schedulingForm' ).submit();
}

function toggleDataMart()
{
	if ( $( '#dataMartStrategy' ).val() == 'never' )
	{
		$( '.dataMart' ).attr( 'disabled', 'disabled' );
	}
	else
	{
		$( '.dataMart' ).removeAttr( 'disabled' );
	}
}

function toggleMoreOptions()
{
	$( "#moreOptionsLink" ).toggle();
	$( "#moreOptionsDiv" ).toggle();
}
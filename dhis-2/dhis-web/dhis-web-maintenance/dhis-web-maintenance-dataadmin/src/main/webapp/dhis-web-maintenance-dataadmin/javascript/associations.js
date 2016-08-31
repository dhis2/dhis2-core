
var numberOfSelects = 0;

function selectAllAtLevel( dataSetId )
{
	var list = document.getElementById( 'levelList' );
    
    var level = list.options[ list.selectedIndex ].value;
    
    window.location.href = 'selectLevel.action?level=' + level + '&dataSetId=' + dataSetId;
}

function unselectAllAtLevel( dataSetId )
{
	var list = document.getElementById( 'levelList' );
    
    var level = list.options[ list.selectedIndex ].value;
    
    window.location.href = 'unselectLevel.action?level=' + level + '&dataSetId=' + dataSetId;
}

function treeClicked()
{
  
	if( ( parent.document.getElementById( "button5" ).disabled == true )
			|| ( parent.document.getElementById( "button6" ).disabled == true )
			|| ( parent.document.getElementById( "button7" ).disabled == true )
			|| (parent.document.getElementById( "button8" ).disabled == true )){
				alert( "Please Wait..... !" ); 
				return true;
			}
   else{ 	
    		numberOfSelects++;   
    		setMessage( i18n_loading );
    		parent.document.getElementById( "button10" ).disabled = true;
    	}
}

function selectCompleted( selectedUnits )
{
    numberOfSelects--;
    
    if ( numberOfSelects <= 0 )
    {
       hideMessage();
        
       parent.document.getElementById( "button10" ).disabled = false;
    }
}

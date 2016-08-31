var selectedList;

function addToSelectedList( srcListId )
{

    selectedList = document.getElementById( 'selectedList' );
    srcList = document.getElementById( srcListId );

    option = srcList.options.item( srcList.selectedIndex );
    selectedList.add( option, null );
}

function submitForm()
{

    if ( !selectedList )
    {
        selectedList = document.getElementById( 'selectedList' );
    }

    // selectAll(availableList);
    selectAll( selectedList );

    return false;
}

function selectAll( list )
{
    for ( var i = 0, option; option = list.options.item( i ); i++ )
    {
        option.selected = true;
    }
}

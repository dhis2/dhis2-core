jQuery(document).ready(function()
{
    validation2('lockingForm', function( form )
    {
        validateCollectiveDataLockingForm(form);
    },
    {
        'beforeValidateHandler' : function() {
            $("#selectedPeriods option").each(function() { $(this).attr("selected", "true"); });
            $("#selectedDataSets option").each(function() { $(this).attr("selected", "true"); });
        },
        'rules' : getValidationRules("dataLocking")
    });
});

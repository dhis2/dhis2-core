function changeRuleType() {
  var ruleType = $('#ruleType').val();

  if( ruleType == 'VALIDATION' ) {
    hideById('organisationUnitLevelTR');
    hideById('sequentialSampleCountTR');
    hideById('annualSampleCountTR');
    hideById('sequentialSkipCountTR');

    showById('compulsory_pair');
    showById('exclusive_pair');
  }
  else {
    showById('organisationUnitLevelTR');
    showById('sequentialSampleCountTR');
    showById('annualSampleCountTR');
    showById('sequentialSkipCountTR');

    var op = document.getElementById('operator');
    if( 'compulsory_pair' == op.value || 'exclusive_pair' == op.value ) {
      showById('select_operator');
      op.selectedIndex = 0;
    }
    hideById('compulsory_pair');
    hideById('exclusive_pair');
  }
}

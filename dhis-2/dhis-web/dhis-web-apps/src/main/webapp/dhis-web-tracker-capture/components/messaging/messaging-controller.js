/* global trackerCapture, angular */

trackerCapture.controller('MessagingController',
        function($scope,
                MessagingService,
                CurrentSelection,
                DialogService) {
    $scope.dashboardReady = false;
    
    //$scope.smsForm = {};
    $scope.note = {};
    $scope.message = {};
    $scope.showMessagingDiv = false;
    $scope.showNotesDiv = true;
    
    $scope.$on('dashboardWidgets', function() {
        $scope.selectedEnrollment = null;
        var selections = CurrentSelection.get();
        $scope.selectedTei = selections.tei;
        $scope.dashboardReady = true;
        
        if($scope.selectedTei){
            //check if the selected TEI has any of the contact attributes
            //that can be used for messaging
            var continueLoop = true;
            for(var i=0; i<$scope.selectedTei.attributes.length && continueLoop; i++){
                if( $scope.selectedTei.attributes[i].valueType === 'PHONE_NUMBER' /*|| $scope.selectedTei.attributes[i].valueType === 'EMAIL'*/ ){
                    $scope.message.phoneNumber = $scope.selectedTei.attributes[i].value;
                    continueLoop = false;
                }
            }
        }
    });
    
    $scope.sendSms = function(){
        //check for form validity
        $scope.smsForm.submitted = true;        
        if( $scope.smsForm.$invalid ){
            return false;
        } 
        
        //form is valid...        
        var smsMessage = {message: $scope.message.value, recipients: [$scope.message.phoneNumber]};        
        MessagingService.sendSmsMessage(smsMessage).then(function(response){
            var dialogOptions = {
                headerText: response.status,
                bodyText: response.message
            };                

            DialogService.showDialog({}, dialogOptions);
            $scope.clear();
        });
        
    };
    
    $scope.clear = function(){
        $scope.smsForm.submitted = false;
        $scope.message = {};
    };
    
    $scope.showMessaging = function(){        
        $scope.showMessagingDiv = !$scope.showMessagingDiv;
    };
});

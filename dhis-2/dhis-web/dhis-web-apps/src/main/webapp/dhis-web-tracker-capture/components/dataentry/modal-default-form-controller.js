trackerCapture.controller('ModalDefaultFormController', function($scope){
    
    var defaultRequestError = "Server error. Please try again later.";
    
    $scope.completeIncompleteEventModal = function(){  
           
        $scope.requestError = "";
        if ($scope.currentEvent.status === 'COMPLETED'){
            var dhis2Event = $scope.makeDhis2EventToUpdate();
            dhis2Event.status = 'ACTIVE';
        }
        else{
            $scope.modalForm.$setSubmitted();
            $scope.modalForm.outerForm.$setSubmitted();     
            
            if($scope.modalForm.$invalid){
                return;
            }     
            
            //check for errors!
            if(angular.isDefined($scope.errorMessages[$scope.currentEvent.event]) && $scope.errorMessages[$scope.currentEvent.event].length > 0) {
                //There is unresolved program rule errors - show error message.
                return;
            }
            
            var dhis2Event = $scope.makeDhis2EventToUpdate();
            dhis2Event.status = 'COMPLETED';
        }        
        
        $scope.executeCompleteIncompleteEvent(dhis2Event).then(function(){                                    
            if(dhis2Event.status === 'COMPLETED'){
                $scope.eventEditFormModalInstance.close();            
            }
        }, function(error){
            $scope.requestError = defaultRequestError;                                   
        });
    };
    
    $scope.deleteEventModal = function(){
        
        $scope.executeDeleteEvent().then(function(){
            
            $scope.eventEditFormModalInstance.close();
        }, function(){
            
            $scope.requestError = defaultRequestError; 
        });        
    };
    
    $scope.skipUnskipEventModal = function(){
                
        var dhis2Event = $scope.makeDhis2EventToUpdate();
        
        if ($scope.currentEvent.status === 'SKIPPED') {//unskip event
            dhis2Event.status = 'ACTIVE';
        }
        else {//skip event
            dhis2Event.status = 'SKIPPED';
        }
        
        
        $scope.executeSkipUnskipEvent(dhis2Event).then(function(){
            if(dhis2Event.status === 'SKIPPED'){
                $scope.eventEditFormModalInstance.close();
            }
        }, function(){
            $scope.requestError = defaultRequestError;
        });        
    };
    
    $scope.closeEventModal = function(){
        $scope.eventEditFormModalInstance.dismiss();
    };
});
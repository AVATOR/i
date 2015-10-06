'use strict';
angular.module('dashboardJsApp').directive('printDialog', [
  'PrintTemplateService',
  function(PrintTemplateService) {
    return {
      restrict: 'E',
      scope: false,
      templateUrl: 'app/tasks/form-buttons/printDialog.html',
      link: function($scope, $elem, attrs) {
        //console.log('printDialog $scope, attrs', $scope, attrs);
        $scope.$watch('printModalState.show', function(newval, oldval) {
          if (oldval != newval) {
            if (newval == true) {
              // load form from api or use default as a fallback
              if (!$scope.model.printTemplate) {
                $scope.processedPrintTemplate = '';
                // if no printTemplate selected - use default print form
                return;
              }
              $scope.processedPrintTemplate = 'Завантаження форми. Зачекайте, будь ласка.';
              var templatePromise = PrintTemplateService.getPrintTemplate($scope.selectedTask, $scope.taskForm, $scope.model.printTemplate.id);
              templatePromise.then(function(template){
                $scope.processedPrintTemplate = template;
              }, function(error) {
                $scope.processedPrintTemplate = 'При завантаженні форми сталася помилка';
              });
            }
          }
        });
      }
    };
  }
]);
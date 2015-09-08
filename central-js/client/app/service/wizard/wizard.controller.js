angular.module('app').controller('WizardController', function(
  $state, AdminService, $rootScope, $scope, $location, $sce, RegionListFactory, LocalityListFactory, PlacesService, ServiceService, service, regions) {

  // Each controller which uses Places Control should tell it:

  // FIXME: preload regions and service and provide them as part of the locations service
  console.log('WizardController. Regions: ', regions );

  PlacesService.setController({
    controller: this,
    regions: regions,
    service: service
  });

  /*
  angular.module('app').controller('ServiceBuiltInController', function($location, $state, $rootScope, $scope) {
    $scope.$location = $location;
    $scope.$state = $state;
  });
  */

});

/*
angular.module('app').controller('ServiceRegionController', function($state, $rootScope, $scope, $sce, RegionListFactory, PlacesService, ServiceService, service, AdminService) {
  !!!
  $scope.onSelectRegionList = function($item, $model, $label) {
    $scope.data.region = $item;
    $scope.regionList.select($item, $model, $label);
  };
});
*/
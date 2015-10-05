/**
 Place.js - компонент для вибору місця - області та міста.
 Використовує сервіс PlacesService.

 TODO: для кращого юзабіліті, розглянути можливість показу локацій, у яких сервіс доступний, у верхній частині списку
 
 Див.: https://github.com/e-government-ua/i/issues/550
 */
angular.module('app')
  .directive('place', function($rootScope, $location, $state, $sce, AdminService, RegionListFactory, LocalityListFactory, PlacesService, ServiceService, serviceLocationParser) {

    return {
      restrict: 'E',
      templateUrl: 'app/common/components/form/directives/place/place.html',
      link: function($scope, element, attrs) {

        $scope.getPlaceControlClass = function() {
          return PlacesService.getClassByState($state);
        };

        $scope.recallPlaceData = function() {
          var placeData = PlacesService.getPlaceData();
          // console.log('recall place data: ', placeData.region, placeData.city.sName);

          var regions = PlacesService.getRegionsForService(ServiceService.oService);
          var initialRegion = serviceLocationParser.getSelectedRegion(regions);

          if ($scope.regionList) {
            $scope.regionList.select(initialRegion || placeData.region);
          }
          if ($scope.localityList) {
            $scope.localityList.select(placeData.city);
          }
          if (initialRegion) {
            // TODO debug it
            // console.log('initialRegion: ', initialRegion);
            $scope.onSelectRegionList(initialRegion);
          }
        };

        $scope.resetPlaceData = function() {
          PlacesService.setPlaceData({
            region: null,
            city: null
          });
        };

        $scope.serviceAvailableIn = function() {
          return PlacesService.serviceAvailableIn();
        };

        $scope.cityIsChosen = function() {
          return PlacesService.cityIsChosen();
        };

        $scope.regionIsChosen = function() {
          return PlacesService.regionIsChosen();
        };

        // TODO improve the logic
        $scope.authControlIsVisible = function() {
          var sa = PlacesService.serviceAvailableIn();
          return (sa.thisRegion || sa.thisCity) && $scope.placeControlIsComplete();
        };

        // TODO do the logic
        $scope.authControlIsComplete = function() {
          var bComplete = false;
          return bComplete;
        };

        $scope.placeControlIsNeeded = function() {
          var bNeeded = false;
          var sa = PlacesService.serviceAvailableIn();

          // needed because service is available for some place
          if (sa.someRegion || sa.someCity) {
            bNeeded = true;
          }

          return bNeeded;
        };

        $scope.placeControlIsVisible = function() {
          var bResult = true;
          bResult = $scope.placeControlIsNeeded();
          return bResult;
        };

        $scope.placeControlIsDisabled = function() {
          var bIsDisabled = false;
          bIsDisabled = $scope.placeControlIsComplete();
          return bIsDisabled;
        };

        /**
         * Ця функція визначає, чи заповнені всі поля, які необхідно заповнити
         */
        $scope.placeControlIsComplete = function() {
          var bIsComplete = false;
          var sa = PlacesService.serviceAvailableIn();
          var regionIsChosen = $scope.regionIsChosen();
          var cityIsChosen = $scope.cityIsChosen();

          //
          // вибір вважається зробленим, якщо:
          // 
          // сервіс недоступний ні в областях, ні в містах, отже вибирати місце не треба:
          if (!sa.someRegion && !sa.someCity) {
            bIsComplete = true;
          }
          // сервіс доступний у вибраній області і недоступний у містах даної області:
          // був баг: при виборі області можна було вибрати ще й місто, хоча область була кінцевою точкою (issues/540)
          if (sa.thisRegion && !sa.someCityInThisRegion) {
            bIsComplete = true;
          }
          // сервіс доступний у вибраному місті:
          if (sa.thisCity) {
            bIsComplete = true;
          }
          // сервіс недоступний у вибраній області, але доступний у якійсь іншій (але не в містах):
          if (regionIsChosen && !sa.thisRegion && sa.someRegion && !sa.someCity) {
            bIsComplete = true;
          }
          // сервіс недоступний у вибраних області та місті, але доступний у якомусь місті:
          // Приклад: /service/159/general, Дніпропетровська > Апостолове, має бути "bIsComplete"
          if (regionIsChosen && cityIsChosen && sa.someCity ) {
           bIsComplete = true; 
          }

          // для тестування:
          // service/17/general
          // наявність обласної послуги не має блокувати можливість розміщення там же і міської послуги (issues/443)
          // Правильно: вибрати "Миколіївську", а потім мати можливість вибрати "Миколаїв"

          // TODO: передбачити можливість вибору тільки міста, якщо його назва є унікальною — тобто воно належить тільки одній області, і її можна взнати автоматично.

          return bIsComplete;
        };

        $scope.editPlace = function() {
          $scope.resetPlaceData();

          var regions = PlacesService.getRegionsForService(ServiceService.oService);

          $scope.regionList.reset();
          $scope.regionList.select(null);
          $scope.regionList.initialize(regions);

          $scope.localityList.reset();
          $scope.localityList.select(null);

          $scope.setStepNumber(1);

          $scope.$emit('onPlaceEdit');
        };

        $scope.setStepNumber = function(nStep) {
          $scope.stepNumber = nStep;
        };

        $scope.getStepNumber = function() {
          return $scope.stepNumber;
        };


        $scope.loadRegionList = function(search) {
          return $scope.regionList.load(ServiceService.oService, search);
        };

        $scope.loadLocalityList = function(search) {
          return $scope.localityList.load(ServiceService.oService, PlacesService.getPlaceData().region.nID, search);
        };

        $scope.processPlaceSelection = function() {
          var placeData = PlacesService.getPlaceData();

          // console.log('Process Place selection.');
          // console.log('1. Region is chosen:', $scope.regionIsChosen(), ', city is chosen:', $scope.cityIsChosen());
          // console.log('2. Place controls is complete:', $scope.placeControlIsComplete());
          // console.log('3. Auth control is visible:', $scope.authControlIsVisible());
          // console.log('4. Service Availability:', JSON.stringify(PlacesService.serviceAvailableIn(), null, ''));

          PlacesService.setPlaceData(placeData);

          $scope.region = PlacesService.getPlaceData().region;
          $scope.city = PlacesService.getPlaceData().city;

          $scope.setStepNumber(2);

          $scope.$emit('onPlaceChange');
        };

        $scope.initPlaceControls = function() {

          var regions = $scope.regions;
          var placeData = PlacesService.getPlaceData();

          $scope.stepNumber = 1;

          // ініціюємо дані зі scope, якщо вони там є:
          $scope.region = $scope.data && $scope.data.region || placeData.region || $scope.region;
          $scope.city = $scope.data && $scope.data.city || placeData.city || $scope.city;

          PlacesService.setPlaceData({
            region: $scope.region,
            city: $scope.city
          });

          $scope.regionList = $scope.regionList || new RegionListFactory();
          $scope.localityList = $scope.localityList || new LocalityListFactory();

          $scope.regionList.initialize($scope.regions);

          var bIsComplete = $scope.placeControlIsComplete();

          console.log('initPlaceControls $scope.regions.length = ', $scope.regions.length, '$scope.region:', $scope.region, '$scope.city:', $scope.city, ' $scope.data:', $scope.data, 'bIsComplete:', bIsComplete);

          $scope.recallPlaceData();

          // Якщо форма вже заповнена після відновлення даних з localStorage, то перейти до наступного кроку:
          if (bIsComplete) {
            $scope.processPlaceSelection();
          }
        };

        $scope.onSelectRegionList = function($item, $model, $label) {

          // console.info('onSelectRegionList, Service Availability:', JSON.stringify(PlacesService.serviceAvailableIn(), null, ''), $item);

          PlacesService.setRegion($item);
          $scope.regionList.select($item, $model, $label);

          $scope.loadLocalityList(null);
          PlacesService.setCity(null);
          $scope.localityList.reset();

          $scope.localityList.load(null, $item.nID, null).then(function(cities) {
            $scope.localityList.typeahead.defaultList = cities;
          });

          var serviceType = PlacesService.findServiceDataByRegion();

          // Сервіс недоступний у області — значить, варто завантажити міста, інакше вважати місце вибраним:
          if (serviceType !== 1 && serviceType !== 4) {
            $scope.localityList.load(ServiceService.oService, $item.nID, null).then(function(cities) {
              $scope.localityList.typeahead.defaultList = cities;
              var initialCity = serviceLocationParser.getSelectedCity(cities);
              if (initialCity) {
                $scope.onSelectLocalityList(initialCity);
              }
            });
          }

          if ($scope.placeControlIsComplete()) {
            $scope.processPlaceSelection();
          }
        };

        $scope.showCityDropdown = function() {
          var sa = $scope.serviceAvailableIn();
          return $scope.regionIsChosen() && (sa.someCityInThisRegion || !sa.thisRegion && sa.someCity);
        };

        $scope.onSelectLocalityList = function($item, $model, $label) {
          PlacesService.setCity($item);
          $scope.localityList.select($item, $model, $label);
          $scope.processPlaceSelection();
        };

        $scope.initPlaceControls();
      }
    };
  });
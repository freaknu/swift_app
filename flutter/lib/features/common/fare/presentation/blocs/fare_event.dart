import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';

abstract class FareEvent {
  const FareEvent();
}

class EstimateFare extends FareEvent {
  final FareEstimateRequestModel request;

  const EstimateFare({required this.request});
}

class ClearFare extends FareEvent {
  const ClearFare();
}

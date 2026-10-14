import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';
import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_event.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:swift_app/features/rider/home/data/model/offer_response_model.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/repository/offer_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/offer_usecase.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_request_model.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/domain/entity/route.dart' as ride;

void main() {
  test(
    'slower old promotion quote cannot overwrite the latest quote',
    () async {
      final repository = _FareRepository();
      final bloc = FareBloc(repository: repository);
      addTearDown(bloc.close);
      bloc.add(EstimateFare(request: _request(offerId: 10)));
      await _tick();
      bloc.add(EstimateFare(request: _request(offerId: 20)));
      await _tick();
      repository.pending[1].complete(_success(_estimate(20)));
      await _tick();
      repository.pending[0].complete(_success(_estimate(10)));
      await _tick();

      expect(bloc.state.isQuoteFor(_request(offerId: 20)), isTrue);
      expect(bloc.state.isQuoteFor(_request(offerId: 10)), isFalse);
      expect(bloc.state.fareEstimate!.fare.discount, 20);
    },
  );

  test('removing promotion cannot restore an older discounted fare', () async {
    final repository = _FareRepository();
    final bloc = FareBloc(repository: repository);
    addTearDown(bloc.close);
    bloc.add(EstimateFare(request: _request(offerId: 10)));
    await _tick();
    bloc.add(EstimateFare(request: _request()));
    await _tick();
    repository.pending[1].complete(_success(_estimate(0)));
    await _tick();
    repository.pending[0].complete(_success(_estimate(10)));
    await _tick();

    expect(bloc.state.isQuoteFor(_request()), isTrue);
    expect(bloc.state.fareEstimate!.fare.totalFare, 100);
    expect(bloc.state.fareEstimate!.fare.discount, 0);
  });

  test('clearing a quote invalidates pending responses', () async {
    final repository = _FareRepository();
    final bloc = FareBloc(repository: repository);
    addTearDown(bloc.close);
    bloc.add(EstimateFare(request: _request(offerId: 10)));
    await _tick();
    bloc.add(const ClearFare());
    await _tick();
    repository.pending.single.complete(_success(_estimate(10)));
    await _tick();

    expect(bloc.state.status, FareStatus.initial);
    expect(bloc.state.fareEstimate, isNull);
    expect(bloc.state.lastRequest, isNull);
  });

  test(
    'quote failure clears stale totals and retry clears old error',
    () async {
      final repository = _FareRepository();
      final bloc = FareBloc(repository: repository);
      addTearDown(bloc.close);
      bloc.add(EstimateFare(request: _request()));
      await _tick();
      repository.pending.single.complete(_success(_estimate(0)));
      await _tick();
      bloc.add(EstimateFare(request: _request(offerId: 10)));
      await _tick();
      expect(bloc.state.fareEstimate, isNull);
      repository.pending.last.complete(
        ApiResponseFormat.failure(
          statusCode: 400,
          message: 'Minimum fare not met',
          endpoint: 'fare/estimate',
        ),
      );
      await _tick();
      expect(bloc.state.status, FareStatus.failure);
      expect(bloc.state.message, 'Minimum fare not met');
      expect(bloc.state.isQuoteFor(_request(offerId: 10)), isFalse);
      bloc.add(EstimateFare(request: _request()));
      await _tick();
      expect(bloc.state.message, isNull);
      repository.pending.last.complete(_success(_estimate(0)));
      await _tick();
    },
  );

  test('an unexpected quote exception becomes a retryable failure', () async {
    final repository = _FareRepository();
    final bloc = FareBloc(repository: repository);
    addTearDown(bloc.close);
    bloc.add(EstimateFare(request: _request()));
    await _tick();
    repository.pending.single.completeError(StateError('offline'));
    await _tick();
    expect(bloc.state.status, FareStatus.failure);
    expect(bloc.state.fareEstimate, isNull);
  });

  test('a quote is valid only for its exact route, vehicle and promotion', () {
    final state = FareState(
      status: FareStatus.success,
      fareEstimate: _estimate(10),
      lastRequest: _request(offerId: 10),
    );
    expect(state.isQuoteFor(_request(offerId: 10)), isTrue);
    expect(state.isQuoteFor(_request()), isFalse);
    expect(
      state.isQuoteFor(_request(offerId: 10, type: 'AUTO_RICKSHAW')),
      isFalse,
    );
    expect(state.isQuoteFor(_request(offerId: 10, latitude: 13)), isFalse);
  });

  test(
    'slower previous vehicle offers cannot replace the selected vehicle',
    () async {
      final repository = _OfferRepository();
      final bloc = OfferBloc(
        offerUsecase: OfferUsecase(offerRepository: repository),
      );
      addTearDown(bloc.close);
      bloc.add(FetchOffers(request: OfferRequest(rideTypeCode: 'BIKE')));
      await _tick();
      bloc.add(
        FetchOffers(request: OfferRequest(rideTypeCode: 'AUTO_RICKSHAW')),
      );
      await _tick();
      repository.pending[1].complete(_success([OfferResponse(id: 2)]));
      await _tick();
      repository.pending[0].complete(_success([OfferResponse(id: 1)]));
      await _tick();

      expect(bloc.state.lastRequest!.rideTypeCode, 'AUTO_RICKSHAW');
      expect(bloc.state.offers.single.id, 2);
    },
  );

  test(
    'old vehicle offer failures cannot hide new successful offers',
    () async {
      final repository = _OfferRepository();
      final bloc = OfferBloc(
        offerUsecase: OfferUsecase(offerRepository: repository),
      );
      addTearDown(bloc.close);
      bloc.add(FetchOffers(request: OfferRequest(rideTypeCode: 'BIKE')));
      await _tick();
      bloc.add(FetchOffers(request: OfferRequest(rideTypeCode: 'HATCHBACK')));
      await _tick();
      repository.pending[1].complete(_success([OfferResponse(id: 2)]));
      await _tick();
      repository.pending[0].completeError(StateError('old request failed'));
      await _tick();
      expect(bloc.state.status, OfferStatus.success);
      expect(bloc.state.offers.single.id, 2);
    },
  );

  test('backend offer timestamps without offsets are interpreted as UTC', () {
    final offer = OfferResponseModel.fromJson({
      'validFrom': '2026-10-15T10:20:30.123456',
      'validUntil': '2026-11-15T10:20:30',
    });
    expect(offer.validFrom, DateTime.utc(2026, 10, 15, 10, 20, 30, 123, 456));
    expect(offer.validUntil, DateTime.utc(2026, 11, 15, 10, 20, 30));
    expect(offer.validFrom!.isUtc, isTrue);
  });

  test(
    'offer timestamp parser preserves explicit offsets and tolerates null',
    () {
      final offer = OfferResponseModel.fromJson({
        'validFrom': '2026-10-15T10:20:30+05:30',
        'validUntil': 'invalid',
      });
      expect(offer.validFrom, DateTime.utc(2026, 10, 15, 4, 50, 30));
      expect(offer.validUntil, isNull);
      expect(OfferResponseModel.fromJson({}).validFrom, isNull);
    },
  );

  test('selected offer uses the same identifier in quote and booking JSON', () {
    final quote = _request(offerId: 20).toJson();
    final booking = RideRequestModel.fromEntity(
      const RideRequest(
        riderId: 7,
        authUserId: 8,
        offerId: 20,
        rideType: 'BIKE',
        route: ride.Route(
          offerId: 20,
          rideType: 'BIKE',
          pickupLatitude: 12.97,
          pickupLongitude: 77.59,
          destinationLatitude: 12.98,
          destinationLongitude: 77.60,
        ),
      ),
    ).toJson();
    expect(booking['offerId'], quote['offerId']);
    expect((booking['route'] as Map)['offerId'], quote['offerId']);
  });
}

Future<void> _tick() => Future<void>.delayed(Duration.zero);

FareEstimateRequestModel _request({
  int? offerId,
  String type = 'BIKE',
  double latitude = 12.97,
}) => FareEstimateRequestModel(
  offerId: offerId,
  rideType: type,
  pickupLatitude: latitude,
  pickupLongitude: 77.59,
  destinationLatitude: 12.98,
  destinationLongitude: 77.60,
);

FareEstimateResponse _estimate(double discount) => FareEstimateResponse(
  estimatedDistanceKm: 2,
  estimatedDurationSeconds: 300,
  fare: Fare(
    baseFare: 30,
    distanceFare: 50,
    timeFare: 10,
    bookingFee: 5,
    tax: 5,
    surgeMultiplier: 1,
    discount: discount,
    totalFare: 100 - discount,
    currency: 'INR',
    paymentStatus: 'PENDING',
  ),
);

ApiResponseFormat<T> _success<T>(T value) => ApiResponseFormat.success(
  data: value,
  statusCode: 200,
  message: 'Success',
  endpoint: 'fixture',
);

class _FareRepository implements FareRepository {
  final pending = <Completer<ApiResponseFormat<FareEstimateResponse>>>[];

  @override
  Future<ApiResponseFormat<FareEstimateResponse>> estimateFare(
    FareEstimateRequestModel request,
  ) {
    final response = Completer<ApiResponseFormat<FareEstimateResponse>>();
    pending.add(response);
    return response.future;
  }
}

class _OfferRepository implements OfferRepository {
  final pending = <Completer<ApiResponseFormat<List<OfferResponse>>>>[];

  @override
  Future<ApiResponseFormat<List<OfferResponse>>> fetchOffers(
    OfferRequest request,
  ) {
    final response = Completer<ApiResponseFormat<List<OfferResponse>>>();
    pending.add(response);
    return response.future;
  }
}

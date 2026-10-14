import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_card_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_section_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/saved_address_card_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/saved_address_section_widget.dart';

void main() {
  testWidgets(
    'saved collection builds only visible cards and keeps selection',
    (tester) async {
      final addresses = List.generate(
        100,
        (i) => UserAddress(
          id: i,
          addressType: AddressTypeEnum.OTHER,
          label: 'Place $i',
          fullAddress: 'Address $i',
          landmark: '',
          city: 'Bengaluru',
          state: 'Karnataka',
          country: 'India',
          pinCode: '560001',
          latitude: 12.97,
          longitude: 77.59,
          defaultAddress: false,
        ),
      );
      final user = _UserBloc(
        UserState(
          status: UserStatus.success,
          user: UserReponse(
            id: 3,
            authUserId: 8,
            fullName: 'Rider',
            email: null,
            phoneNumber: '',
            profileImageUrl: null,
            active: true,
            deleted: false,
            allAddresses: addresses,
          ),
        ),
      );
      addTearDown(user.close);
      int? selected;
      await tester.pumpWidget(
        BlocProvider<UserBloc>.value(
          value: user,
          child: _viewport(
            SavedAddressSection(
              expanded: true,
              asSliver: true,
              onAddressSelected: (address) => selected = address.id,
            ),
          ),
        ),
      );
      expect(
        find.byType(SavedAddressCard).evaluate().length,
        inExclusiveRange(0, 20),
      );
      expect(find.text('Place 99'), findsNothing);
      await tester.tap(find.text('Place 0'));
      expect(selected, 0);
      await tester.drag(find.byType(CustomScrollView), const Offset(0, -700));
      await tester.pumpAndSettle();
      expect(find.byType(SavedAddressCard).evaluate().length, lessThan(20));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'offers collection stays lazy with large text and selectable cards',
    (tester) async {
      final offers = _OfferBloc(
        OfferState(
          status: OfferStatus.success,
          offers: List.generate(
            100,
            (i) => OfferResponse(
              id: i,
              title: 'Offer $i',
              offerCode: 'CODE$i',
              description: 'Save on your next journey.',
              discountType: 'PERCENTAGE',
              discountValue: 10,
              active: true,
            ),
          ),
        ),
      );
      addTearDown(offers.close);
      int? selected;
      await tester.pumpWidget(
        BlocProvider<OfferBloc>.value(
          value: offers,
          child: _viewport(
            OfferSection(
              expanded: true,
              asSliver: true,
              onOfferSelected: (offer) => selected = offer.id,
            ),
          ),
        ),
      );
      expect(find.byType(OfferCard).evaluate().length, inExclusiveRange(0, 10));
      expect(find.text('Offer 99'), findsNothing);
      await tester.tap(find.text('Offer 0'));
      expect(selected, 0);
      await tester.drag(find.byType(CustomScrollView), const Offset(0, -700));
      await tester.pumpAndSettle();
      expect(find.byType(OfferCard).evaluate().length, lessThan(10));
      expect(tester.takeException(), isNull);
    },
  );
}

Widget _viewport(Widget sliver) => MaterialApp(
  home: Scaffold(
    body: MediaQuery(
      data: const MediaQueryData(textScaler: TextScaler.linear(2)),
      child: SizedBox(
        width: 320,
        height: 600,
        child: CustomScrollView(slivers: [sliver]),
      ),
    ),
  ),
);

class _RecordingCubit<S, E> extends Cubit<S> {
  _RecordingCubit(super.initialState);
  void add(E event) {}
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _UserBloc extends _RecordingCubit<UserState, UserEvent>
    implements UserBloc {
  _UserBloc(super.initialState);
}

class _OfferBloc extends _RecordingCubit<OfferState, OfferEvent>
    implements OfferBloc {
  _OfferBloc(super.initialState);
}

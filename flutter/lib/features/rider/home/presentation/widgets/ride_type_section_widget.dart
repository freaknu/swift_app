import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';

import 'package:swift_app/features/rider/home/presentation/widgets/home_section_widgets.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/ride_type_card_widget.dart';

import 'package:flutter/cupertino.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class RideTypeSection extends StatelessWidget {
  final ValueChanged<RideType>? onRideTypeSelected;

  const RideTypeSection({super.key, this.onRideTypeSelected});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<RideTypeBloc, RideTypeState>(
      builder: (context, state) {
        final rideTypes = state.rideTypes.where((type) => type.active).toList()
          ..sort((a, b) => a.sortOrder.compareTo(b.sortOrder));

        Widget content;

        if ((state.status == RideTypeStatus.loading ||
                state.status == RideTypeStatus.initial) &&
            state.rideTypes.isEmpty) {
          content = const HomeStatusCard(
            icon: SwiftIcons.car,
            title: 'Loading rides',
            loading: true,
          );
        } else if (state.status == RideTypeStatus.failure &&
            state.rideTypes.isEmpty) {
          content = HomeStatusCard(
            icon: SwiftIcons.car,
            title: 'Vehicle types unavailable',
            message: state.message == null || state.message!.isEmpty
                ? 'Check your connection.'
                : state.message,
            onRetry: () {
              context.read<RideTypeBloc>().add(const FetchRideTypes());
            },
          );
        } else if (rideTypes.isEmpty) {
          content = HomeStatusCard(
            icon: SwiftIcons.car,
            title: 'Vehicle types unavailable',
            message: 'The catalog is empty. Please try again.',
            onRetry: () {
              context.read<RideTypeBloc>().add(const FetchRideTypes());
            },
          );
        } else {
          final double scale = MediaQuery.textScalerOf(
            context,
          ).scale(1).clamp(1.0, 2.5);

          content = SizedBox(
            height: 152 + (scale - 1) * 64,

            child: ListView.separated(
              physics: const BouncingScrollPhysics(),

              scrollDirection: Axis.horizontal,

              itemCount: rideTypes.length,

              separatorBuilder: (_, _) => const SizedBox(width: 10),

              itemBuilder: (context, index) {
                final RideType type = rideTypes[index];

                return SizedBox(
                  width: 140 + (scale - 1) * 22,

                  child: RideTypeCard(
                    rideType: type,

                    selected: state.selectedRideTypeId == type.id,

                    onTap: () {
                      /// Select ride type inside RideTypeBloc.
                      context.read<RideTypeBloc>().add(
                        SelectRideType(rideTypeId: type.id),
                      );

                      /// Tell parent page which ride
                      /// type has been selected.
                      onRideTypeSelected?.call(type);

                      /// Load offers for selected ride type.
                      context.read<OfferBloc>().add(
                        FetchOffers(
                          request: OfferRequest(
                            rideTypeCode: type.code,

                            startFrom: DateTime.now(),
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          );
        }

        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 18),

          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,

            children: [
              const HomeSectionHeading(title: 'Vehicle type'),

              content,
            ],
          ),
        );
      },
    );
  }
}

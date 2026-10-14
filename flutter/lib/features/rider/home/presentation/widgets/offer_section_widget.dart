import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';

import 'package:swift_app/features/rider/home/presentation/widgets/home_section_widgets.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_card_widget.dart';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:swift_app/themes/app_radii.dart';

class OfferSection extends StatefulWidget {
  final bool expanded;
  final bool asSliver;
  final bool browseCategories;

  final int? selectedOfferId;

  final ValueChanged<OfferResponse>? onOfferSelected;

  const OfferSection({
    super.key,
    this.expanded = false,
    this.asSliver = false,
    this.browseCategories = false,
    this.selectedOfferId,
    this.onOfferSelected,
  });

  @override
  State<OfferSection> createState() => _OfferSectionState();
}

class _OfferSectionState extends State<OfferSection> {
  bool _showAll = false;

  @override
  void initState() {
    super.initState();
    if (widget.browseCategories) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final categories = context.read<RideTypeBloc>().state;
        if (categories.status == RideTypeStatus.initial) {
          context.read<RideTypeBloc>().add(const FetchRideTypes());
        }
        _loadFirstCategory(categories);
      });
    }
  }

  void _loadFirstCategory(RideTypeState state) {
    if (context.read<OfferBloc>().state.lastRequest != null) return;
    final types = state.rideTypes.where((type) => type.active);
    if (types.isNotEmpty) {
      context.read<OfferBloc>().add(
        FetchOffers(request: OfferRequest(rideTypeCode: types.first.code)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<OfferBloc, OfferState>(
      builder: (context, state) {
        final DateTime now = DateTime.now();

        final offers = state.offers
            .where(
              (offer) =>
                  offer.active != false &&
                  (offer.validUntil == null ||
                      !offer.validUntil!.isBefore(now)),
            )
            .toList();

        Widget content;
        bool lazyContent = false;

        if (state.status == OfferStatus.initial) {
          content = const HomeStatusCard(
            icon: SwiftIcons.tag,
            title: 'Choose a ride type',
            message: 'Available offers will appear here.',
          );
        } else if (state.status == OfferStatus.loading) {
          content = const HomeStatusCard(
            icon: SwiftIcons.tag,
            title: 'Loading offers',
            loading: true,
          );
        } else if (state.status == OfferStatus.failure) {
          content = HomeStatusCard(
            icon: SwiftIcons.tag,

            title: 'Offers unavailable',

            message: state.message == null || state.message!.isEmpty
                ? 'Check your connection.'
                : state.message,

            onRetry: state.lastRequest == null
                ? null
                : () {
                    context.read<OfferBloc>().add(
                      FetchOffers(request: state.lastRequest!),
                    );
                  },
          );
        } else if (offers.isEmpty) {
          content = const HomeStatusCard(
            icon: SwiftIcons.tag,

            title: 'No offers right now',

            message: 'You can still request this ride.',
          );
        } else {
          final visible = widget.expanded || _showAll
              ? offers
              : offers.take(2).toList();
          Widget card(int index) {
            final offer = visible[index];
            return Padding(
              padding: const EdgeInsets.only(bottom: 9),

              child: GestureDetector(
                behavior: HitTestBehavior.opaque,

                onTap: () {
                  widget.onOfferSelected?.call(offer);
                },

                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),

                  padding: const EdgeInsets.all(2),

                  decoration: BoxDecoration(
                    borderRadius: AppRadii.cardRadius,

                    border: widget.selectedOfferId == offer.id
                        ? Border.all(
                            color: Theme.of(context).colorScheme.primary,

                            width: 2,
                          )
                        : null,
                  ),

                  child: Stack(
                    children: [
                      OfferCard(offer: offer),

                      if (widget.selectedOfferId == offer.id)
                        Positioned(
                          top: 10,
                          right: 10,

                          child: Container(
                            width: 26,
                            height: 26,

                            decoration: BoxDecoration(
                              color: Theme.of(context).colorScheme.primary,

                              shape: BoxShape.circle,
                            ),

                            child: AppIcon(
                              SwiftIcons.check,

                              color: Theme.of(context).colorScheme.onPrimary,

                              size: 15,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            );
          }

          lazyContent = widget.asSliver;
          content = widget.asSliver
              ? SliverList.builder(
                  itemCount: visible.length,
                  itemBuilder: (_, index) => card(index),
                )
              : Column(
                  children: [for (var i = 0; i < visible.length; i++) card(i)],
                );
        }

        final heading = Column(
          crossAxisAlignment: CrossAxisAlignment.start,

          children: [
            if (widget.browseCategories) ...[
              BlocConsumer<RideTypeBloc, RideTypeState>(
                listener: (_, categories) => _loadFirstCategory(categories),
                builder: (context, categories) {
                  final types = categories.rideTypes
                      .where((type) => type.active)
                      .toList();
                  if (types.isEmpty) {
                    return HomeStatusCard(
                      icon: SwiftIcons.car,
                      title: categories.status == RideTypeStatus.failure
                          ? 'Vehicle types unavailable'
                          : categories.status == RideTypeStatus.success
                          ? 'No vehicle types available'
                          : 'Loading vehicle types',
                      loading:
                          categories.status == RideTypeStatus.loading ||
                          categories.status == RideTypeStatus.initial,
                      onRetry: categories.status == RideTypeStatus.failure
                          ? () => context.read<RideTypeBloc>().add(
                              const FetchRideTypes(),
                            )
                          : null,
                    );
                  }
                  final selected =
                      types.any(
                        (type) => type.code == state.lastRequest?.rideTypeCode,
                      )
                      ? state.lastRequest!.rideTypeCode
                      : types.first.code;
                  return DropdownButtonFormField<String>(
                    key: ValueKey(selected),
                    initialValue: selected,
                    icon: const AppIcon(SwiftIcons.chevronDown, size: 18),
                    isExpanded: true,
                    decoration: const InputDecoration(
                      labelText: 'Vehicle type',
                    ),
                    items: [
                      for (final type in types)
                        DropdownMenuItem(
                          value: type.code,
                          child: Text(
                            type.name,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (code) {
                      if (code != null) {
                        context.read<OfferBloc>().add(
                          FetchOffers(
                            request: OfferRequest(rideTypeCode: code),
                          ),
                        );
                      }
                    },
                  );
                },
              ),
              const SizedBox(height: 18),
            ],
            if (!widget.expanded)
              HomeSectionHeading(
                title: 'Offers',

                subtitle: state.lastRequest?.rideTypeCode == null
                    ? null
                    : state.lastRequest!.rideTypeCode!.toLowerCase(),

                action: offers.length > 2
                    ? CupertinoButton(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),

                        minimumSize: const Size(44, 44),

                        onPressed: () {
                          setState(() {
                            _showAll = !_showAll;
                          });
                        },

                        child: Text(
                          _showAll ? 'Show less' : 'See all',

                          style: const TextStyle(
                            fontSize: 13,

                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      )
                    : null,
              ),
          ],
        );
        if (widget.asSliver) {
          return SliverPadding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 18),
            sliver: SliverMainAxisGroup(
              slivers: [
                SliverToBoxAdapter(child: heading),
                lazyContent ? content : SliverToBoxAdapter(child: content),
              ],
            ),
          );
        }
        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [heading, content],
          ),
        );
      },
    );
  }
}

import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_state.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/home_section_widgets.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/ride_history_card_widget.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';

class RideHistorySection extends StatelessWidget {
  final int? maxItems;
  final bool asSliver;

  const RideHistorySection({
    super.key,
    this.maxItems = 3,
    this.asSliver = false,
  }) : assert(
         maxItems != null || asSliver,
         'Full history needs a lazy sliver viewport.',
       );

  void _showHistory(BuildContext context) {
    final bloc = context.read<RideHistoryBloc>();
    Navigator.of(context).push(
      CupertinoPageRoute<void>(
        builder: (_) => BlocProvider.value(
          value: bloc,
          child: Scaffold(
            appBar: SwiftAppBar(
              toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
              title: const Text('Activity'),
            ),
            body: const SafeArea(
              child: CustomScrollView(
                slivers: [RideHistorySection(maxItems: null, asSliver: true)],
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) =>
      BlocBuilder<RideHistoryBloc, RideHistoryState>(
        builder: (context, state) {
          Widget? status;
          if (state.rides.isEmpty) {
            status = switch (state.status) {
              RideHistoryStatus.initial ||
              RideHistoryStatus.loading => const HomeStatusCard(
                icon: SwiftIcons.clock3,
                title: 'Loading rides',
                loading: true,
              ),
              RideHistoryStatus.failure => HomeStatusCard(
                icon: SwiftIcons.clock3,
                title: 'Rides unavailable',
                message: state.message == null || state.message!.isEmpty
                    ? 'Check your connection.'
                    : state.message,
                onRetry: () => context.read<RideHistoryBloc>().add(
                  const FetchRideHistory(),
                ),
              ),
              _ => const HomeStatusCard(
                icon: SwiftIcons.map,
                title: 'No rides yet',
                message: 'Your completed rides will appear here.',
              ),
            };
          }
          final count = maxItems == null || maxItems! > state.rides.length
              ? state.rides.length
              : maxItems!;
          Widget card(int index) {
            final ride = state.rides[index];
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: RideHistoryCard(
                key: ValueKey('${ride.id}-${ride.rideId}'),
                ride: ride,
              ),
            );
          }

          if (asSliver) {
            return SliverPadding(
              padding: const EdgeInsets.fromLTRB(12, 0, 12, 18),
              sliver: status != null
                  ? SliverToBoxAdapter(child: status)
                  : SliverList.builder(
                      itemCount: count,
                      itemBuilder: (_, index) => card(index),
                    ),
            );
          }
          return Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                HomeSectionHeading(
                  title: 'Recent',
                  action: state.rides.length > count
                      ? CupertinoButton(
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          minimumSize: const Size(44, 44),
                          onPressed: () => _showHistory(context),
                          child: const Text(
                            'See all',
                            style: TextStyle(fontSize: 14),
                          ),
                        )
                      : null,
                ),
                if (status != null)
                  status
                else
                  for (var index = 0; index < count; index++) card(index),
              ],
            ),
          );
        },
      );
}

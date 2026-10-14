import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/pages/account_settings_pages.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/add_address_sheet_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/home_section_widgets.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/saved_address_card_widget.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class SavedAddressSection extends StatelessWidget {
  final bool expanded;
  final bool asSliver;

  final ValueChanged<UserAddress>? onAddressSelected;

  const SavedAddressSection({
    super.key,
    this.expanded = false,
    this.asSliver = false,
    this.onAddressSelected,
  });

  void _openAddAddressSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      useSafeArea: true,
      builder: (_) => BlocProvider.value(
        value: context.read<UserBloc>(),
        child: const AddAddressSheet(),
      ),
    );
  }

  void _selectAddress(BuildContext context, UserAddress address) {
    if (onAddressSelected != null) {
      onAddressSelected!(address);
      return;
    }

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      useSafeArea: true,
      isScrollControlled: true,
      builder: (context) {
        final ThemeData theme = Theme.of(context);

        return ClipRRect(
          borderRadius: AppRadii.sheetRadius,
          child: RepaintBoundary(
            child: Container(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 22),
              decoration: BoxDecoration(
                color: theme.colorScheme.surface,
                borderRadius: AppRadii.sheetRadius,
                border: Border(
                  top: BorderSide(color: theme.colorScheme.outlineVariant),
                ),
              ),
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Container(
                        width: 38,
                        height: 5,
                        decoration: BoxDecoration(
                          color: theme.colorScheme.onSurface.withValues(
                            alpha: 0.16,
                          ),
                          borderRadius: BorderRadius.circular(999),
                        ),
                      ),
                    ),

                    const SizedBox(height: 18),

                    Text(
                      address.label,
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w700,
                        letterSpacing: -0.4,
                      ),
                    ),

                    const SizedBox(height: 8),

                    SelectableText(
                      address.fullAddress.isEmpty
                          ? 'Saved location'
                          : address.fullAddress,
                      style: theme.textTheme.bodyLarge,
                    ),

                    if (address.landmark.isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Text(
                        'Landmark: ${address.landmark}',
                        style: theme.textTheme.bodyMedium,
                      ),
                    ],

                    const SizedBox(height: 14),

                    Text(
                      '${address.latitude.toStringAsFixed(5)}, '
                      '${address.longitude.toStringAsFixed(5)}',
                      style: theme.textTheme.bodySmall,
                    ),

                    const SizedBox(height: 20),

                    SizedBox(
                      width: double.infinity,
                      child: CupertinoButton(
                        color: Theme.of(context).colorScheme.primary,
                        borderRadius: BorderRadius.circular(16),
                        onPressed: () {
                          Navigator.of(context).pop();
                        },
                        child: const Text(
                          'Done',
                          style: TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Future<void> _removeAddress(BuildContext context, UserAddress address) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Remove ${address.label}?'),
        content: const Text('You can add this place again later.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Keep'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );
    if (confirmed != true || !context.mounted) return;
    try {
      await riderAccountApi().removeAddress(address.id);
      if (context.mounted) {
        context.read<UserBloc>().add(const FetchUserDetails());
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(error.toString().replaceFirst('Bad state: ', '')),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<UserBloc, UserState>(
      builder: (context, state) {
        final List<UserAddress> addresses =
            state.user?.allAddresses ?? <UserAddress>[];

        Widget content;
        bool lazyContent = false;

        if ((state.status == UserStatus.loading ||
                state.status == UserStatus.initial) &&
            state.user == null) {
          content = const HomeStatusCard(
            icon: SwiftIcons.bookmark,
            title: 'Loading places',
            loading: true,
          );
        } else if (state.status == UserStatus.failure && state.user == null) {
          content = HomeStatusCard(
            icon: SwiftIcons.cloudOff,
            title: 'Places unavailable',
            message: 'Check your connection.',
            onRetry: () {
              context.read<UserBloc>().add(const FetchUserDetails());
            },
          );
        } else if (addresses.isEmpty) {
          content = HomeStatusCard(
            icon: SwiftIcons.bookmark,
            title: 'No saved places',
            message: 'Add home, work, or a favourite.',
            actionLabel: 'Add place',
            onRetry: () {
              _openAddAddressSheet(context);
            },
          );
        } else if (expanded) {
          Widget card(int index) {
            final address = addresses[index];
            return Padding(
              padding: const EdgeInsets.only(bottom: 9),
              child: Row(
                children: [
                  Expanded(
                    child: SavedAddressCard(
                      address: address,
                      expanded: true,
                      onTap: () {
                        _selectAddress(context, address);
                      },
                    ),
                  ),
                  IconButton(
                    tooltip: 'Remove ${address.label}',
                    onPressed: () => _removeAddress(context, address),
                    icon: const AppIcon(SwiftIcons.trash, size: 18),
                  ),
                ],
              ),
            );
          }

          lazyContent = asSliver;
          content = asSliver
              ? SliverList.builder(
                  itemCount: addresses.length,
                  itemBuilder: (_, index) => card(index),
                )
              : Column(
                  children: [
                    for (var i = 0; i < addresses.length; i++) card(i),
                  ],
                );
        } else {
          final double scale = MediaQuery.textScalerOf(
            context,
          ).scale(1).clamp(1.0, 2.0);

          content = SizedBox(
            height: 68 + (scale - 1) * 30,

            child: ListView.separated(
              physics: const BouncingScrollPhysics(),
              scrollDirection: Axis.horizontal,
              itemCount: addresses.length,
              separatorBuilder: (_, _) => const SizedBox(width: 10),
              itemBuilder: (_, index) => SavedAddressCard(
                address: addresses[index],
                onTap: () {
                  _selectAddress(context, addresses[index]);
                },
              ),
            ),
          );
        }

        final Widget addButton = CupertinoButton(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
          minimumSize: const Size(44, 44),
          onPressed: () {
            _openAddAddressSheet(context);
          },
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppIcon(SwiftIcons.plus, size: 18),
              SizedBox(width: 3),
              Text(
                'Add',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        );

        final heading = expanded
            ? Align(alignment: Alignment.centerRight, child: addButton)
            : HomeSectionHeading(title: 'Saved', action: addButton);
        if (asSliver) {
          return SliverPadding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 18),
            sliver: SliverMainAxisGroup(
              slivers: [
                SliverToBoxAdapter(child: heading),
                lazyContent ? content : SliverToBoxAdapter(child: content),
              ],
            ),
          );
        }
        return Padding(
          padding: const EdgeInsets.fromLTRB(12, 0, 12, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [heading, content],
          ),
        );
      },
    );
  }
}

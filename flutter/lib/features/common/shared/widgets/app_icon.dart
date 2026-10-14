import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

/// Selected Lucide artwork downloaded from its official repository and bundled
/// with the app. The source revision and license live beside the SVG assets.
enum SwiftIcons {
  arrowLeft('arrow-left'),
  arrowRight('arrow-right'),
  arrowUp('arrow-up'),
  arrowDown('arrow-down'),
  chevronLeft('chevron-left'),
  chevronRight('chevron-right'),
  chevronDown('chevron-down'),
  chevronUp('chevron-up'),
  refreshCw('refresh-cw'),
  bell('bell'),
  bellOff('bell-off'),
  search('search'),
  x('x'),
  menu('menu'),
  settings('settings'),
  sun('sun'),
  moon('moon'),
  circleCheck('circle-check'),
  info('info'),
  car('car'),
  userRound('user-round'),
  mapPin('map-pin'),
  navigation('navigation'),
  bookmark('bookmark'),
  clock3('clock-3'),
  tag('tag'),
  receiptText('receipt-text'),
  house('house'),
  logOut('log-out'),
  mail('mail'),
  phone('phone'),
  messageCircle('message-circle'),
  send('send'),
  creditCard('credit-card'),
  wallet('wallet'),
  flag('flag'),
  check('check'),
  star('star'),
  route('route'),
  wifiOff('wifi-off'),
  cloudOff('cloud-off'),
  triangleAlert('triangle-alert'),
  circleAlert('circle-alert'),
  loaderCircle('loader-circle'),
  plus('plus'),
  minus('minus'),
  circle('circle'),
  eye('eye'),
  eyeOff('eye-off'),
  shieldCheck('shield-check'),
  fileText('file-text'),
  inbox('inbox'),
  contactRound('contact-round'),
  map('map'),
  pencil('pencil'),
  calendarDays('calendar-days'),
  badgeCheck('badge-check'),
  usersRound('users-round'),
  heart('heart'),
  gift('gift'),
  arrowUpRight('arrow-up-right'),
  locateFixed('locate-fixed'),
  timer('timer'),
  volume2('volume-2'),
  banknote('banknote'),
  lifeBuoy('life-buoy'),
  externalLink('external-link'),
  copy('copy'),
  trash('trash'),
  lockKeyhole('lock-keyhole'),
  bike('bike'),
  moveRight('move-right'),
  checkCheck('check-check'),
  circleArrowUp('circle-arrow-up'),
  circleArrowRight('circle-arrow-right'),
  circleX('circle-x'),
  circlePause('circle-pause'),
  play('play'),
  pause('pause'),
  keyRound('key-round'),
  sparkles('sparkles'),
  upload('upload'),
  circleQuestionMark('circle-question-mark'),
  gauge('gauge'),
  ruler('ruler'),
  palette('palette'),
  ticket('ticket'),
  briefcase('briefcase'),
  cake('cake'),
  power('power'),
  atSign('at-sign'),
  mic('mic'),
  busFront('bus-front'),
  carTaxiFront('car-taxi-front'),
  zap('zap');

  const SwiftIcons(this.fileName);
  final String fileName;
  String get assetPath => 'assets/icons/lucide/$fileName.svg';
}

/// Thin SVG artwork with the same color inheritance as Flutter's Icon widget.
/// The visible mark stays small; its enclosing button owns the touch target.
class AppIcon extends StatelessWidget {
  const AppIcon(
    this.icon, {
    super.key,
    this.size,
    this.color,
    this.semanticLabel,
    this.matchTextDirection = false,
  });

  final SwiftIcons icon;
  final double? size;
  final Color? color;
  final String? semanticLabel;
  final bool matchTextDirection;

  @override
  Widget build(BuildContext context) {
    final theme = IconTheme.of(context);
    final effectiveSize = size ?? theme.size ?? 20;
    final baseColor =
        color ?? theme.color ?? Theme.of(context).colorScheme.onSurface;
    final effectiveColor = baseColor.withValues(
      alpha: baseColor.a * (theme.opacity ?? 1),
    );
    // A circular badge can impose a much larger tight size on this widget.
    // Loosen that constraint so the artwork stays at its requested size.
    return Center(
      widthFactor: 1,
      heightFactor: 1,
      child: SizedBox.square(
        dimension: effectiveSize,
        child: SvgPicture.asset(
          icon.assetPath,
          width: effectiveSize,
          height: effectiveSize,
          colorFilter: ColorFilter.mode(effectiveColor, BlendMode.srcIn),
          semanticsLabel: semanticLabel,
          excludeFromSemantics: semanticLabel == null,
          matchTextDirection: matchTextDirection,
        ),
      ),
    );
  }
}

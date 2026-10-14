import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';

void main() {
  testWidgets(
    'catalog categories use distinct bundled photos with bounded decode',
    (tester) async {
      const codes = [
        'BIKE',
        'SCOOTER',
        'AUTO_RICKSHAW',
        'SEDAN',
        'SUV',
        'DELIVERY_VAN',
      ];
      await tester.pumpWidget(
        MaterialApp(
          home: MediaQuery(
            data: const MediaQueryData(devicePixelRatio: 4),
            child: Scaffold(
              body: Column(
                children: [
                  for (final code in codes)
                    VehiclePhoto(code: code, width: 240, height: 70),
                ],
              ),
            ),
          ),
        ),
      );
      await tester.runAsync(() async {
        for (final image in tester.widgetList<Image>(find.byType(Image))) {
          await precacheImage(
            image.image,
            tester.element(find.byWidget(image)),
          );
        }
      });
      await tester.pumpAndSettle();
      final assets = <String>{};
      for (final image in tester.widgetList<Image>(find.byType(Image))) {
        final provider = image.image as ResizeImage;
        expect(provider.width, 640);
        expect(image.excludeFromSemantics, isTrue);
        assets.add((provider.imageProvider as AssetImage).assetName);
      }
      expect(assets.length, codes.length);
      expect(find.text('Photo unavailable'), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );

  test('electric and premium variants keep the correct vehicle silhouette', () {
    expect(
      VehiclePhoto.assetFor('ELECTRIC_BIKE'),
      VehiclePhoto.assetFor('BIKE'),
    );
    expect(
      VehiclePhoto.assetFor('ELECTRIC_SCOOTER'),
      VehiclePhoto.assetFor('SCOOTER'),
    );
    expect(VehiclePhoto.assetFor('PREMIUM_SUV'), VehiclePhoto.assetFor('SUV'));
    expect(
      VehiclePhoto.assetFor('ELECTRIC_CAR'),
      VehiclePhoto.assetFor('SEDAN'),
    );
    expect(
      VehiclePhoto.assetFor('NEW_CATALOG_CATEGORY'),
      VehiclePhoto.assetFor('SEDAN'),
    );
  });
}

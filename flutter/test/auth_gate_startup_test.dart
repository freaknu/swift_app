import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/pages/welcome_page.dart';
import 'package:swift_app/main.dart';

class _AuthStateCubit extends Cubit<AuthState> implements AuthBloc {
  _AuthStateCubit() : super(const AuthInitial());

  void update(AuthState state) => emit(state);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  testWidgets('launch never flashes welcome before saved session is checked', (
    tester,
  ) async {
    final auth = _AuthStateCubit();
    addTearDown(auth.close);
    await tester.pumpWidget(
      BlocProvider<AuthBloc>.value(
        value: auth,
        child: const MaterialApp(home: AuthGate()),
      ),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    expect(find.byType(WelcomePage), findsNothing);

    auth.update(const AuthLoading(AuthAction.checking));
    await tester.pump();
    expect(find.byType(AppLoadingPage), findsOneWidget);
    expect(find.byType(WelcomePage), findsNothing);

    auth.update(const AuthUnauthenticated());
    await tester.pump();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.byType(WelcomePage), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}

sealed class AppFailure implements Exception {
  const AppFailure();

  String get title;
  String get message;
}

class ConnectionFailure extends AppFailure {
  const ConnectionFailure();

  @override
  String get title => 'Couldn\'t connect';

  @override
  String get message => 'Check your connection and try again.';
}

class TimeoutFailure extends AppFailure {
  const TimeoutFailure();

  @override
  String get title => 'This is taking longer than usual';

  @override
  String get message => 'Check your connection and try again.';
}

class CancelledFailure extends AppFailure {
  const CancelledFailure();

  @override
  String get title => 'Route request cancelled';

  @override
  String get message => 'Choose a start and destination when you are ready.';
}

class ServerFailure extends AppFailure {
  const ServerFailure();

  @override
  String get title => 'Something went wrong';

  @override
  String get message => 'Try again shortly.';
}

class UnauthorizedFailure extends AppFailure {
  const UnauthorizedFailure();

  @override
  String get title => 'Signed out';

  @override
  String get message => 'Sign in again to continue.';
}

class InvalidCredentialsFailure extends AppFailure {
  const InvalidCredentialsFailure();

  @override
  String get title => 'Sign in failed';

  @override
  String get message => 'Username or password is incorrect.';
}

class NotFoundFailure extends AppFailure {
  const NotFoundFailure();

  @override
  String get title => 'No longer available';

  @override
  String get message => 'This item is no longer available.';
}

class ConflictFailure extends AppFailure {
  const ConflictFailure();

  @override
  String get title => 'This changed while you were editing';

  @override
  String get message => 'Reload the latest details and try again.';
}

class RateLimitedFailure extends AppFailure {
  const RateLimitedFailure();

  @override
  String get title => 'Try again shortly';

  @override
  String get message => 'Too many requests were made. Please wait and try again.';
}

class ValidationFailure extends AppFailure {
  const ValidationFailure();

  @override
  String get title => 'Check the information';

  @override
  String get message => 'Some information needs to be corrected.';
}

class InvalidResponseFailure extends AppFailure {
  const InvalidResponseFailure();

  @override
  String get title => 'Couldn\'t load buildings';

  @override
  String get message => 'Try again.';
}

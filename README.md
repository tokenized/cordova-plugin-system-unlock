# Cordova System Unlock Plugin

A Cordova plugin for managing secrets protected by iOS/Android biometrics and
passcode. This is a fork of the excellent
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme),
adding support for multiple named secrets, more options for configuring the
unlocking behavior of secrets, and exporting additional JS functions for
checking whether a stored secret already exists, and deleting secrets.

## Under construction!

This fork has been developed to support the
[Tokenized Mobile Authenticator App](https://tokenized.com), and is not
recommended over the original
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme)
unless your exact needs happen to match ours. Currently the only stored secret
configurations that are implemented and tested are these two:

- `{scope: 'onePasscode', lockBehavior: 'lockWithDevice'}`: A secret which is
  accessible to the app whenever the device is unlocked, and gets deleted if the
  user turns off the system passcode. The secret never leaves the device it’s
  created on (not saved to system backups, and never transferred, restored, or
  synced to other devices).
- `{scope: 'onePasscode', lockBehavior: 'lockAfterUsePasscodeFallback'}`: A
  secret which has to be unlocked by the user every time it’s used, either using
  biometrics or the system passcode, and gets deleted if the user turns off the
  system passcode. The secret never leaves the device it’s created on (not saved
  to system backups, and never transferred, restored, or synced to other
  devices).

## Acknowledgements

Many thanks to Niklas Merz and the other contributors to
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme).

## License

The project is MIT licensed: [MIT](https://opensource.org/licenses/MIT).

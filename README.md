# Cordova System Unlock Plugin

A Cordova plugin for managing secrets protected by iOS/Android biometrics and
passcode. This is a fork of the excellent
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme),
adding support for multiple named secrets, more options for configuring the
unlocking behavior of secrets, and exporting additional JS functions for
checking whether a stored secret already exists, and deleting secrets.

Please consult [`SystemUnlock.js`](www/SystemUnlock.js) for more details of the
available functions and options.

## Requirements

This plugin will not work on OS versions prior to:

- iOS version 14.5
- Android 9 (API level 28)

On Android, unlocking with the system password/PIN/pattern is only supported on
Android 11 and later. All supported versions of iOS support fallback to the
system passcode.

## Under construction!

This fork has been developed to support the
[Tokenized Mobile Authenticator App](https://tokenized.com), and is not
recommended over the original
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme)
unless your exact needs happen to match ours. Currently the only stored secret
configurations that are fully implemented and tested are these:

- `{scope: 'activeSystemLock', lockBehavior: 'lockWithDevice'}`: A secret which
  is accessible to the app whenever the device is unlocked, and gets deleted if
  the user turns off the system password/PIN/pattern. The secret never leaves
  the device it’s created on (not saved to system backups, and never
  transferred, restored, or synced to other devices). On Android the secret will
  occasionally require full biometic/passcode unlock (every two weeks by
  default).
- `{scope: 'activeSystemLock', lockBehavior: 'lockAfterUse'}`: A secret which
  has to be unlocked by the user every time it’s used, either using biometrics
  or the system password/PIN/pattern (if supported – see above), and gets
  deleted if the user turns off the system password/PIN/pattern. The secret
  never leaves the device it’s created on (not saved to system backups, and
  never transferred, restored, or synced to other devices).
- (iOS only) `{scope: 'sync', lockBehavior: 'lockAfterUse'}`: A secret which is
  saved in iCloud Keychain (synced to all devices, end-to-end encrypted). For
  this to work, iCloud must be signed in on the device (which can be checked:
  `await window.SystemUnlock.isiCloudLoggedIn()`), and the “Passwords and
  Keychain” feature must be turned on in iCloud settings, which can not be
  checked programmatically (you must prompt the user to check). If it isn’t
  turned on, the secret will be saved locally (and also in the system backup, if
  enabled), but won’t be synced to other devices. Setting `lockAfterUse` causes
  the plugin to challenge the user to unlock with Face ID, Touch ID, or the
  system passcode every time the secret is accessed.

## Acknowledgements

Many thanks to Niklas Merz and the other contributors to
[`cordova-plugin-fingerprint-aio`](https://github.com/niklasmerz/cordova-plugin-fingerprint-aio#readme).

## License

The project is MIT licensed: [MIT](https://opensource.org/licenses/MIT).

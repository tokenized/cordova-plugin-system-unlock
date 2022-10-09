/* global cordova */

class SystemUnlock {
  // Plugin Errors
  BIOMETRIC_UNKNOWN_ERROR = -100;
  BIOMETRIC_UNAVAILABLE = -101;
  BIOMETRIC_AUTHENTICATION_FAILED = -102;
  BIOMETRIC_SDK_NOT_SUPPORTED = -103;
  BIOMETRIC_HARDWARE_NOT_SUPPORTED = -104;
  BIOMETRIC_PERMISSION_NOT_GRANTED = -105;
  BIOMETRIC_NOT_ENROLLED = -106;
  BIOMETRIC_INTERNAL_PLUGIN_ERROR = -107;
  BIOMETRIC_DISMISSED = -108;
  BIOMETRIC_PIN_OR_PATTERN_DISMISSED = -109;
  BIOMETRIC_SCREEN_GUARD_UNSECURED = -110;
  BIOMETRIC_LOCKED_OUT = -111;
  BIOMETRIC_LOCKED_OUT_PERMANENT = -112;
  BIOMETRIC_NO_SECRET_FOUND = -113;

  // Biometric types
  BIOMETRIC_TYPE_FINGERPRINT = 'finger';
  BIOMETRIC_TYPE_FACE = 'face';
  BIOMETRIC_TYPE_COMMON = 'biometric';

  execNative(name, options) {
    return new Promise((resolve, reject) => {
      cordova.exec(
        (result) => {
          resolve(result);
        },
        (errorInfo) => {
          if (errorInfo instanceof Error) {
            reject(errorInfo);
          } else if (errorInfo && typeof errorInfo === 'string') {
            reject(new Error(errorInfo));
          }

          let error = new Error();
          if (typeof errorInfo === 'object') {
            error = Object.assign(error, errorInfo);
          }
          if (
            error?.code === this.BIOMETRIC_DISMISSED ||
            error?.code === this.BIOMETRIC_PIN_OR_PATTERN_DISMISSED
          ) {
            error.wasCancelledByUser = true;
          }
          reject(error);
        },
        'SystemUnlock',
        name,
        [options],
      );
    });
  }

  /**
   * @param {Object} options
   * @param {'lockAfterUsePasscodeFallback' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUsePasscodeFallback']
   * @returns {Promise<'none' | 'finger' | 'face' | 'unknown'>}
   */
  async isAvailable(options) {
    return await this.execNative('isAvailable', options);
  }

  /**
   * @returns {Promise<boolean>}
   */
  async isiCloudLoggedIn() {
    return await this.execNative('isiCloudLoggedIn');
  }

  /**
   * @param {Object} options
   * @param {'lockAfterUsePasscodeFallback' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUsePasscodeFallback']
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @returns {Promise<void>}
   */
  async challenge(options) {
    return await this.execNative('challenge', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @param {string} options.secret
   * @param {'sync' | 'backup' | 'oneDevice' | 'onePasscode' | 'oneBiometric'} [options.scope='onePasscode']
   * @param {'lockWithDevice' | 'lockAfterUsePasscodeFallback' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUsePasscodeFallback']
   * @param {boolean} [options.interactionNotAllowed=false]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @returns {Promise<void>}
   */
  async setSecret(options) {
    return await this.execNative('setSecret', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @returns {Promise<boolean>}
   */
  async hasSecret(options) {
    return await this.execNative('hasSecret', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @param {boolean} [options.interactionNotAllowed=false]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @returns {Promise<string>}
   */
  async getSecret(options) {
    return await this.execNative('getSecret', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @param {'lockWithDevice' | 'lockAfterUsePasscodeFallback' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUsePasscodeFallback']
   * @param {boolean} [options.interactionNotAllowed=false]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @returns {Promise<string>}
   */
  async deleteSecret(options) {
    return await this.execNative('deleteSecret', options);
  }
}

module.exports = new SystemUnlock();

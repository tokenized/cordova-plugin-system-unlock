/* global cordova */

// See https://advancedweb.hu/how-to-serialize-calls-to-an-async-function/
const serialize = (fn) => {
  let queue = Promise.resolve();
  return (...args) => {
    const res = queue.then(() => fn(...args));
    queue = res.catch(() => {});
    return res;
  };
};

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

  debugOn = false;

  /** @param {boolean} enable */
  debug(enable) {
    this.debugOn = !!enable;
  }

  execNative = serialize((name, options) => {
    return new Promise((resolve, reject) => {
      this.debugOn && console.log(`Running native SystemUnlock.${name}`);
      cordova.exec(
        (result) => {
          this.debugOn &&
            console.log(`Finished native SystemUnlock.${name}: success`);
          resolve(result);
        },
        (errorInfo) => {
          if (errorInfo instanceof Error) {
            this.debugOn &&
              console.log(
                `Finished native SystemUnlock.${name}: error(${errorInfo})`,
              );
            reject(errorInfo);
          } else if (errorInfo && typeof errorInfo === 'string') {
            this.debugOn &&
              console.log(
                `Finished native SystemUnlock.${name}: error(${errorInfo})`,
              );
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
          this.debugOn &&
            console.log(
              `Finished native SystemUnlock.${name}: error(${error})`,
            );
          reject(error);
        },
        'SystemUnlock',
        name,
        [options],
      );
    });
  });

  /**
   * @param {Object} options
   * @returns {Promise<'passcode' | 'finger' | 'finger+passcode' | 'face' | 'face+passcode' | 'biometric' | 'biometric+passcode' | 'unknown'>}
   */
  async isAvailable() {
    return await this.execNative('isAvailable');
  }

  /**
   * @returns {Promise<boolean>}
   */
  async isiCloudLoggedIn() {
    if (window.device?.platform === 'iOS') {
      return await this.execNative('isiCloudLoggedIn');
    }
    return false;
  }

  /**
   * @param {Object} options
   * @param {'lockAfterUse' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUse']
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @returns {Promise<void>}
   */
  async challenge(options) {
    return await this.execNative('challenge', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @param {string} options.secret
   * @param {'sync' | 'backup' | 'oneDevice' | 'activeSystemLock' | 'oneBiometric'} [options.scope='activeSystemLock']
   * @param {'lockWithDevice' | 'lockAfterUse' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUse']
   * @param {number} [options.androidAutoLockTimeSeconds=1209600]
   * @param {boolean} [options.interactionNotAllowed=false]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
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
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @returns {Promise<string>}
   */
  async getSecret(options) {
    return await this.execNative('getSecret', options);
  }

  /**
   * @param {Object} options
   * @param {string} [options.secretName="__aio_key"]
   * @param {'lockWithDevice' | 'lockAfterUse' | 'lockAfterUseBiometricOnly'} [options.lockBehavior='lockAfterUse']
   * @param {boolean} [options.interactionNotAllowed=false]
   * @param {boolean} [options.confirmationRequired=true]
   * @param {'start' | 'continue'} [options.batch]
   * @param {string} [options.title]
   * @param {string} [options.subtitle]
   * @param {string} [options.description]
   * @param {string} [options.fallbackButtonTitle]
   * @param {string} [options.cancelButtonTitle]
   * @returns {Promise<string>}
   */
  async deleteSecret(options) {
    try {
      return await this.execNative('deleteSecret', options);
    } catch (error) {
      if (error?.code !== this.BIOMETRIC_NO_SECRET_FOUND) {
        throw error;
      }
    }
  }
}

module.exports = new SystemUnlock();

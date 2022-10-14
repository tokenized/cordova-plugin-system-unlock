import Foundation
import LocalAuthentication

enum PluginError: Int {
    case BIOMETRIC_UNKNOWN_ERROR = -100
    case BIOMETRIC_UNAVAILABLE = -101
    case BIOMETRIC_AUTHENTICATION_FAILED = -102
    case BIOMETRIC_PERMISSION_NOT_GRANTED = -105
    case BIOMETRIC_NOT_ENROLLED = -106
    case BIOMETRIC_DISMISSED = -108
    case BIOMETRIC_SCREEN_GUARD_UNSECURED = -110
    case BIOMETRIC_LOCKED_OUT = -111
    case BIOMETRIC_SECRET_NOT_FOUND = -113
}

let errorCodeMapping = [
    1: PluginError.BIOMETRIC_AUTHENTICATION_FAILED.rawValue,
    2: PluginError.BIOMETRIC_DISMISSED.rawValue,
    5: PluginError.BIOMETRIC_SCREEN_GUARD_UNSECURED.rawValue,
    6: PluginError.BIOMETRIC_UNAVAILABLE.rawValue,
    7: PluginError.BIOMETRIC_NOT_ENROLLED.rawValue,
    8: PluginError.BIOMETRIC_LOCKED_OUT.rawValue,
]

@objc(SystemUnlock) class SystemUnlock: CDVPlugin {
    static var batchContext: LAContext?

    private func makeAuthPolicy(
        lockBehavior: String
    ) -> LAPolicy {
        switch lockBehavior {
        case "lockAfterUseBiometricOnly":
            return LAPolicy.deviceOwnerAuthenticationWithBiometrics
        case "lockAfterUse":
            return LAPolicy.deviceOwnerAuthentication
        default:
            preconditionFailure(
                "Unsupported SystemUnlock lockBehavior (\(lockBehavior))")
        }
    }

    @objc(isAvailable:)
    func isAvailable(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?
        let lockBehavior =
            options?["lockBehavior"] as! String? ?? "lockAfterUse"

        let authenticationContext = LAContext()

        var error: NSError?
        var available = authenticationContext.canEvaluatePolicy(
            makeAuthPolicy(lockBehavior: lockBehavior), error: &error)
        if error != nil {
            available = false
        }

        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR, messageAs: "Not available")

        if available {
            var biometryType: String
            switch authenticationContext.biometryType {
            case .none:
                biometryType = "passcode"
            case .touchID:
                biometryType = "finger+passcode"
            case .faceID:
                biometryType = "face+passcode"
            @unknown default:
                biometryType = "unknown"
            }

            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: biometryType)
        } else {
            var code: Int
            switch error!._code {
            case Int(kLAErrorBiometryNotAvailable):
                code = PluginError.BIOMETRIC_UNAVAILABLE.rawValue
                break
            case Int(kLAErrorBiometryNotEnrolled):
                code = PluginError.BIOMETRIC_NOT_ENROLLED.rawValue
                break
            default:
                code = PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue
                break
            }

            let errorResult: [String: Any] = ["code": code, "message": error!.localizedDescription]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResult)
        }

        commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(isiCloudLoggedIn:)
    func isiCloudLoggedIn(_ command: CDVInvokedUrlCommand) {
        var result = false
        if FileManager.default.ubiquityIdentityToken != nil {
            result = true
        }

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(challenge:)
    func challenge(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?
        let lockBehavior =
            options?["lockBehavior"] as! String? ?? "lockAfterUse"

        let batch = options?["batch"] as! String?
        var authenticationContext = LAContext()
        if batch == "continue", let previousContext = SystemUnlock.batchContext {
            authenticationContext = previousContext
        }
        if batch == "start" {
            SystemUnlock.batchContext = authenticationContext
        }

        var reason = "Unlock this app"
        if let description = options?["description"] as! String? {
            reason = description
        }

        let errorResponse: [String: Any] = [
            "message": "Something went wrong"
        ]
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResponse)

        authenticationContext.evaluatePolicy(
            makeAuthPolicy(lockBehavior: lockBehavior),
            localizedReason: reason,
            reply: { [unowned self] (success, error) -> Void in
                if success {
                    pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK, messageAs: "Success")
                } else {
                    if error != nil {
                        var errorResult: [String: Any] = [
                            "code": PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue,
                            "message": error?.localizedDescription ?? "",
                        ]

                        let errorCode = abs(error!._code)
                        if let mappedErrorCode = errorCodeMapping[errorCode] {
                            errorResult = [
                                "code": mappedErrorCode, "message": error!.localizedDescription,
                            ]
                        }

                        pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR, messageAs: errorResult)
                    }
                }
                self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
            }
        )
    }

    private func makeInteractiveAuthContextForSecret(
        options: [String: Any]?
    ) -> LAContext {
        let context = LAContext()

        if let description = options?["description"] as! String? {
            context.localizedReason = description
        }
        if let fallbackButtonTitle = options?["fallbackButtonTitle"] as! String? {
            context.localizedFallbackTitle = fallbackButtonTitle
        }
        if let cancelButtonTitle = options?["cancelButtonTitle"] as! String? {
            context.localizedCancelTitle = cancelButtonTitle
        }

        return context
    }

    private func makeAuthContextForSecret(
        options: [String: Any]?
    ) -> LAContext {
        let context = makeInteractiveAuthContextForSecret(options: options)

        if options?["interactionNotAllowed"] as! Bool? ?? false {
            context.interactionNotAllowed = true
        }

        return context
    }

    @objc(setSecret:)
    func setSecret(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?

        let lockBehavior =
            options?["lockBehavior"] as! String? ?? "lockAfterUse"

        let batch = options?["batch"] as! String?
        var context: LAContext
        if batch == "continue", let previousContext = SystemUnlock.batchContext {
            context = previousContext
        } else {
            context = makeAuthContextForSecret(options: options)
        }
        if batch == "start" {
            SystemUnlock.batchContext = context
        }

        if lockBehavior == "lockWithDevice" {
            setSecretInternal(command, context: context)
        } else {
            var reason = "Unlock this app"
            if let description = options?["description"] as! String? {
                reason = description
            }

            context.evaluatePolicy(
                makeAuthPolicy(lockBehavior: lockBehavior),
                localizedReason: reason,
                reply: { [unowned self] (success, error) -> Void in
                    if success {
                        setSecretInternal(command, context: context)
                    } else {
                        var errorResult: [String: Any] = [
                            "code": PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue,
                            "message": error?.localizedDescription ?? "Something went wrong",
                        ]
                        if error != nil {
                            let errorCode = abs(error!._code)
                            if let mappedErrorCode = errorCodeMapping[errorCode] {
                                errorResult = [
                                    "code": mappedErrorCode, "message": error!.localizedDescription,
                                ]
                            }
                        }
                        let pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR, messageAs: errorResult)
                        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                    }
                }
            )
        }
    }

    func setSecretInternal(
        _ command: CDVInvokedUrlCommand,
        context: LAContext
    ) {
        let options = command.arguments[0] as! [String: Any]?

        guard let secretStr = options?["secret"] as! String? else {
            return
        }

        var secret = Secret()
        if let secretName = options?["secretName"] as! String? {
            secret = Secret(keyName: secretName)
        }

        let scope =
            options?["scope"] as! String? ?? "activeSystemLock"
        let lockBehavior =
            options?["lockBehavior"] as! String? ?? "lockAfterUse"

        var pluginResult: CDVPluginResult
        do {
            try? secret.delete(context: context)

            try secret.save(
                secretStr,
                scope: scope,
                lockBehavior: lockBehavior,
                context: context
            )

            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Success")
        } catch {
            var code = PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue
            var message = error.localizedDescription
            if let err = error as? KeychainError {
                code = err.pluginError.rawValue
                message = err.localizedDescription
            }
            let errorResult = ["code": code, "message": message] as [String: Any]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResult)
        }

        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(hasSecret:)
    func hasSecret(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?

        var secret = Secret()
        if let secretName = options?["secretName"] as! String? {
            secret = Secret(keyName: secretName)
        }

        var pluginResult: CDVPluginResult
        do {
            let result = try secret.exists()
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        } catch {
            var code = PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue
            var message = error.localizedDescription
            if let err = error as? KeychainError {
                code = err.pluginError.rawValue
                message = err.localizedDescription
            }
            let errorResult = ["code": code, "message": message] as [String: Any]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResult)
        }

        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(getSecret:)
    func getSecret(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?

        var secret = Secret()
        if let secretName = options?["secretName"] as! String? {
            secret = Secret(keyName: secretName)
        }

        let batch = options?["batch"] as! String?
        var context: LAContext
        if batch == "continue", let previousContext = SystemUnlock.batchContext {
            context = previousContext
        } else {
            context = makeAuthContextForSecret(options: options)
        }
        if batch == "start" {
            SystemUnlock.batchContext = context
        }

        var pluginResult: CDVPluginResult
        do {
            let result = try secret.load(context: context)
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        } catch {
            var code = PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue
            var message = error.localizedDescription
            if let err = error as? KeychainError {
                code = err.pluginError.rawValue
                message = err.localizedDescription
            }
            let errorResult = ["code": code, "message": message] as [String: Any]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResult)
        }

        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(deleteSecret:)
    func deleteSecret(_ command: CDVInvokedUrlCommand) {
        let options = command.arguments[0] as! [String: Any]?

        let lockBehavior =
            options?["lockBehavior"] as! String? ?? "lockAfterUse"

        let batch = options?["batch"] as! String?
        var context: LAContext
        if batch == "continue", let previousContext = SystemUnlock.batchContext {
            context = previousContext
        } else {
            context = makeAuthContextForSecret(options: options)
        }
        if batch == "start" {
            SystemUnlock.batchContext = context
        }

        if lockBehavior == "lockWithDevice" {
            deleteSecretInternal(command, context: context)
        } else {
            var reason = "Unlock this app"
            if let description = options?["description"] as! String? {
                reason = description
            }

            context.evaluatePolicy(
                makeAuthPolicy(lockBehavior: lockBehavior),
                localizedReason: reason,
                reply: { [unowned self] (success, error) -> Void in
                    if success {
                        deleteSecretInternal(command, context: context)
                    } else {
                        var errorResult: [String: Any] = [
                            "code": PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue,
                            "message": error?.localizedDescription ?? "Something went wrong",
                        ]
                        if error != nil {
                            let errorCode = abs(error!._code)
                            if let mappedErrorCode = errorCodeMapping[errorCode] {
                                errorResult = [
                                    "code": mappedErrorCode, "message": error!.localizedDescription,
                                ]
                            }
                        }
                        let pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR, messageAs: errorResult)
                        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                    }
                }
            )
        }
    }

    func deleteSecretInternal(
        _ command: CDVInvokedUrlCommand,
        context: LAContext
    ) {
        let options = command.arguments[0] as! [String: Any]?

        var secret = Secret()
        if let secretName = options?["secretName"] as! String? {
            secret = Secret(keyName: secretName)
        }

        var pluginResult: CDVPluginResult
        do {
            try secret.delete(context: context)
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "Success")
        } catch {
            var code = PluginError.BIOMETRIC_UNKNOWN_ERROR.rawValue
            var message = error.localizedDescription
            if let err = error as? KeychainError {
                code = err.pluginError.rawValue
                message = err.localizedDescription
            }
            let errorResult = ["code": code, "message": message] as [String: Any]
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: errorResult)
        }

        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    override func pluginInitialize() {
        super.pluginInitialize()
    }
}

struct KeychainError: Error {
    var status: OSStatus

    var localizedDescription: String {
        if let result = SecCopyErrorMessageString(status, nil) as String? {
            return result
        }
        switch status {
        case errSecItemNotFound:
            return "Secret not found"
        case errSecUserCanceled:
            return "Biometric dissmissed"
        case errSecAuthFailed:
            return "Authentication failed"
        default:
            return "Something went wrong \(status)"
        }
    }

    var pluginError: PluginError {
        switch status {
        case errSecItemNotFound:
            return PluginError.BIOMETRIC_SECRET_NOT_FOUND
        case errSecUserCanceled:
            return PluginError.BIOMETRIC_DISMISSED
        case errSecAuthFailed:
            return PluginError.BIOMETRIC_AUTHENTICATION_FAILED
        default:
            return PluginError.BIOMETRIC_UNKNOWN_ERROR
        }
    }
}

struct Secret {
    var keyName = "__aio_key"

    private func makeAccessControl(
        scope: String,
        lockBehavior: String
    ) -> SecAccessControl {
        var access: SecAccessControl?

        switch (scope, lockBehavior) {
        case ("activeSystemLock", "lockWithDevice"):
            access = SecAccessControlCreateWithFlags(
                nil,
                kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                [],
                nil)
        case ("activeSystemLock", "lockAfterUse"):
            access = SecAccessControlCreateWithFlags(
                nil,
                kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                .userPresence,
                nil)
        default:
            preconditionFailure(
                "Unsupported SystemUnlock secret behavior (\(scope), \(lockBehavior))")
        }

        precondition(access != nil, "SecAccessControlCreateWithFlags failed")
        return access!
    }

    func save(
        _ secret: String,
        scope: String,
        lockBehavior: String,
        context: LAContext
    ) throws {
        let password = secret.data(using: String.Encoding.utf8)!

        // Build the query for use in the add operation.
        let query =
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: keyName,
                kSecAttrAccessControl: makeAccessControl(
                    scope: scope,
                    lockBehavior: lockBehavior
                ),
                kSecValueData: password,
                kSecUseAuthenticationContext: context,
            ] as CFDictionary

        let status = SecItemAdd(query, nil)

        guard status == errSecSuccess else {
            throw KeychainError(status: status)
        }
    }

    func exists() throws -> Bool {
        let context = LAContext()
        context.interactionNotAllowed = true
        let query =
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: keyName,
                kSecMatchLimit: kSecMatchLimitOne,
                kSecReturnAttributes: true,
                kSecUseAuthenticationContext: context,
            ] as CFDictionary

        var result: AnyObject?
        let status = SecItemCopyMatching(query, &result)

        if status == errSecItemNotFound {
            return false
        }
        if status == errSecInteractionNotAllowed {
            return true
        }

        guard status == errSecSuccess else {
            throw KeychainError(status: status)
        }

        if result as? [String: Any] == nil {
            return false
        }

        return true
    }

    func load(context: LAContext) throws -> String {
        let query =
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: keyName,
                kSecMatchLimit: kSecMatchLimitOne,
                kSecReturnData: true,
                kSecUseAuthenticationContext: context,
            ] as CFDictionary

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query, &result)

        guard status == errSecSuccess else {
            throw KeychainError(status: status)
        }

        guard let passwordData = result as? Data,
            let password = String(data: passwordData, encoding: String.Encoding.utf8)
        else {
            throw KeychainError(status: errSecInternalError)
        }

        return password
    }

    func delete(context: LAContext) throws {
        let query =
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: keyName,
                kSecUseAuthenticationContext: context,
            ] as CFDictionary

        let status = SecItemDelete(query)

        guard status == errSecSuccess else {
            throw KeychainError(status: status)
        }
    }
}

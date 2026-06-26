package com.osimatic.core_android;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class Biometric {

    public interface BiometricCallback {
        void onResult(String status, @Nullable String method);
    }

    public static void authenticate(FragmentActivity activity, String title, String subtitle, BiometricCallback callback) {
        int allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        BiometricManager biometricManager = BiometricManager.from(activity);
        int canAuthenticate = biometricManager.canAuthenticate(allowedAuthenticators);

        if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED) {
            callback.onResult("hardware_missing", null);
            return;
        }
        if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            callback.onResult("not_enrolled", null);
            return;
        }
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            callback.onResult("unavailable", null);
            return;
        }

        String detectedMethod = detectMethod(activity);

        BiometricPrompt prompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            callback.onResult("cancelled", null);
                        } else {
                            callback.onResult("error", null);
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        boolean isDeviceCredential = result.getAuthenticationType() == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL;
                        String status = isDeviceCredential ? "device_credential" : "success";
                        String method = isDeviceCredential ? "device_credential" : detectedMethod;
                        callback.onResult(status, method);
                    }

                    @Override
                    public void onAuthenticationFailed() {}
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build();

        prompt.authenticate(promptInfo);
    }

    private static String detectMethod(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        boolean hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        boolean hasFace = false;
        boolean hasIris = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hasFace = pm.hasSystemFeature(PackageManager.FEATURE_FACE);
            hasIris = pm.hasSystemFeature(PackageManager.FEATURE_IRIS);
        }
        if (hasFingerprint && !hasFace && !hasIris) {
            return "fingerprint";
        }
        if (hasFace && !hasFingerprint && !hasIris) {
            return "face";
        }
        if (hasIris && !hasFingerprint && !hasFace) {
            return "iris";
        }
        return "biometric";
    }
}
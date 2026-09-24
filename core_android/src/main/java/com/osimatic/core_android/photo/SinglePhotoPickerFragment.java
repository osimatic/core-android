package com.osimatic.core_android.photo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.osimatic.core_android.FlashMessage;
import com.osimatic.core_android.Image;
import com.osimatic.core_android.R;

public class SinglePhotoPickerFragment extends Fragment {

    private static final String TAG = "SinglePhotoPickerFragment";

    private static final String ARG_FILEPROVIDER_AUTHORITY = "fileproviderAuthority";
    private static final String ARG_ADD_BUTTON_TEXT = "addButtonText";
    private static final String ARG_NO_PHOTO_TEXT = "noPhotoText";
    private static final String ARG_CAMERA_PERMISSION_DENIED_MESSAGE = "cameraPermissionDeniedMessage";
    private static final String ARG_BUTTON_VISIBLE = "buttonVisible";

    public interface OnPhotoChangedListener {
        void onPhotoChanged(@Nullable Bitmap photo);
    }

    /**
     * Creates a new instance configured with the given texts, with its built-in "take photo" button visible.
     *
     * @param fileproviderAuthority        the {@link androidx.core.content.FileProvider} authority declared in the host app's manifest, used to obtain a full-resolution photo capture {@link Uri}; must not be {@code null}
     * @param addButtonText                the text of the "take photo" button; may be {@code null} to keep the layout's default text
     * @param noPhotoText                  the text shown when no photo has been taken yet; may be {@code null} to keep the layout's default text
     * @param cameraPermissionDeniedMessage the message displayed when the camera permission is denied; may be {@code null}
     * @return a new, configured {@link SinglePhotoPickerFragment}
     */
    public static SinglePhotoPickerFragment newInstance(String fileproviderAuthority, @Nullable String addButtonText, @Nullable String noPhotoText, @Nullable String cameraPermissionDeniedMessage) {
        return newInstance(fileproviderAuthority, addButtonText, noPhotoText, cameraPermissionDeniedMessage, true);
    }

    /**
     * Creates a new instance configured with the given texts.
     *
     * @param fileproviderAuthority        the {@link androidx.core.content.FileProvider} authority declared in the host app's manifest, used to obtain a full-resolution photo capture {@link Uri}; must not be {@code null}
     * @param addButtonText                the text of the "take photo" button; may be {@code null} to keep the layout's default text
     * @param noPhotoText                  the text shown when no photo has been taken yet; may be {@code null} to keep the layout's default text
     * @param cameraPermissionDeniedMessage the message displayed when the camera permission is denied; may be {@code null}
     * @param buttonVisible                {@code false} to hide the built-in "take photo" button, for callers that trigger {@link #takePhoto()} themselves from an external button/dialog
     * @return a new, configured {@link SinglePhotoPickerFragment}
     */
    public static SinglePhotoPickerFragment newInstance(String fileproviderAuthority, @Nullable String addButtonText, @Nullable String noPhotoText, @Nullable String cameraPermissionDeniedMessage, boolean buttonVisible) {
        SinglePhotoPickerFragment fragment = new SinglePhotoPickerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILEPROVIDER_AUTHORITY, fileproviderAuthority);
        args.putString(ARG_ADD_BUTTON_TEXT, addButtonText);
        args.putString(ARG_NO_PHOTO_TEXT, noPhotoText);
        args.putString(ARG_CAMERA_PERMISSION_DENIED_MESSAGE, cameraPermissionDeniedMessage);
        args.putBoolean(ARG_BUTTON_VISIBLE, buttonVisible);
        fragment.setArguments(args);
        return fragment;
    }

    private String fileproviderAuthority;
    private String addButtonText;
    private String noPhotoText;
    private String cameraPermissionDeniedMessage;
    private boolean buttonVisible = true;

    private SinglePhotoPickerView singlePhotoPickerView;
    private Uri currentPhotoUri;
    private OnPhotoChangedListener onPhotoChangedListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (null == args) {
            return;
        }
        fileproviderAuthority = args.getString(ARG_FILEPROVIDER_AUTHORITY);
        addButtonText = args.getString(ARG_ADD_BUTTON_TEXT);
        noPhotoText = args.getString(ARG_NO_PHOTO_TEXT);
        cameraPermissionDeniedMessage = args.getString(ARG_CAMERA_PERMISSION_DENIED_MESSAGE);
        buttonVisible = args.getBoolean(ARG_BUTTON_VISIBLE, true);
    }

    private final ActivityResultLauncher<Intent> photoLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || null == currentPhotoUri) {
                    return;
                }
                Bitmap bitmap = Image.getImageBitmap(new Intent().setData(currentPhotoUri), requireContext().getApplicationContext());
                if (null == bitmap) {
                    Log.d(TAG, "Photo null après capture.");
                    return;
                }
                singlePhotoPickerView.setPhoto(bitmap);
                notifyPhotoChanged(bitmap);
            });

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                takePhoto();
            } else {
                if (null != cameraPermissionDeniedMessage) {
                    FlashMessage.display(requireActivity(), cameraPermissionDeniedMessage);
                }
            }
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_photo_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        singlePhotoPickerView = view.findViewById(R.id.singlePhotoPickerView);
        if (null != addButtonText) {
            singlePhotoPickerView.setButtonText(addButtonText);
        }
        if (null != noPhotoText) {
            singlePhotoPickerView.setNoPhotoText(noPhotoText);
        }
        singlePhotoPickerView.setOnTakePhotoListener(this::takePhoto);
        singlePhotoPickerView.setOnPhotoClickListener(() -> showFullscreen(singlePhotoPickerView.getPhoto()));
        singlePhotoPickerView.setButtonVisible(buttonVisible);
    }

    private void showFullscreen(Bitmap bitmap) {
        if (null == bitmap) {
            return;
        }
        FullscreenPhotoFragment.newInstance(bitmap).show(getChildFragmentManager(), "fullscreen");
    }

    public void setOnPhotoChangedListener(OnPhotoChangedListener listener) {
        this.onPhotoChangedListener = listener;
    }

    @Nullable
    public Bitmap getPhoto() {
        if (null == singlePhotoPickerView) {
            return null;
        }
        return singlePhotoPickerView.getPhoto();
    }

    private void notifyPhotoChanged(@Nullable Bitmap photo) {
        if (null != onPhotoChangedListener) {
            onPhotoChangedListener.onPhotoChanged(photo);
        }
    }

    /**
     * Clears the currently selected photo, for callers that offer another attachment option alongside this fragment (e.g. a document picker) and need to reset the photo state when the other option is chosen.
     */
    public void reset() {
        if (null == singlePhotoPickerView) {
            return;
        }
        singlePhotoPickerView.reset();
        notifyPhotoChanged(null);
    }

    /**
     * Launches the camera to take a photo, requesting the camera permission first if needed.
     *
     * <p>Exposed publicly so callers with {@code buttonVisible=false} can trigger the capture from their own external button/dialog.
     */
    public void takePhoto() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        currentPhotoUri = Image.prepareCameraIntent(requireContext(), intent, fileproviderAuthority, "single_photo_picker");
        if (null != currentPhotoUri) {
            photoLauncher.launch(intent);
        }
    }
}
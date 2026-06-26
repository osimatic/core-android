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

    public interface OnPhotoChangedListener {
        void onPhotoChanged(@Nullable Bitmap photo);
    }

    // Config statique — à initialiser avant l'ajout du fragment
    public static String fileproviderAuthority;
    public static String addButtonText;
    public static String noPhotoText;
    public static String cameraPermissionDeniedMessage;

    private SinglePhotoPickerView singlePhotoPickerView;
    private Uri currentPhotoUri;
    private OnPhotoChangedListener onPhotoChangedListener;

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

    private void takePhoto() {
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
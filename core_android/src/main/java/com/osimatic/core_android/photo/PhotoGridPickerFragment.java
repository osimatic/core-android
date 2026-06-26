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

import java.util.ArrayList;
import java.util.List;

public class PhotoGridPickerFragment extends Fragment {

	private static final String TAG = "PhotoGridPickerFragment";

	public interface OnPhotosChangedListener {
		void onPhotosChanged(List<Bitmap> photos);
	}

	// Config statique — à initialiser avant l'ajout du fragment
	public static String fileproviderAuthority;
	public static int maxPhotos = 5;
	public static String addButtonText;
	public static String counterFormat;
	public static String cameraPermissionDeniedMessage;

	private PhotoGridPickerView photoGridPickerView;
	private Uri currentPhotoUri;
	private OnPhotosChangedListener onPhotosChangedListener;

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
				photoGridPickerView.addPhoto(bitmap);
				notifyPhotosChanged();
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
		return inflater.inflate(R.layout.fragment_photo_grid_picker, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		photoGridPickerView = view.findViewById(R.id.photoGridPickerView);
		photoGridPickerView.setMaxPhotos(maxPhotos);
		if (null != addButtonText) {
			photoGridPickerView.setAddButtonText(addButtonText);
		}
		if (null != counterFormat) {
			photoGridPickerView.setCounterFormat(counterFormat);
		}
		photoGridPickerView.setOnAddClickListener(this::takePhoto);
		photoGridPickerView.setOnRemoveListener(position -> notifyPhotosChanged());
	}

	public void setOnPhotosChangedListener(OnPhotosChangedListener listener) {
		this.onPhotosChangedListener = listener;
	}

	public List<Bitmap> getPhotos() {
		if (null == photoGridPickerView) {
			return new ArrayList<>();
		}
		return photoGridPickerView.getPhotos();
	}

	private void notifyPhotosChanged() {
		if (null != onPhotosChangedListener) {
			onPhotosChangedListener.onPhotosChanged(photoGridPickerView.getPhotos());
		}
	}

	private void takePhoto() {
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			permissionLauncher.launch(Manifest.permission.CAMERA);
			return;
		}
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		currentPhotoUri = Image.prepareCameraIntent(requireContext(), intent, fileproviderAuthority, "photo_grid_picker");
		if (null != currentPhotoUri) {
			photoLauncher.launch(intent);
		}
	}
}
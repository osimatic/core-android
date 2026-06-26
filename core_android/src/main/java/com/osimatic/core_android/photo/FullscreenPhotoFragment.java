package com.osimatic.core_android.photo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.osimatic.core_android.R;

public class FullscreenPhotoFragment extends DialogFragment {

	// référence statique temporaire pour transmettre le bitmap sans passer par Bundle
	private static Bitmap pendingBitmap;

	public static FullscreenPhotoFragment newInstance(Bitmap bitmap) {
		pendingBitmap = bitmap;
		return new FullscreenPhotoFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_fullscreen_photo, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ImageView imageView = view.findViewById(R.id.fullscreenPhotoImageView);
		if (null != pendingBitmap) {
			imageView.setImageBitmap(pendingBitmap);
			pendingBitmap = null;
		}
		view.setOnClickListener(v -> dismiss());
	}
}
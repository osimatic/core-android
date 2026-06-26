package com.osimatic.core_android.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osimatic.core_android.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoGridPickerView extends LinearLayout {

	public interface OnAddClickListener {
		void onAddClick();
	}

	public interface OnRemoveListener {
		void onRemove(int position);
	}

	private int maxPhotos = 5;
	private String counterFormat;

	private final List<Bitmap> photos = new ArrayList<>();
	private PhotoGridAdapter adapter;

	private Button addButton;
	private TextView counterText;

	private OnAddClickListener onAddClickListener;
	private OnRemoveListener onRemoveListener;

	public PhotoGridPickerView(Context context) {
		super(context);
		init(context);
	}

	public PhotoGridPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		setOrientation(VERTICAL);
		LayoutInflater.from(context).inflate(R.layout.view_photo_grid_picker, this, true);

		addButton = findViewById(R.id.photoGridPickerAddButton);
		counterText = findViewById(R.id.photoGridPickerCounterText);

		adapter = new PhotoGridAdapter(photos, position -> {
			photos.remove(position);
			adapter.notifyDataSetChanged();
			if (null != onRemoveListener) {
				onRemoveListener.onRemove(position);
			}
			updateUI();
		});

		RecyclerView recyclerView = findViewById(R.id.photoGridPickerRecyclerView);
		recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
		recyclerView.setAdapter(adapter);

		addButton.setOnClickListener(v -> {
			if (null != onAddClickListener) {
				onAddClickListener.onAddClick();
			}
		});

		updateUI();
	}

	public void addPhoto(Bitmap bitmap) {
		photos.add(bitmap);
		adapter.notifyDataSetChanged();
		updateUI();
	}

	public List<Bitmap> getPhotos() {
		return new ArrayList<>(photos);
	}

	public void setMaxPhotos(int maxPhotos) {
		this.maxPhotos = maxPhotos;
		updateUI();
	}

	public void setAddButtonText(String text) {
		addButton.setText(text);
	}

	public void setCounterFormat(String format) {
		this.counterFormat = format;
		updateUI();
	}

	public void setOnAddClickListener(OnAddClickListener listener) {
		this.onAddClickListener = listener;
	}

	public void setOnRemoveListener(OnRemoveListener listener) {
		this.onRemoveListener = listener;
	}

	public void setOnPhotoClickListener(PhotoGridAdapter.OnPhotoClickListener listener) {
		adapter.setOnPhotoClickListener(listener);
	}

	private void updateUI() {
		if (null != counterFormat) {
			counterText.setText(String.format(counterFormat, photos.size(), maxPhotos));
		}
		addButton.setVisibility(photos.size() < maxPhotos ? View.VISIBLE : View.GONE);
	}
}
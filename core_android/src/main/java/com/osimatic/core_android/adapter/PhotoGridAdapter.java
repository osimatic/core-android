package com.osimatic.core_android.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osimatic.core_android.R;

import java.util.List;

public class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder> {

	public interface OnPhotoDeleteListener {
		void onDelete(int position);
	}

	private final List<Bitmap> photos;
	private final OnPhotoDeleteListener deleteListener;

	public PhotoGridAdapter(List<Bitmap> photos, OnPhotoDeleteListener deleteListener) {
		this.photos = photos;
		this.deleteListener = deleteListener;
	}

	@NonNull
	@Override
	public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_grid, parent, false);
		return new PhotoViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
		holder.imageView.setImageBitmap(photos.get(position));
		holder.deleteButton.setOnClickListener(v -> {
			int pos = holder.getAdapterPosition();
			if (pos != RecyclerView.NO_ID) {
				deleteListener.onDelete(pos);
			}
		});
	}

	@Override
	public int getItemCount() {
		return photos.size();
	}

	public static class PhotoViewHolder extends RecyclerView.ViewHolder {
		final ImageView imageView;
		final ImageButton deleteButton;

		public PhotoViewHolder(@NonNull View itemView) {
			super(itemView);
			imageView = itemView.findViewById(R.id.photoGridItemImageView);
			deleteButton = itemView.findViewById(R.id.photoGridItemDeleteButton);
		}
	}
}
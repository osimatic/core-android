package com.osimatic.core_android.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.osimatic.core_android.R;

public class SinglePhotoPickerView extends LinearLayout {

    public interface OnTakePhotoListener {
        void onTakePhoto();
    }

    public interface OnPhotoClickListener {
        void onPhotoClick();
    }

    private Bitmap photo;
    private ImageView photoImageView;
    private TextView noPhotoLabel;
    private Button takePhotoButton;
    private OnTakePhotoListener onTakePhotoListener;
    private OnPhotoClickListener onPhotoClickListener;

    public SinglePhotoPickerView(Context context) {
        super(context);
        init(context);
    }

    public SinglePhotoPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_single_photo_picker, this, true);

        photoImageView = findViewById(R.id.singlePhotoPickerImageView);
        noPhotoLabel = findViewById(R.id.singlePhotoPickerNoPhotoLabel);
        takePhotoButton = findViewById(R.id.singlePhotoPickerButton);

        takePhotoButton.setOnClickListener(v -> {
            if (null != onTakePhotoListener) {
                onTakePhotoListener.onTakePhoto();
            }
        });
        photoImageView.setOnClickListener(v -> {
            if (null != onPhotoClickListener && null != photo) {
                onPhotoClickListener.onPhotoClick();
            }
        });
    }

    public void setPhoto(Bitmap bitmap) {
        photo = bitmap;
        photoImageView.setImageBitmap(bitmap);
        photoImageView.setVisibility(View.VISIBLE);
        noPhotoLabel.setVisibility(View.GONE);
    }

    @Nullable
    public Bitmap getPhoto() {
        return photo;
    }

    public void setButtonText(String text) {
        takePhotoButton.setText(text);
    }

    public void setNoPhotoText(String text) {
        noPhotoLabel.setText(text);
    }

    public void setOnTakePhotoListener(OnTakePhotoListener listener) {
        this.onTakePhotoListener = listener;
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.onPhotoClickListener = listener;
    }
}
package com.osimatic.core_android.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

public class PhotoView extends AppCompatImageView {

	private Bitmap bitmap;

	public PhotoView(Context context) {
		super(context);
		init();
	}

	public PhotoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		setScaleType(ScaleType.FIT_CENTER);
		setAdjustViewBounds(true);
		// annule le tint blanc appliqué globalement par le thème de l'app
		setImageTintList(null);
		int maxHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, getResources().getDisplayMetrics());
		setMaxHeight(maxHeightPx);
		setVisibility(View.GONE);
		setOnClickListener(v -> {
			if (null != bitmap) {
				Context ctx = getContext();
				if (ctx instanceof FragmentActivity) {
					FullscreenPhotoFragment.newInstance(bitmap)
						.show(((FragmentActivity) ctx).getSupportFragmentManager(), "fullscreen");
				}
			}
		});
	}

	public void setPhoto(@Nullable Bitmap bm) {
		bitmap = bm;
		setImageBitmap(bm);
		setVisibility(null != bm ? View.VISIBLE : View.GONE);
	}
}
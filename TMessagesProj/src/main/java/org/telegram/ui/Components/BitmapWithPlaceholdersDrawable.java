package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import java.util.List;

public class BitmapWithPlaceholdersDrawable extends BitmapDrawable {

    private List<RectF> placeholders;

    public BitmapWithPlaceholdersDrawable(Bitmap bitmap, List<RectF> placeholders) {
        super(bitmap);
        this.placeholders = placeholders;
    }
}

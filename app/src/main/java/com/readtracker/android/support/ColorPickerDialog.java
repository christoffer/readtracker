package com.readtracker.android.support;

import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.readtracker.R;

public class ColorPickerDialog extends Dialog implements SeekBar.OnSeekBarChangeListener {
  private final SeekBar[] mSeekBars = new SeekBar[3];
  private final Listener mListener;
  private final Drawable mBookImageDrawable;


  public ColorPickerDialog(Context context, int initialColor, Listener listener) {
    super(context);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.dialog_color_picker);

    mListener = listener;

    ImageView bookImage = findViewById(R.id.book_image);
    // Use a mutated drawable to avoid affecting all instances of this drawable with the dynamic
    // color changes used in the color picker dialog.
    mBookImageDrawable = bookImage.getDrawable().mutate();

    Button applyButton = findViewById(R.id.apply_color_button);
    applyButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final int color = getColor();
        mListener.onColorSelected(color);
        dismiss();
      }
    });

    mSeekBars[0] = findViewById(R.id.red_seekbar);
    mSeekBars[1] = findViewById(R.id.blue_seekbar);
    mSeekBars[2] = findViewById(R.id.green_seekbar);

    int[] rgbColorValues = new int[]{
        0xffff0000,
        0xff00ff00,
        0xff0000ff,
    };

    for(int i = 0; i < mSeekBars.length; i++) {
      SeekBar seekbar = mSeekBars[i];
      int color = rgbColorValues[i];
      seekbar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
      seekbar.setOnSeekBarChangeListener(this);
    }

    setColor(initialColor);
    updateCurrentColor();
  }

  @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    updateCurrentColor();
  }

  @Override public void onStartTrackingTouch(SeekBar seekBar) {
    // NOOP
  }

  @Override public void onStopTrackingTouch(SeekBar seekBar) {
    // NOOP
  }

  private void updateCurrentColor() {
    final int color = getColor();
    mBookImageDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
  }

  private int getColor() {
    int red = mSeekBars[0].getProgress();
    int green  = mSeekBars[1].getProgress();
    int blue = mSeekBars[2].getProgress();

    red = (red << 16) & 0x00ff0000;
    green = (green << 8) & 0x0000ff00;
    blue = blue & 0x000000ff;

    return 0xff000000 | red | green | blue;
  }

  private void setColor(int color) {
    int red = (color & 0x00ff0000) >> 16;
    int green  = (color & 0x0000ff00) >> 8;
    int blue = color & 0x000000ff;

    mSeekBars[0].setProgress(red);
    mSeekBars[1].setProgress(green);
    mSeekBars[2].setProgress(blue);
  }

  public interface Listener {
    void onColorSelected(int color);
  }
}

/* Copyright 2013 Alexander Osmanov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.evilduck.piano.views.instrument;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.OverScroller;

import com.evilduck.piano.R;
import com.evilduck.piano.music.Note;

public class PianoView extends View {

    private int xOffset = 0;

    private OverScroller scroller;

    private GestureDetector gestureDetector;

    private ScaleGestureDetector scaleGestureDetector;

    private int canvasWidth;

    private int instrumentWidth;

    private Keyboard instrument;

    private OnKeyTouchListener onTouchListener;

    private EdgeEffectCompat leftEdgeEffect;

    private EdgeEffectCompat rightEdgeEffect;

    private int flingDirection = 0;

    private float scaleX = 1.0f;

    private boolean isScaling = false;

    private ArrayList<Note> notesToDraw = new ArrayList<Note>();

    public PianoView(Context context, AttributeSet attrs) {
	super(context, attrs);

	init();

	leftEdgeEffect = new EdgeEffectCompat(getContext());
	rightEdgeEffect = new EdgeEffectCompat(getContext());

	setVerticalScrollBarEnabled(false);
	setHorizontalScrollBarEnabled(true);

	TypedArray a = context.obtainStyledAttributes(R.styleable.View);
	initializeScrollbars(a);
	a.recycle();

	TypedArray pianoAttrs = context.obtainStyledAttributes(attrs, R.styleable.PianoView);

	boolean asBitmaps;
	int circleColor;
	float circleRadius;
	float circleTextSize;
	try {
	    asBitmaps = pianoAttrs.getBoolean(R.styleable.PianoView_overlay_bitmaps, true);
	    circleColor = pianoAttrs.getInteger(R.styleable.PianoView_overlay_color, Color.GREEN);
	    circleRadius = pianoAttrs.getDimension(R.styleable.PianoView_overlay_circle_radius, TypedValue
		    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics()));
	    circleTextSize = pianoAttrs.getDimension(R.styleable.PianoView_overlay_circle_text_size, TypedValue
		    .applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, context.getResources().getDisplayMetrics()));
	} finally {
	    pianoAttrs.recycle();
	}

	instrument = new Keyboard(getContext(), asBitmaps, circleColor, circleRadius, circleTextSize);
    }

    public void addNotes(List<Note> notes) {
	notesToDraw.addAll(notes);

	invalidate();
    }

    public void removeNotes(List<Note> notes) {
	notesToDraw.removeAll(notes);

	invalidate();
    }

    public void clear() {
	notesToDraw.clear();

	invalidate();
    }

    private void init() {
	if (!isInEditMode()) {
	    scroller = new OverScroller(getContext());
	    gestureDetector = new GestureDetector(getContext(), gestureListener);
	    scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleGestureListener);
	}
    }

    public void smoothScrollXTo(int x) {
	scroller.startScroll(xOffset, 0, x - xOffset, 0);
    }

    // ========== preserving scroll position during screen rotations

    @Override
    protected Parcelable onSaveInstanceState() {
	SavedState st = new SavedState(super.onSaveInstanceState());

	st.xOffset = xOffset;
	return st;
    }

    protected void onRestoreInstanceState(Parcelable state) {
	if (!(state instanceof SavedState)) {
	    super.onRestoreInstanceState(state);
	    return;
	}

	SavedState ss = (SavedState) state;
	super.onRestoreInstanceState(ss.getSuperState());

	xOffset = ss.xOffset;
    };

    public static class SavedState extends BaseSavedState {

	int xOffset;

	SavedState(Parcelable superState) {
	    super(superState);
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
	    super.writeToParcel(out, flags);
	    out.writeInt(xOffset);
	}

	public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
	    public SavedState createFromParcel(Parcel in) {
		return new SavedState(in);
	    }

	    public SavedState[] newArray(int size) {
		return new SavedState[size];
	    }
	};

	private SavedState(Parcel in) {
	    super(in);
	    xOffset = in.readInt();
	}

    }

    // ==========

    @Override
    public void draw(Canvas canvas) {
	long start = System.currentTimeMillis();

	super.draw(canvas);
	boolean needsInvalidate = false;

	final int overScrollMode = ViewCompat.getOverScrollMode(this);
	if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS
		|| (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS)) {
	    if (!leftEdgeEffect.isFinished()) {
		final int restoreCount = canvas.save();
		final int height = getHeight() - getPaddingTop() - getPaddingBottom();
		final int width = getWidth();

		canvas.rotate(270);
		canvas.translate(-height + getPaddingTop(), 0);
		leftEdgeEffect.setSize(height, width);
		needsInvalidate |= leftEdgeEffect.draw(canvas);
		canvas.restoreToCount(restoreCount);
	    }
	    if (!rightEdgeEffect.isFinished()) {
		final int restoreCount = canvas.save();
		final int width = getWidth();
		final int height = getHeight() - getPaddingTop() - getPaddingBottom();

		canvas.rotate(90);
		canvas.translate(-getPaddingTop(), -width);
		rightEdgeEffect.setSize(height, width);
		needsInvalidate |= rightEdgeEffect.draw(canvas);
		canvas.restoreToCount(restoreCount);
	    }
	} else {
	    leftEdgeEffect.finish();
	    rightEdgeEffect.finish();
	}

	long end = System.currentTimeMillis();
	Log.d(VIEW_LOG_TAG, "Drawing took: " + (end - start));

	if (needsInvalidate) {
	    ViewCompat.postInvalidateOnAnimation(this);
	}
    }

    @Override
    protected void onDraw(Canvas canvas) {
	if (isInEditMode()) {
	    canvas.drawColor(Color.GRAY);
	    return;
	}

	if (scroller.computeScrollOffset()) {
	    xOffset = scroller.getCurrX();
	}

	if (scroller.isOverScrolled()) {
	    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
		absorbFling();
	    }
	}

	if (!instrument.isInitialized()) {
	    instrument.initializeInstrument(getMeasuredHeight(), getContext());
	    instrumentWidth = instrument.getWidth();
	}

	int localXOffset = getOffsetInsideOfBounds();

	canvas.save();
	canvas.scale(scaleX, 1.0f);
	canvas.translate(-localXOffset, 0);

	instrument.updateBounds(localXOffset, canvasWidth + localXOffset);
	instrument.draw(canvas);

	if (!notesToDraw.isEmpty()) {
	    instrument.drawOverlays(notesToDraw, canvas);
	}

	canvas.restore();

	if (!scroller.isFinished()) {
	    ViewCompat.postInvalidateOnAnimation(this);
	}
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void absorbFling() {
	if (flingDirection > 0) {
	    leftEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
	} else {
	    rightEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
	}
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected int computeHorizontalScrollExtent() {
	return canvasWidth;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
	return getOffsetInsideOfBounds();
    }

    @Override
    protected int computeHorizontalScrollRange() {
	return instrumentWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	int action = event.getAction();
	if (action == MotionEvent.ACTION_CANCEL) {
	    resetTouchFeedback();

	    leftEdgeEffect.onRelease();
	    rightEdgeEffect.onRelease();
	    ViewCompat.postInvalidateOnAnimation(this);
	}
	if (action == MotionEvent.ACTION_UP) {
	    if (xOffset < 0) {
		xOffset = 0;
	    }
	    if (xOffset > instrumentWidth - getMeasuredWidth()) {
		xOffset = instrumentWidth - getMeasuredWidth();
	    }

	    leftEdgeEffect.onRelease();
	    rightEdgeEffect.onRelease();
	    ViewCompat.postInvalidateOnAnimation(this);
	}

	boolean result = super.onTouchEvent(event);
	result |= scaleGestureDetector.onTouchEvent(event);
	if (!isScaling) {
	    result |= gestureDetector.onTouchEvent(event);
	}

	return result;
    }

    private void fireTouchListeners(int code) {
	if (onTouchListener != null) {
	    onTouchListener.onTouch(code);
	}
    }

    private void fireLongTouchListeners(int code) {
	if (onTouchListener != null) {
	    onTouchListener.onLongTouch(code);
	}
    }

    public void setOnKeyTouchListener(OnKeyTouchListener listener) {
	this.onTouchListener = listener;
    }

    public interface OnKeyTouchListener {

	void onTouch(int midiCode);

	void onLongTouch(int midiCode);

    }

    private OnScaleGestureListener scaleGestureListener = new OnScaleGestureListener() {

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	    isScaling = false;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
	    isScaling = true;
	    return true;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
	    Log.d(VIEW_LOG_TAG, "Scale: " + detector.getScaleFactor());

	    scaleX *= detector.getScaleFactor();
	    if (scaleX < 1) {
		scaleX = 1;
	    }
	    if (scaleX > 2) {
		scaleX = 2;
	    }
	    invalidate();
	    return true;
	}
    };

    private OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

	public boolean onDown(MotionEvent e) {
	    scroller.forceFinished(true);
	    if (instrument.touchItem(e.getX() / scaleX + xOffset, e.getY())) {
		invalidate();
	    }

	    return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	    flingDirection = velocityX > 0 ? 1 : -1;
	    scroller.fling(xOffset, 0, (int) -velocityX, 0, 0, instrumentWidth - getMeasuredWidth(), 0, 0);

	    if (!awakenScrollBars()) {
		invalidate();
	    }
	    return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
	    resetTouchFeedback();
	    xOffset += distanceX;

	    if (xOffset < 0) {
		leftEdgeEffect.onPull(distanceX / (float) getMeasuredWidth());
	    }
	    if (xOffset > instrumentWidth - getMeasuredWidth()) {
		rightEdgeEffect.onPull(distanceX / (float) getMeasuredWidth());
	    }

	    if (!awakenScrollBars()) {
		ViewCompat.postInvalidateOnAnimation(PianoView.this);
	    }

	    return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {
	    fireTouchListeners(instrument.getTouchedCode());

	    resetTouchFeedback();
	    return super.onSingleTapUp(e);
	}

	public void onLongPress(MotionEvent e) {
	    fireLongTouchListeners(instrument.getTouchedCode());

	    super.onLongPress(e);
	    resetTouchFeedback();
	};

	public boolean onDoubleTapEvent(MotionEvent e) {
	    resetTouchFeedback();
	    return super.onDoubleTapEvent(e);
	};

    };

    private void resetTouchFeedback() {
	if (instrument.releaseTouch()) {
	    invalidate();
	}
    };

    private int getOffsetInsideOfBounds() {
	int localxOffset = xOffset;
	if (localxOffset < 0) {
	    localxOffset = 0;
	}
	if (localxOffset > instrumentWidth - getMeasuredWidth()) {
	    localxOffset = instrumentWidth - getMeasuredWidth();
	}
	return localxOffset;
    }

}

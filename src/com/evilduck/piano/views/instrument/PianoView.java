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

    private Keyboard keyboard;

    private OnKeyTouchListener onTouchListener;

    private EdgeEffectCompat leftEdgeEffect;

    private EdgeEffectCompat rightEdgeEffect;

    private float scaleX = 1.0f;

    private ArrayList<Note> notesToDraw = new ArrayList<Note>();

    private boolean measurementChanged = false;

    private int scrollDirection;

    private boolean leftEdgeEffectActive = false;

    private boolean rightEdgeEffectActive = false;

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
	    circleColor = pianoAttrs.getColor(R.styleable.PianoView_overlay_color, Color.GREEN);
	    circleRadius = pianoAttrs.getDimension(R.styleable.PianoView_overlay_circle_radius, TypedValue
		    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics()));
	    circleTextSize = pianoAttrs.getDimension(R.styleable.PianoView_overlay_circle_text_size, TypedValue
		    .applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, context.getResources().getDisplayMetrics()));
	} finally {
	    pianoAttrs.recycle();
	}

	keyboard = new Keyboard(getContext(), asBitmaps, circleColor, circleRadius, circleTextSize);
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
	st.instrumentWidth = instrumentWidth;
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
	instrumentWidth = ss.instrumentWidth;
    };

    public static class SavedState extends BaseSavedState {

	int xOffset;
	int instrumentWidth;

	SavedState(Parcelable superState) {
	    super(superState);
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
	    super.writeToParcel(out, flags);
	    out.writeInt(xOffset);
	    out.writeInt(instrumentWidth);
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
	    instrumentWidth = in.readInt();
	}

    }

    // ==========

    @Override
    public void computeScroll() {
	super.computeScroll();

	boolean needsInvalidate = false;
	if (scroller.computeScrollOffset()) {
	    xOffset = scroller.getCurrX();

	    if (scroller.isOverScrolled()) {
		if (xOffset > 0 && scrollDirection > 0 && !leftEdgeEffectActive) {
		    leftEdgeEffect.onAbsorb(getCurrentVelocity());
		    leftEdgeEffectActive = true;
		    needsInvalidate = true;
		} else if (xOffset < instrumentWidth - getMeasuredWidth() && scrollDirection < 0
			&& !rightEdgeEffectActive) {
		    rightEdgeEffect.onAbsorb(getCurrentVelocity());
		    needsInvalidate = true;
		}
	    }
	}

	if (!scroller.isFinished()) {
	    needsInvalidate = true;
	}

	if (needsInvalidate) {
	    ViewCompat.postInvalidateOnAnimation(this);
	}
    }

    @Override
    public void draw(Canvas canvas) {
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

	if (measurementChanged) {
	    measurementChanged = false;
	    keyboard.initializeInstrument(getMeasuredHeight(), getContext());

	    float oldInstrumentWidth = instrumentWidth;
	    instrumentWidth = keyboard.getWidth();

	    float ratio = (float) instrumentWidth / oldInstrumentWidth;
	    xOffset = (int) (xOffset * ratio);
	}

	int localXOffset = getOffsetInsideOfBounds();

	canvas.save();
	canvas.scale(scaleX, 1.0f);
	canvas.translate(-localXOffset, 0);

	keyboard.updateBounds(localXOffset, canvasWidth + localXOffset);
	keyboard.draw(canvas);

	if (!notesToDraw.isEmpty()) {
	    keyboard.drawOverlays(notesToDraw, canvas);
	}

	canvas.restore();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private int getCurrentVelocity() {
	if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	    return (int) scroller.getCurrVelocity();
	}
	return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
	measurementChanged = true;
	Log.d(VIEW_LOG_TAG, "measurement changed");

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
	    releaseEdgeEffects();
	    xOffset = getOffsetInsideOfBounds();
	    ViewCompat.postInvalidateOnAnimation(this);
	}
	if (action == MotionEvent.ACTION_UP) {
	    xOffset = getOffsetInsideOfBounds();
	    releaseEdgeEffects();
	    ViewCompat.postInvalidateOnAnimation(this);
	}

	boolean retVal = scaleGestureDetector.onTouchEvent(event);
	retVal = gestureDetector.onTouchEvent(event) || retVal;
	return retVal || super.onTouchEvent(event);
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
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
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
	    ViewCompat.postInvalidateOnAnimation(PianoView.this);
	    return true;
	}
    };

    private void releaseEdgeEffects() {
	leftEdgeEffectActive = rightEdgeEffectActive = false;
	leftEdgeEffect.onRelease();
	rightEdgeEffect.onRelease();
    }

    private OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

	public boolean onDown(MotionEvent e) {
	    releaseEdgeEffects();
	    scroller.forceFinished(true);
	    if (keyboard.touchItem(e.getX() / scaleX + xOffset, e.getY())) {
		invalidate();
	    }

	    return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	    releaseEdgeEffects();
	    scrollDirection = velocityX > 0 ? 1 : -1;

	    scroller.fling(xOffset, 0, (int) -velocityX, 0, 0, instrumentWidth - getMeasuredWidth(), 0, 0);

	    if (!awakenScrollBars()) {
		ViewCompat.postInvalidateOnAnimation(PianoView.this);
	    }
	    return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
	    resetTouchFeedback();
	    xOffset += distanceX;

	    if (xOffset < 0) {
		leftEdgeEffect.onPull(distanceX / (float) getMeasuredWidth());
		leftEdgeEffectActive = true;
	    }
	    if (xOffset > instrumentWidth - getMeasuredWidth()) {
		rightEdgeEffect.onPull(distanceX / (float) getMeasuredWidth());
		rightEdgeEffectActive = true;
	    }

	    if (!awakenScrollBars()) {
		invalidate();
	    }

	    return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {
	    fireTouchListeners(keyboard.getTouchedCode());

	    resetTouchFeedback();
	    return super.onSingleTapUp(e);
	}

	public void onLongPress(MotionEvent e) {
	    fireLongTouchListeners(keyboard.getTouchedCode());

	    super.onLongPress(e);
	    resetTouchFeedback();
	};

	public boolean onDoubleTapEvent(MotionEvent e) {
	    resetTouchFeedback();
	    return super.onDoubleTapEvent(e);
	};

    };

    private void resetTouchFeedback() {
	if (keyboard.releaseTouch()) {
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

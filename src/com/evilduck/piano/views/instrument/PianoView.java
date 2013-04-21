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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.widget.OverScroller;

import com.evilduck.piano.R;
import com.evilduck.piano.music.Note;

@TargetApi(17)
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

    private AccessibilityManager mAccessibilityManager;

    private VirtualDescendantsProvider mAccessibilityNodeProvider;

    private int currentHoveredKey = -1;

    private Key mLastHoveredChild;

    public PianoView(Context context, AttributeSet attrs) {
	super(context, attrs);

	init();

	mAccessibilityManager = (AccessibilityManager) context.getSystemService(Service.ACCESSIBILITY_SERVICE);

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

	setFocusable(true);
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

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
	if (mAccessibilityNodeProvider == null) {
	    mAccessibilityNodeProvider = new VirtualDescendantsProvider();
	}
	return mAccessibilityNodeProvider;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
	super.onInitializeAccessibilityEvent(event);

	if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
	    event.setClassName(PianoView.class.toString());
	    event.setSource(PianoView.this);

	    event.setEventTime(System.currentTimeMillis());
	    event.setScrollX(xOffset);
	    event.setMaxScrollX(instrumentWidth - getMeasuredWidth());
	    event.setScrollable(true);
	    event.setItemCount(keyboard.getKeysArray().length);
	    event.setFromIndex(keyboard.getFirstVisibleKey());
	    event.setToIndex(keyboard.getFirstVisibleKey());

	    event.setContentDescription("Scrolling bitch");
	} else {

	    if (currentHoveredKey == -1) {
		event.setClassName(PianoView.class.toString());
		event.setSource(PianoView.this);
	    } else {
		event.setClassName(Key.class.toString());
		event.setSource(PianoView.this, currentHoveredKey);
	    }
	}

	Log.d(VIEW_LOG_TAG, "onInitializeAccessibilityEvent" + event);
    }

    private class VirtualDescendantsProvider extends AccessibilityNodeProvider {

	@Override
	public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
	    AccessibilityNodeInfo info = null;
	    if (virtualViewId == View.NO_ID) {
		info = AccessibilityNodeInfo.obtain(PianoView.this);

		onInitializeAccessibilityNodeInfo(info);
		info.setScrollable(true);
		info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
		info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);

		info.setText("Piano Keyboard");
		Key[] children = keyboard.getKeysArray();
		for (Key key : children) {
		    info.addChild(PianoView.this, key.midiCode - Keyboard.START_MIDI_CODE);
		}
	    } else {
		Key virtualView = keyboard.getKeysArray()[virtualViewId];
		if (virtualView == null) {
		    return null;
		}

		info = AccessibilityNodeInfo.obtain(PianoView.this, virtualViewId);
		onInitializeAccessibilityNodeInfo(info);

		info.setClassName(virtualView.getClass().getName());
		info.setParent(PianoView.this);

		info.setFocusable(true);
		info.setClickable(true);

		int[] location = new int[2];
		getLocationOnScreen(location);

		info.setBoundsInParent(new Rect((int) virtualView.startX + xOffset, (int) virtualView.startY,
			(int) virtualView.endX + xOffset, (int) virtualView.endY));

		info.setBoundsInScreen(new Rect((int) virtualView.startX + location[0] + xOffset,
			(int) virtualView.startY + location[1], (int) virtualView.endX + location[0] + xOffset,
			(int) virtualView.endY + location[1]));

		Note note = Note.fromCode(virtualViewId);

		info.setText(note.getAudibleName());
		info.setContentDescription("Piano key " + note.getAudibleName());
	    }
	    return info;
	}

	@Override
	public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String searched, int virtualViewId) {
	    if (TextUtils.isEmpty(searched)) {
		return Collections.emptyList();
	    }
	    String searchedLowerCase = searched.toLowerCase(Locale.getDefault());
	    List<AccessibilityNodeInfo> result = null;
	    if (virtualViewId == View.NO_ID) {
		List<Key> children = Arrays.asList(keyboard.getKeysArray());
		final int childCount = children.size();
		for (int i = 0; i < childCount; i++) {
		    Key child = children.get(i);
		    String textToLowerCase = ("Midi code: " + child.midiCode).toLowerCase(Locale.getDefault());
		    if (textToLowerCase.contains(searchedLowerCase)) {
			if (result == null) {
			    result = new ArrayList<AccessibilityNodeInfo>();
			}
			result.add(createAccessibilityNodeInfo(child.midiCode));
		    }
		}
	    } else {
		Key virtualView = keyboard.getKeysArray()[virtualViewId];
		if (virtualView != null) {
		    String textToLowerCase = ("Midi code: " + virtualView.midiCode).toLowerCase(Locale.getDefault());
		    if (textToLowerCase.contains(searchedLowerCase)) {
			result = new ArrayList<AccessibilityNodeInfo>();
			result.add(createAccessibilityNodeInfo(virtualViewId));
		    }
		}
	    }
	    if (result == null) {
		return Collections.emptyList();
	    }
	    return result;
	}

	@Override
	public boolean performAction(int virtualViewId, int action, Bundle arguments) {
	    currentHoveredKey = virtualViewId;
	    if (virtualViewId == View.NO_ID) {
		return performAccessibilityAction(action, arguments);
	    } else {
		Key child = keyboard.getKeysArray()[virtualViewId];
		if (child == null) {
		    return false;
		}
		switch (action) {
		case AccessibilityNodeInfo.ACTION_SELECT:
		    setVirtualViewSelected(child, true);
		    invalidate();
		    return true;
		case AccessibilityNodeInfo.ACTION_CLEAR_SELECTION:
		    setVirtualViewSelected(child, false);
		    invalidate();
		    return true;
		case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS: {
		    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
		    invalidate();
		    return true;
		}
		case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS: {
		    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
		    invalidate();
		    return true;
		}
		}
	    }
	    return false;
	}
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
	// This implementation assumes that the virtual children
	// cannot overlap and are always visible. Do NOT use this
	// code as a reference of how to implement hover event
	// dispatch. Instead, refer to ViewGroup#dispatchHoverEvent.
	boolean handled = false;
	List<Key> children = Arrays.asList(keyboard.getKeysArray());
	final int childCount = children.size();
	for (int i = 0; i < childCount; i++) {
	    Key child = children.get(i);
	    Rect childBounds = new Rect((int) child.startX, (int) child.startY, (int) child.endX, (int) child.endY);
	    final int childCoordsX = (int) event.getX() + getScrollX();
	    final int childCoordsY = (int) event.getY() + getScrollY();
	    if (!childBounds.contains(childCoordsX, childCoordsY)) {
		continue;
	    }
	    final int action = event.getAction();
	    switch (action) {
	    case MotionEvent.ACTION_HOVER_ENTER: {
		mLastHoveredChild = child;
		handled |= onHoverVirtualView(child, event);
		event.setAction(action);
	    }
		break;
	    case MotionEvent.ACTION_HOVER_MOVE: {
		if (child == mLastHoveredChild) {
		    handled |= onHoverVirtualView(child, event);
		    event.setAction(action);
		} else {
		    MotionEvent eventNoHistory = event.getHistorySize() > 0 ? MotionEvent.obtainNoHistory(event)
			    : event;
		    eventNoHistory.setAction(MotionEvent.ACTION_HOVER_EXIT);
		    onHoverVirtualView(mLastHoveredChild, eventNoHistory);
		    eventNoHistory.setAction(MotionEvent.ACTION_HOVER_ENTER);
		    onHoverVirtualView(child, eventNoHistory);
		    mLastHoveredChild = child;
		    eventNoHistory.setAction(MotionEvent.ACTION_HOVER_MOVE);
		    handled |= onHoverVirtualView(child, eventNoHistory);
		    if (eventNoHistory != event) {
			eventNoHistory.recycle();
		    } else {
			event.setAction(action);
		    }
		}
	    }
		break;
	    case MotionEvent.ACTION_HOVER_EXIT: {
		mLastHoveredChild = null;
		handled |= onHoverVirtualView(child, event);
		event.setAction(action);
	    }
		break;
	    }
	}
	if (!handled) {
	    handled |= onHoverEvent(event);
	}
	return handled;
    }

    private boolean onHoverVirtualView(Key virtualView, MotionEvent event) {
	final int action = event.getAction();
	switch (action) {
	case MotionEvent.ACTION_HOVER_ENTER: {
	    sendAccessibilityEventForVirtualView(virtualView, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
	}
	    break;
	case MotionEvent.ACTION_HOVER_EXIT: {
	    sendAccessibilityEventForVirtualView(virtualView, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
	}
	    break;
	}
	return true;
    }

    /**
     * Sends a properly initialized accessibility event for a virtual view..
     * 
     * @param virtualView
     *            The virtual view.
     * @param eventType
     *            The type of the event to send.
     */
    private void sendAccessibilityEventForVirtualView(Key virtualView, int eventType) {
	if (mAccessibilityManager.isTouchExplorationEnabled()) {
	    AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
	    event.setPackageName(getContext().getPackageName());
	    event.setClassName(virtualView.getClass().getName());
	    event.setSource(PianoView.this, virtualView.midiCode - Keyboard.START_MIDI_CODE);

	    getParent().requestSendAccessibilityEvent(PianoView.this, event);
	}
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
	super.sendAccessibilityEvent(eventType);
    }

    private void setVirtualViewSelected(Key virtualView, boolean selected) {
	virtualView.pressed = selected;
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
	measurementChanged = true;
	Log.d(VIEW_LOG_TAG, "measurement changed");

	super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	if (measurementChanged) {
	    measurementChanged = false;
	    keyboard.initializeInstrument(getMeasuredHeight(), getContext());

	    float oldInstrumentWidth = instrumentWidth;
	    instrumentWidth = keyboard.getWidth();

	    float ratio = (float) instrumentWidth / oldInstrumentWidth;
	    xOffset = (int) (xOffset * ratio);
	}
    }

    @Override
    protected void onDraw(Canvas canvas) {
	if (isInEditMode()) {
	    canvas.drawColor(Color.GRAY);
	    return;
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

	    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED);

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
	    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);

	    resetTouchFeedback();
	    return super.onSingleTapUp(e);
	}

	public void onLongPress(MotionEvent e) {
	    fireLongTouchListeners(keyboard.getTouchedCode());
	    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);

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

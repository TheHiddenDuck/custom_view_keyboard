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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import com.evilduck.piano.R;
import com.evilduck.piano.music.Note;

class Keyboard {

    private static final float BLACK_KEY_HEIGHT_PERCENT = 1.57f;

    private static final float WHITE_KEY_ASPECT_RATIO = 6.12f;

    private static final int OCTAVES = 4;

    static final int START_MIDI_CODE = Note.C.inOctave(2).getMidiCode();

    private static final int KEYS_IN_OCTAVE = 12;

    private static final int[] WHITE_INDICES = { 0, 2, 4, 5, 7, 9, 11 };

    private static final int[] BLACK_INDICES = { 1, 3, 6, 8, 10 };

    protected static final int NOT_FOUND = -1;

    protected Paint overlayTextPaint;

    protected float overlayCircleRadius;

    protected Rect bounds = new Rect();

    protected int screenLeft;

    protected int screenRight;

    private Key[] keysArray;

    private int octaveWidth;

    private int blackKeyHeight;

    private int whiteKeyWidth;

    private Drawable whiteKeyDrawable;

    private Drawable blackKeyDrawable;

    private int touchedKey;

    private int circleColor;

    private boolean asBitmaps;

    private Bitmap circleAsBitmap;

    private Bitmap notesAtlas;

    private float noteSizeInAtlas;

    private float signSizeInAtlas;

    private Rect src;

    private RectF dst;

    public Key[] getKeysArray() {
	return keysArray;
    }

    public int getTouchedCode() {
	return touchedKey + START_MIDI_CODE;
    }

    public Keyboard(Context context, boolean asBitmaps, int circleColor, float circleRadius, float circleTextSize) {
	this.circleColor = circleColor;
	this.asBitmaps = asBitmaps;

	overlayTextPaint = new Paint();
	overlayTextPaint.setColor(Color.BLACK);
	overlayTextPaint.setTextSize(circleTextSize);

	overlayCircleRadius = circleRadius;

	src = new Rect();
	dst = new RectF();

	whiteKeyDrawable = context.getResources().getDrawable(R.drawable.white_key_selector);
	blackKeyDrawable = context.getResources().getDrawable(R.drawable.black_key_selector);

	if (asBitmaps) {
	    circleAsBitmap = Bitmap.createBitmap((int) circleRadius * 2, (int) circleRadius * 2, Config.ARGB_8888);

	    Canvas c = new Canvas(circleAsBitmap);
	    overlayTextPaint.setColor(circleColor);
	    overlayTextPaint.setAntiAlias(true);
	    c.drawCircle(circleRadius, circleRadius, circleRadius, overlayTextPaint);

	    notesAtlas = BitmapFactory.decodeResource(context.getResources(), R.drawable.note_atlas);
	    noteSizeInAtlas = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13.5f, context.getResources()
		    .getDisplayMetrics());
	    signSizeInAtlas = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.getResources()
		    .getDisplayMetrics());
	}
    }

    public void updateBounds(int left, int right) {
	screenLeft = left;
	screenRight = right;
    }

    public void draw(Canvas canvas) {
	final Key[] keys = keysArray;

	int firstVisibleKey = getFirstVisibleKey();
	int lastVisibleKey = getLastVisibleKey();

	for (int i = 0; i < Keyboard.OCTAVES; i++) {
	    for (int j = 0; j < Keyboard.WHITE_INDICES.length; j++) {
		drawSingleKey(canvas, keys[i * Keyboard.KEYS_IN_OCTAVE + Keyboard.WHITE_INDICES[j]], firstVisibleKey,
			lastVisibleKey);
	    }
	    for (int j = 0; j < Keyboard.BLACK_INDICES.length; j++) {
		drawSingleKey(canvas, keys[i * Keyboard.KEYS_IN_OCTAVE + Keyboard.BLACK_INDICES[j]], firstVisibleKey,
			lastVisibleKey);
	    }
	}
    }

    private void drawSingleKey(Canvas canvas, Key key, int firstVisibleKey, int lastVisibleKey) {
	if (key.midiCode < firstVisibleKey || key.midiCode > lastVisibleKey) {
	    return;
	}

	Drawable drawable = key.black ? blackKeyDrawable : whiteKeyDrawable;
	drawable.setState(new int[] { key.pressed ? android.R.attr.state_pressed : -android.R.attr.state_pressed });
	drawable.setBounds((int) key.startX, (int) key.startY, (int) key.endX, (int) key.endY);
	drawable.draw(canvas);
    }

    public int getWidth() {
	return octaveWidth * Keyboard.OCTAVES;
    }

    public void drawOverlays(ArrayList<Note> notes, Canvas canvas) {
	int firstVisibleKey = getFirstVisibleKey();
	int lastVisibleKey = getLastVisibleKey();

	for (Note note : notes) {
	    int midiCode = note.getMidiCode();
	    if (midiCode >= firstVisibleKey && midiCode <= lastVisibleKey) {
		drawNoteFromMidi(canvas, note, midiCode, false);
	    }
	}
    }

    private void drawNoteFromMidi(Canvas canvas, Note note, int midiCode, boolean replica) {
	Key key = keysArray[midiCode - Keyboard.START_MIDI_CODE];
	overlayTextPaint.setColor(circleColor);
	if (asBitmaps) {
	    drawNoteAsBitmap(canvas, note, key);
	} else {
	    drawNoteAsText(canvas, note, key);
	}
    }

    private void drawNoteAsText(Canvas canvas, Note note, Key key) {
	canvas.drawCircle(key.getOverlayPivotX(), key.getOverlayPivotY(), overlayCircleRadius, overlayTextPaint);

	String name = note.toString();
	overlayTextPaint.getTextBounds(name, 0, name.length(), bounds);
	int width = bounds.right - bounds.left;
	int height = bounds.bottom - bounds.top;

	overlayTextPaint.setColor(Color.BLACK);
	canvas.drawText(name, key.getOverlayPivotX() - width / 2, key.getOverlayPivotY() + height / 2, overlayTextPaint);
    }

    private void drawNoteAsBitmap(Canvas canvas, Note note, Key key) {
	canvas.drawBitmap(circleAsBitmap, key.getOverlayPivotX() - overlayCircleRadius, key.getOverlayPivotY()
		- overlayCircleRadius, null);
	int height = notesAtlas.getHeight();

	setupSourceRect(height, 0, noteSizeInAtlas, note.getNote());

	dst.left = key.getOverlayPivotX() - noteSizeInAtlas / 2;
	dst.right = key.getOverlayPivotX() + noteSizeInAtlas / 2;
	dst.top = key.getOverlayPivotY() - height / 2;
	dst.bottom = key.getOverlayPivotY() + height / 2;
	if (note.getModifier() != Note.MODIFIER_NONE) {
	    dst.left -= signSizeInAtlas / 2;
	    dst.right -= signSizeInAtlas / 2;
	}
	canvas.drawBitmap(notesAtlas, src, dst, null);

	if (note.getModifier() != Note.MODIFIER_NONE) {
	    setupSourceRect(height, noteSizeInAtlas * 7, signSizeInAtlas, note.getModifier() == Note.SHARP ? 0 : 1);

	    dst.left = key.getOverlayPivotX() + (noteSizeInAtlas - signSizeInAtlas) / 2;
	    dst.right = key.getOverlayPivotX() + (noteSizeInAtlas - signSizeInAtlas) / 2 + signSizeInAtlas;
	    dst.top = key.getOverlayPivotY() - height / 2;
	    dst.bottom = key.getOverlayPivotY() + height / 2;

	    canvas.drawBitmap(notesAtlas, src, dst, null);
	}
    }

    private void setupSourceRect(int height, float offset, float step, int index) {
	src.left = (int) (index * step + offset);
	src.top = 0;
	src.right = (int) ((index + 1) * step + offset);
	src.bottom = height;
    }

    public int getFirstVisibleKey() {
	int locateVisibleKey = locateVisibleKey(screenLeft, true);
	if (locateVisibleKey == NOT_FOUND) {
	    locateVisibleKey = 0;
	}
	return locateVisibleKey + Keyboard.START_MIDI_CODE;
    }

    public int getLastVisibleKey() {
	int locateVisibleKey = locateVisibleKey(screenRight, false);
	if (locateVisibleKey == NOT_FOUND) {
	    locateVisibleKey = keysArray.length - 1;
	}
	return locateVisibleKey + Keyboard.START_MIDI_CODE;
    }

    public boolean isInitialized() {
	return keysArray != null;
    }

    public void initializeInstrument(float measuredHeight, Context context) {
	whiteKeyWidth = Math.round(measuredHeight / WHITE_KEY_ASPECT_RATIO);
	octaveWidth = whiteKeyWidth * 7;

	int blackHalfWidth = octaveWidth / 20;
	blackKeyHeight = Math.round(measuredHeight / BLACK_KEY_HEIGHT_PERCENT);

	keysArray = new Key[KEYS_IN_OCTAVE * OCTAVES];
	int whiteIndex = 0;
	int blackIndex = 0;
	for (int i = 0; i < KEYS_IN_OCTAVE; i++) {

	    Key key = new Key();
	    if (isWhite(i)) {
		key.black = false;
		key.setBounds(whiteKeyWidth * whiteIndex, whiteKeyWidth * whiteIndex + whiteKeyWidth, 0, measuredHeight);
		whiteIndex++;
	    } else {
		key.black = true;
		int indexDisplacement = i == 1 || i == 3 ? 1 : 2;
		key.setBounds(whiteKeyWidth * (blackIndex + indexDisplacement) - blackHalfWidth, whiteKeyWidth
			* (blackIndex + indexDisplacement) + blackHalfWidth, 0, blackKeyHeight);
		blackIndex++;
	    }
	    key.midiCode = START_MIDI_CODE + i;
	    keysArray[i] = key;
	}
	for (int i = KEYS_IN_OCTAVE; i < KEYS_IN_OCTAVE * OCTAVES; i++) {
	    Key firstOctaveKey = keysArray[i % KEYS_IN_OCTAVE];

	    Key key = firstOctaveKey.clone();
	    key.startX += (i / KEYS_IN_OCTAVE) * octaveWidth;
	    key.endX += (i / KEYS_IN_OCTAVE) * octaveWidth;
	    key.midiCode = START_MIDI_CODE + i;
	    keysArray[i] = key;
	}
    }

    private static boolean isWhite(int i) {
	return i == 0 || i == 2 || i == 4 || i == 5 || i == 7 || i == 9 || i == 11;
    }

    public boolean touchItem(float x, float y) {
	touchedKey = locateTouchedKey(x, y);
	if (touchedKey != -1) {
	    keysArray[touchedKey].pressed = true;
	    return true;
	}
	return false;
    }

    public boolean releaseTouch() {
	if (touchedKey != -1) {
	    keysArray[touchedKey].pressed = false;
	    touchedKey = -1;
	    return true;
	}
	return false;
    }

    private int locateVisibleKey(float x, boolean first) {
	int octaveIndex = (int) (x / (float) octaveWidth);

	int resultIndex = 0;
	for (int j = 0; j < BLACK_INDICES.length; j++) {
	    int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + BLACK_INDICES[j];
	    if (checkKeyAtIndex(x, 40, index)) {
		resultIndex = index;
	    }
	}

	int whiteKeyIndex = (int) ((x - octaveIndex * octaveWidth) / (float) whiteKeyWidth);
	int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + WHITE_INDICES[whiteKeyIndex];
	if (checkKeyAtIndex(x, 40, index)) {
	    if (first) {
		return resultIndex > index ? index : resultIndex;
	    } else {
		return resultIndex > index ? resultIndex : index;
	    }
	}

	return NOT_FOUND;
    }

    private int locateTouchedKey(float x, float y) {
	int octaveIndex = (int) (x / (float) octaveWidth);

	if (y <= blackKeyHeight) {
	    for (int j = 0; j < BLACK_INDICES.length; j++) {
		int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + BLACK_INDICES[j];
		if (checkKeyAtIndex(x, y, index)) {
		    return index;
		}
	    }
	}

	int whiteKeyIndex = (int) ((x - octaveIndex * octaveWidth) / (float) whiteKeyWidth);
	int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + WHITE_INDICES[whiteKeyIndex];
	if (checkKeyAtIndex(x, y, index)) {
	    return index;
	}

	return NOT_FOUND;
    }

    private boolean checkKeyAtIndex(float x, float y, int index) {
	if (index < 0 || index >= keysArray.length) {
	    return false;
	}
	return keysArray[index].containsPoint(x, y);
    }

}

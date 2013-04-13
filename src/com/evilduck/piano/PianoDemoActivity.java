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
package com.evilduck.piano;

import java.util.Arrays;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.DecelerateInterpolator;

import com.evilduck.piano.music.Note;
import com.evilduck.piano.views.instrument.PianoView;
import com.evilduck.piano.views.instrument.PianoView.OnKeyTouchListener;

@SuppressLint("NewApi")
public class PianoDemoActivity extends Activity {

    private PianoView pianoView;

    private boolean scaledDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_piano_demo);

	pianoView = (PianoView) findViewById(R.id.instrument_view);
	pianoView.setOnKeyTouchListener(new OnKeyTouchListener() {
	    @Override
	    public void onTouch(int midiCode) {
		Note note = Note.fromCode(midiCode);

		pianoView.addNotes(Arrays.asList(note));
	    }

	    @Override
	    public void onLongTouch(int midiCode) {
	    }
	});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.piano_demo, menu);

	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.action_clear:
	    pianoView.clear();
	    break;
	case R.id.action_scale:
	    if (!scaledDown) {
		scaleDown();
	    } else {
		scaleUp();
	    }
	    scaledDown = !scaledDown;
	    break;
	default:
	    break;
	}
	return super.onOptionsItemSelected(item);
    }

    private void scaleDown() {
	ValueAnimator va = ValueAnimator.ofInt(pianoView.getHeight(), pianoView.getHeight() / 2);
	va.setInterpolator(new DecelerateInterpolator());
	va.addUpdateListener(new AnimatorUpdateListener() {
	    @Override
	    public void onAnimationUpdate(ValueAnimator animation) {
		pianoView.getLayoutParams().height = (Integer) animation.getAnimatedValue();
		pianoView.requestLayout();
	    }
	});
	va.start();
    }

    private void scaleUp() {
	ValueAnimator va = ValueAnimator.ofInt(pianoView.getHeight(), pianoView.getHeight() * 2);
	va.setInterpolator(new DecelerateInterpolator());
	va.addUpdateListener(new AnimatorUpdateListener() {
	    @Override
	    public void onAnimationUpdate(ValueAnimator animation) {
		pianoView.getLayoutParams().height = (Integer) animation.getAnimatedValue();
		pianoView.requestLayout();
	    }
	});
	va.start();
    }

    // private ArrayList<Note> getAllNotes() {
    // ArrayList<Note> notesToDraw = new ArrayList<Note>();
    //
    // notesToDraw.add(Note.C.inOctave(2));
    // notesToDraw.add(Note.C.sharp().inOctave(2));
    // notesToDraw.add(Note.D.inOctave(2));
    // notesToDraw.add(Note.D.sharp().inOctave(2));
    // notesToDraw.add(Note.E.inOctave(2));
    // notesToDraw.add(Note.F.inOctave(2));
    // notesToDraw.add(Note.F.sharp().inOctave(2));
    // notesToDraw.add(Note.G.inOctave(2));
    // notesToDraw.add(Note.G.sharp().inOctave(2));
    // notesToDraw.add(Note.A.inOctave(2));
    // notesToDraw.add(Note.A.sharp().inOctave(2));
    // notesToDraw.add(Note.B.inOctave(2));
    // notesToDraw.add(Note.C.inOctave(3));
    // notesToDraw.add(Note.C.sharp().inOctave(3));
    // notesToDraw.add(Note.D.inOctave(3));
    // notesToDraw.add(Note.D.sharp().inOctave(3));
    // notesToDraw.add(Note.E.inOctave(3));
    // notesToDraw.add(Note.F.inOctave(3));
    // notesToDraw.add(Note.F.sharp().inOctave(3));
    // notesToDraw.add(Note.G.inOctave(3));
    // notesToDraw.add(Note.G.sharp().inOctave(3));
    // notesToDraw.add(Note.A.inOctave(3));
    // notesToDraw.add(Note.A.sharp().inOctave(3));
    // notesToDraw.add(Note.B.inOctave(3));
    // notesToDraw.add(Note.C.inOctave(4));
    // notesToDraw.add(Note.C.sharp().inOctave(4));
    // notesToDraw.add(Note.D.inOctave(4));
    // notesToDraw.add(Note.D.sharp().inOctave(4));
    // notesToDraw.add(Note.E.inOctave(4));
    // notesToDraw.add(Note.F.inOctave(4));
    // notesToDraw.add(Note.F.sharp().inOctave(4));
    // notesToDraw.add(Note.G.inOctave(4));
    // notesToDraw.add(Note.G.sharp().inOctave(4));
    // notesToDraw.add(Note.A.inOctave(4));
    // notesToDraw.add(Note.A.sharp().inOctave(4));
    // notesToDraw.add(Note.B.inOctave(4));
    // notesToDraw.add(Note.C.inOctave(5));
    // notesToDraw.add(Note.C.sharp().inOctave(5));
    // notesToDraw.add(Note.D.inOctave(5));
    // notesToDraw.add(Note.D.sharp().inOctave(5));
    // notesToDraw.add(Note.E.inOctave(5));
    // notesToDraw.add(Note.F.inOctave(5));
    // notesToDraw.add(Note.F.sharp().inOctave(5));
    // notesToDraw.add(Note.G.inOctave(5));
    // notesToDraw.add(Note.G.sharp().inOctave(5));
    // notesToDraw.add(Note.A.inOctave(5));
    // notesToDraw.add(Note.A.sharp().inOctave(5));
    // notesToDraw.add(Note.B.inOctave(5));
    //
    // return notesToDraw;
    // }

}

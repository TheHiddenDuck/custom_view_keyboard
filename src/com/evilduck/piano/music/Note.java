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
package com.evilduck.piano.music;

/**
 * Class represents a single note.
 * 
 * @author EvilDuck
 */
public final class Note implements Comparable<Note> {

    public static final byte MODIFIER_NONE = 0;

    public static final byte SHARP = 1;

    public static final byte FLAT = -1;

    private static final int[] NOTE_INDEX = { 0, 2, 4, 5, 7, 9, 11 };

    private static final char[] NOTES = { 'C', 'D', 'E', 'F', 'G', 'A', 'B' };

    private static final String CHAR_SHARP = "\u266F";

    private static final String CHAR_FLAT = "\u266D";

    public static class Names {

	public static final int C = 0;
	public static final int D = 1;
	public static final int E = 2;
	public static final int F = 3;
	public static final int G = 4;
	public static final int A = 5;
	public static final int B = 6;

    }

    private byte note;

    private byte modifier;

    private byte octave;

    private Note(byte note, byte modifier, byte octave) {
	this.note = note;
	this.modifier = modifier;
	this.octave = octave;
    }

    public byte getNote() {
	return note;
    }

    public byte getModifier() {
	return modifier;
    }

    public byte getOctave() {
	return octave;
    }

    public byte getMidiCode() {
	return (byte) (12 * octave + NOTE_INDEX[note] + modifier);
    }

    public byte getMidiCodeWithoutModifier() {
	return (byte) (12 * octave + NOTE_INDEX[note]);
    }

    @Override
    public String toString() {
	String modifierString = null;
	switch (modifier) {
	case SHARP:
	    modifierString = CHAR_SHARP;
	    break;
	case FLAT:
	    modifierString = CHAR_FLAT;
	    break;
	}

	return NOTES[note] + (modifierString != null ? modifierString : "");
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + modifier;
	result = prime * result + note;
	result = prime * result + octave;
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Note other = (Note) obj;
	if (modifier != other.modifier)
	    return false;
	if (note != other.note)
	    return false;
	if (octave != other.octave)
	    return false;
	return true;
    }

    // Builders

    public static class NoteBuilder {

	int note;
	int modifier = 0;

	private NoteBuilder(int note) {
	    this.note = note;
	}

	public NoteBuilder sharp() {
	    modifier = SHARP;
	    return this;
	}

	public NoteBuilder flat() {
	    modifier = FLAT;
	    return this;
	}

	public Note inOctave(int octave) {
	    Note createdNote = new Note((byte) note, (byte) modifier, (byte) octave);
	    modifier = 0;
	    return createdNote;
	}

    }

    public static final NoteBuilder C = new NoteBuilder(Names.C);

    public static final NoteBuilder D = new NoteBuilder(Names.D);

    public static final NoteBuilder E = new NoteBuilder(Names.E);

    public static final NoteBuilder F = new NoteBuilder(Names.F);

    public static final NoteBuilder G = new NoteBuilder(Names.G);

    public static final NoteBuilder A = new NoteBuilder(Names.A);

    public static final NoteBuilder B = new NoteBuilder(Names.B);

    @Override
    public int compareTo(Note another) {
	return getMidiCode() - another.getMidiCode();
    }

    public static Note fromCode(int code) {
	byte octave = (byte) (code / 12);

	int cd = code % 12;

	byte modifier = 0;
	byte note = 0;
	for (int i = NOTE_INDEX.length - 1; i >= 0; i--) {
	    if (cd == NOTE_INDEX[i]) {
		modifier = 0;
		note = (byte) i;
		break;
	    } else if (cd == NOTE_INDEX[i] + 1) {
		modifier = 1;
		note = (byte) i;
		break;
	    }
	}

	return new Note(note, modifier, octave);
    }

}

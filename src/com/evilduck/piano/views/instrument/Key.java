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

class Key implements Cloneable {

    float startX;

    float endX;

    float startY;

    float endY;

    int midiCode;

    boolean black;

    boolean pressed = false;

    void setBounds(float startX, float endX, float startY, float endY) {
	this.startX = startX;
	this.startY = startY;
	this.endX = endX;
	this.endY = endY;
    }

    @Override
    protected Key clone() {
	Key key = new Key();
	key.startX = startX;
	key.endX = endX;
	key.startY = startY;
	key.endY = endY;
	key.midiCode = midiCode;
	key.black = black;

	return key;
    }

    boolean containsPoint(float x, float y) {
	return startX <= x && endX > x && startY <= y && endY > y;
    }

    float getOverlayPivotX() {
	return (endX - startX) / 2f + startX;
    }

    float getOverlayPivotY() {
	if (!black) {
	    return (endY - startY) * 0.85f + startY;
	}
	return (endY - startY) / 2f + startY;
    }
}
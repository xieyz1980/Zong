package com.xenoage.zong.musiclayout.spacing;

import com.xenoage.zong.musiclayout.notation.ChordNotation;

public class ChordSpacing {
	
	public static float getStemXIs(ElementSpacing chord) {
		ChordNotation not = (ChordNotation) chord.notation;
		return chord.getOffsetIs() + not.accidentals.widthIs + not.notes.stemOffsetIs;
	}

}
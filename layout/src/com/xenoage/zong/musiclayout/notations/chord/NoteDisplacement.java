package com.xenoage.zong.musiclayout.notations.chord;

import lombok.AllArgsConstructor;

import com.xenoage.utils.annotations.Const;


/**
 * The vertical position, horizontal offset and suspension of a single
 * note within a chord.
 *
 * @author Andreas Wenger
 */
@Const @AllArgsConstructor
public class NoteDisplacement {

	public final int yLp;
	public final float xIs;
	public final NoteSuspension suspension;

}
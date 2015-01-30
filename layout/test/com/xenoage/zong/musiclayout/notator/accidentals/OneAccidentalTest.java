package com.xenoage.zong.musiclayout.notator.accidentals;

import static com.xenoage.utils.collections.CollectionUtils.alist;
import static com.xenoage.utils.math.Delta.df;
import static com.xenoage.zong.core.music.Pitch.pi;
import static com.xenoage.zong.musiclayout.notations.chord.NoteDisplacementTest.note;
import static com.xenoage.zong.musiclayout.notator.accidentals.OneAccidental.oneAccidental;
import static com.xenoage.zong.musiclayout.notator.accidentals.Strategy.getParams;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.xenoage.zong.musiclayout.notations.chord.AccidentalsNotation;
import com.xenoage.zong.musiclayout.notations.chord.NoteDisplacement;

/**
 * Tests for {@link OneAccidental}.
 * 
 * @author Andreas Wenger
 */
public class OneAccidentalTest
	extends TestData {
	
	private OneAccidental testee = oneAccidental;
	
	
	@Test public void test() {
		//C#5
		AccidentalsNotation accs = testee.compute(getParams(
			alist(pi(0, 1, 5)), new NoteDisplacement[] { note(5) }, 1, cw, contextC));
		assertEquals(1, accs.accidentals.length);
		assertEquals(cw.sharp + cw.accToNoteGap, accs.widthIs, df);
		//C##5
		accs = testee.compute(getParams(
			alist(pi(0, 2, 5)), new NoteDisplacement[] { note(5) }, 1, cw, contextC));
		assertEquals(1, accs.accidentals.length);
		assertEquals(cw.doubleSharp + cw.accToNoteGap, accs.widthIs, df);
		//C4, D4, Gbb4
		accs = testee.compute(getParams(
			alist(pi(0, 0, 4), pi(1, 0, 4), pi(4, -2, 4)),
			new NoteDisplacement[] { note(-2), note(-1, noteOffset, susRight), note(2) }, 1, cw, contextC));
		assertEquals(1, accs.accidentals.length);
		assertEquals(cw.doubleFlat + cw.accToNoteGap, accs.widthIs, df);
		//Eb4, A4, G##5 with contextEb
		accs = testee.compute(getParams(
			alist(pi(2, -1, 4), pi(5, 0, 4), pi(4, 2, 5)),
			new NoteDisplacement[] { note(0), note(3), note(9) }, 1, cw, contextEb));
		assertEquals(1, accs.accidentals.length);
		assertEquals(cw.natural + cw.accToNoteGap, accs.widthIs, df);
	}

}

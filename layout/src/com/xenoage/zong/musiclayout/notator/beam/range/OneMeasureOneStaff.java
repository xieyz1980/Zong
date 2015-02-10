package com.xenoage.zong.musiclayout.notator.beam.range;

import static com.xenoage.utils.collections.CollectionUtils.alist;
import static com.xenoage.utils.kernel.Range.range;
import static com.xenoage.zong.core.music.chord.StemDirection.Down;
import static com.xenoage.zong.core.music.chord.StemDirection.Up;
import static com.xenoage.zong.core.position.MP.getMP;
import static com.xenoage.zong.musiclayout.layouter.beamednotation.design.ChordBeamSpacing.Close;
import static com.xenoage.zong.musiclayout.layouter.beamednotation.design.ChordBeamSpacing.Normal;
import static com.xenoage.zong.musiclayout.layouter.beamednotation.design.ChordBeamSpacing.Wide;

import java.util.List;

import com.xenoage.utils.math.MathUtils;
import com.xenoage.zong.core.music.beam.Beam;
import com.xenoage.zong.core.music.beam.BeamWaypoint;
import com.xenoage.zong.core.music.chord.Chord;
import com.xenoage.zong.core.music.chord.StemDirection;
import com.xenoage.zong.core.position.MP;
import com.xenoage.zong.musiclayout.layouter.beamednotation.design.ChordBeamSpacing;
import com.xenoage.zong.musiclayout.notations.BeamNotation;
import com.xenoage.zong.musiclayout.notations.ChordNotation;
import com.xenoage.zong.musiclayout.notations.chord.StemNotation;
import com.xenoage.zong.musiclayout.notator.beam.lines.BeamLines;
import com.xenoage.zong.musiclayout.notator.beam.lines.MultipleLines;
import com.xenoage.zong.musiclayout.notator.beam.lines.OneLine;
import com.xenoage.zong.musiclayout.notator.beam.lines.ThreeLines;
import com.xenoage.zong.musiclayout.notator.beam.lines.TwoLines;
import com.xenoage.zong.musiclayout.spacing.ColumnSpacing;
import com.xenoage.zong.musiclayout.spacing.ElementSpacing;

/**
 * {@link Strategy} for a {@link Beam}, which spans over a single staff and measure.
 * 
 * TIDY
 * 
 * @author Uli Teschemacher
 * @author Andreas Wenger
 */
public class OneMeasureOneStaff
	implements Strategy {
	
	public static final OneMeasureOneStaff oneMeasureOneStaff = new OneMeasureOneStaff();

	private static final float closeSpacing = 5;
	private static final float wideSpacing = 8;


	@Override public void compute(Beam beam, List<ColumnSpacing> columnSpacings) {

		//collect needed information
		ColumnSpacing column = columnSpacings.get(getMP(beam.getChord(0)).measure);
		List<ChordNotation> chords = alist(beam.size());
		float[] stemX = new float[beam.getWaypoints().size()];
		Chord firstChord = beam.getStart().getChord();
		MP firstChordMP = getMP(firstChord);
		int staffLinesCount = firstChord.getScore().getStaff(firstChordMP).getLinesCount();
		int beamLinesCount = beam.getMaxBeamLinesCount();
		int i = 0;
		StemDirection dir = column.getNotation(beam.getChord(0)).stemDirection;
		for (BeamWaypoint waypoint : beam.getWaypoints()) {
			Chord chord = waypoint.getChord();
			ElementSpacing cs = column.getElement(chord);
			ChordNotation cn = (ChordNotation) cs.notation;
			chords.add(cn);
			stemX[i] = getStemXIs(cs);
			i++;
		}

		//compute the stem alignments
		compute(chords, stemX, staffLinesCount, beamLinesCount, dir);
	}

	/**
	 * Computes the vertical positions of the stems of the given
	 * chord layouts grouped by a beam within a single staff and single measure.
	 * @param chords           the notations of all chords of the beam
	 * @param stemX            the horizontal positions of the stems in interline spaces
	 * @param staffLinesCount  the number of lines in this staff
	 * @param beamLinesCount   the number of lines of the beam
	 * @param stemDirection    the direction of the stem
	 * @return  the alignments of all stems of the given chords                        
	 */
	void compute(List<ChordNotation> chords, float[] stemX,
		int staffLinesCount, int beamLinesCount, StemDirection stemDirection) {
		
		//get appropriate beam design
		BeamLines beamDesign;
		switch (beamLinesCount) {
			case 1:
				beamDesign = new OneLine(stemDirection, staffLinesCount);
				break;
			case 2:
				beamDesign = new TwoLines(stemDirection, staffLinesCount);
				break;
			case 3:
				beamDesign = new ThreeLines(stemDirection, staffLinesCount);
				break;
			default:
				beamDesign = new MultipleLines(stemDirection, staffLinesCount, beamLinesCount);
		}

		//compute beautiful slant
		float slantIs = computeSlant(beamDesign, chords, stemX, stemDirection, staffLinesCount);

		//compute stem alignments
		computeStemLengths(beamDesign, chords, stemX, slantIs, beamLinesCount, stemDirection);
	}

	/**
	 * Computes the slant that fits best to the given beam.
	 * The slant is defined as the directed vertical distance of
	 * the first stem endpoint and the last stem endpoint in IS.
	 * @param beamDesign     design information about the beam
	 * @param chords         the notations of all chords of the beam
	 * @param stemX          the horizontal positions of the stems in interline spaces   
	 * @param stemDirection  the direction of the stems
	 * @param staffLinesCount  the number of lines in this staff
	 */
	private float computeSlant(BeamLines beamDesign, List<ChordNotation> chords,
		float[] stemX, StemDirection stemDirection, int staffLinesCount) {
		
		//collect relevant note line positions (positions of the outermost notes)
		int firstRelevantNoteLp;
		int lastRelevantNoteLp;
		int chordsCount = chords.size();
		int[] relevantNoteLps = new int[chordsCount];
		if (stemDirection == Down) {
			for (int i : range(chords))
				relevantNoteLps[i] = chords.get(i).notes.getBottomNote().lp;
		}
		else if (stemDirection == StemDirection.Up) {
			for (int i : range(chords))
				relevantNoteLps[i] = chords.get(i).notes.getTopNote().lp;
		}
		firstRelevantNoteLp = relevantNoteLps[0];
		lastRelevantNoteLp = relevantNoteLps[chordsCount - 1];

		//if the notes outline the same interval e.g. a e a e => horizontal beam
		if (chordsCount >= 4 && chordsCount % 2 == 0) {
			boolean useHorizontalBeam = true;
			for (int i = 2; i < chordsCount; i++) {
				if (relevantNoteLps[i % 2] != relevantNoteLps[i]) {
					useHorizontalBeam = false;
				}
			}
			if (useHorizontalBeam) {
				return 0;
			}
		}

		//if all middlenotes are further away from the beam than a direct line from
		//the first to the last note, a normal slant is used
		boolean useDefaultSlant = true;
		for (int i = 1; i < chordsCount - 2; i++) {
			float lp = firstRelevantNoteLp + 1f * (lastRelevantNoteLp - firstRelevantNoteLp) * i /
				(chordsCount - 1);
			if ((stemDirection == Up && relevantNoteLps[i] > Math.ceil(lp)) ||
				(stemDirection == Down && relevantNoteLps[i] < Math.floor(lp))) {
				useDefaultSlant = false;
				break;
			}
		}
		if (useDefaultSlant) {
			//use default rules (Ted Ross page 111)
			float lengthX = stemX[chordsCount - 1] - stemX[0];
			return computeNormalBeamSlant(beamDesign, firstRelevantNoteLp, lastRelevantNoteLp, lengthX,
				staffLinesCount);
		}

		//rule Ted Ross page 97 bottom
		//When the first and last notes are on different staff degrees, and all inside notes
		//descend to the last note, the beam slants one-half space in the direction
		//of the run of the inside notes.
		if (firstRelevantNoteLp != lastRelevantNoteLp) {
			boolean ascend = true;
			for (int i = 2; i < chordsCount; i++) {
				if (relevantNoteLps[i - 1] >= relevantNoteLps[i]) {
					ascend = false;
					break;
				}
			}
			if (ascend) {
				return beamDesign.getSlantAscendingMiddleNotesIs();
			}
			boolean descend = true;
			for (int i = 1; i < chordsCount - 1; i++) {
				if (relevantNoteLps[i - 1] <= relevantNoteLps[i]) {
					descend = false;
					break;
				}
			}
			if (descend) {
				return beamDesign.getSlantDescendingMiddleNotesIs();
			}
		}

		//otherwise: horizontal beam
		return 0;
	}

	/**
	 * This method calculates the slant of a beam in IS.
	 * The method of the calculations is a mixture between those of Ted Ross (p. 97ff)
	 * and the behaviour of Sibelius.
	 * @param beamDesign       design information about the beam
	 * @param firstNoteLp      the LP of the outermost note of the first chord
	 * @param lastNoteLp       the LP of the outermost note of the last chord
	 * @param lengthX          the horizontal distance between the first and the last note in IS
	 * @param staffLinesCount  the number of lines in this staff
	 */
	private float computeNormalBeamSlant(BeamLines beamDesign, int firstNoteLp, int lastNoteLp,
		float lengthX, int staffLinesCount) {
		ChordBeamSpacing spacing;
		if (lengthX > wideSpacing)
			spacing = Wide;
		else if (lengthX < closeSpacing)
			spacing = Close;
		else
			spacing = Normal;
		return computeSlant(beamDesign, firstNoteLp, lastNoteLp, spacing, staffLinesCount);
	}

	/**
	 * This method calculates the slant of a beam in IS.
	 * The method of the calculations is a mixture between those of Ted Ross (p. 97ff)
	 * and the behaviour of Sibelius.
	 * @param beamDesign       design information about the beam
	 * @param firstNoteLp      the LP of the outermost note of the first chord
	 * @param lastNoteLp       the LP of the outermost note of the last chord 
	 * @param spacing          the horizontal spacing of the beam chords
	 * @param staffLinesCount  the number of lines in this staff
	 */
	private float computeSlant(BeamLines beamDesign, int firstNoteLp, int lastNoteLp,
		ChordBeamSpacing spacing, int staffLinesCount) {
		int distanceLp = lastNoteLp - firstNoteLp;
		float slantIs = 0;
		boolean useSimpleSlant = false;

		if (distanceLp == 0)
			return 0;

		if (Math.abs(distanceLp) > 8) {
			//interval greater than an octave -> only a small slant (Sibelius)
			return MathUtils.sign(distanceLp) * 1f;
		}

		int staffMaxLp = (staffLinesCount - 1) * 2;
		useSimpleSlant = (firstNoteLp >= staffMaxLp + 2 || lastNoteLp >= staffMaxLp + 2 //above the first leger line
			|| firstNoteLp <= -2 || lastNoteLp <= -2); //below the first leger line

		if (useSimpleSlant || spacing == Close)
			slantIs = beamDesign.getCloseSpacingSlantIs(firstNoteLp, lastNoteLp) / 2;
		else if (spacing == Wide)
			slantIs = beamDesign.getWideSpacingSlantIs(firstNoteLp, lastNoteLp) / 2;
		else
			slantIs = beamDesign.getNormalSpacingSlantIs(firstNoteLp, lastNoteLp) / 2;

		if (distanceLp < 0)
			slantIs = -1 * slantIs;

		return slantIs;
	}

	/**
	 * Computes the lengths of all stems of the chord.
	 * @param beamDesign       design information about the beam
	 * @param chords           the notations of all chords of the beam
	 * @param stemX            the horizontal positions of all stems of the beam
	 * @param slantIS          the slant in interline spaces        
	 * @param beamLinesCount   the number of beam lines         
	 * @param stemDirection    the direction of the stems
	 */
	private void computeStemLengths(BeamLines beamDesign, List<ChordNotation> chords,
		float[] stemX, float slantIS, int beamLinesCount, StemDirection stemDirection) {

		float beamStartLPCorrection = stemDirection.getSign() * 0.5f;

		boolean correctStemLength = false;
		float lengthX = stemX[stemX.length - 1] - stemX[0];
		float endline = 0;

		//try to place the beam at LP 4 first. correct step for step, if not ok.
		float beamStartLP = 4;

		BeamNotation beamNot = new BeamNotation(BeamLines.beamLineHeightIs,
			beamDesign.getDistanceBetweenBeamLinesIs(), beamLinesCount);
		
		
		while (true) {

			//for each chord: check if stem length is ok
			for (int i : range(chords)) {
				int highestNote = chords.get(i).notes.getTopNote().lp;
				int lowestNote =  chords.get(i).notes.getBottomNote().lp;

				//compute stem length at current chord
				endline = beamStartLP + slantIS * 2 * (stemX[i] - stemX[0]) / lengthX;
				float startline;
				float currentStemLength = 0;

				if (stemDirection == Up) {
					startline = lowestNote;
					currentStemLength = (endline - highestNote) / 2;
				}
				else {
					startline = highestNote;
					currentStemLength = (lowestNote - endline) / 2;
				}

				//stem length ok?
				if (currentStemLength < beamDesign.getMinimumStemLengthIs()) {
					correctStemLength = false;
					break;
				}

				chords.get(i).stem = new StemNotation(startline, endline);
				chords.get(i).beam = beamNot;
				
				correctStemLength = true;
			}

			//beam found, that looks ok for all stems?
			if (correctStemLength &&
				beamDesign.isGoodStemPosition(beamStartLP, slantIS)) {
				//ok. we can break the loop.
				break;
			}
			else {
				//no. start another try.
				beamStartLP += beamStartLPCorrection;
			}
		}
	}
	
	private float getStemXIs(ElementSpacing chord) {
		ChordNotation not = (ChordNotation) chord.notation;
		return chord.getOffsetIs() + not.accidentals.widthIs + not.notes.stemOffsetIs;
	}

}
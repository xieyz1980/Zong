package com.xenoage.zong.io.midi.out;

import com.xenoage.zong.core.music.Pitch;

import static com.xenoage.utils.math.MathUtils.clamp;
import static com.xenoage.zong.core.music.Pitch.pi;


/**
 * Some useful methods for working
 * with MIDI data.
 *
 * @author Andreas Wenger
 */
public class MidiTools {

  /**
   * Converts the given MIDI note number (between 0 and 127)
   * to a {@link Pitch} and returns it.
   * Only positive values are used for alter.
   */
  public static Pitch getPitchFromNoteNumber(int noteNumber) {
    //12 = C0, 24 = C1, 36 = C2, ...
    int octave = noteNumber / 12 - 1;
    noteNumber -= (octave + 1) * 12;
    int step = 0, alter = 0;
    switch (noteNumber) {
      case 0: step = 0; alter = 0; break;
      case 1: step = 0; alter = 1; break;
      case 2: step = 1; alter = 0; break;
      case 3: step = 1; alter = 1; break;
      case 4: step = 2; alter = 0; break;
      case 5: step = 3; alter = 0; break;
      case 6: step = 3; alter = 1; break;
      case 7: step = 4; alter = 0; break;
      case 8: step = 4; alter = 1; break;
      case 9: step = 5; alter = 0; break;
      case 10: step = 5; alter = 1; break;
      case 11: step = 6; alter = 0; break;
    }
    return pi(step, alter, octave);
  }

  /**
   * Converts the given {@link Pitch} to a MIDI note number
   * (between 0 and 127) and returns it.
   * If the note number would be outside the range of 0 to 127,
   * 0 or 127 is returned.
   */
  public static int getNoteNumber(Pitch pitch) {
    return getNoteNumber(pitch, 0);
  }

  /**
   * Converts the given {@link Pitch} with the given transposition in half steps
   * to a MIDI note number (between 0 and 127) and returns it.
   * If the note number would be outside the range of 0 to 127,
   * 0 or 127 is returned.
   */
  public static int getNoteNumber(Pitch pitch, int transpose) {
    int ret = (pitch.getOctave() + 1) * 12 + transpose;
    switch (pitch.getStep()) {
      case 0: break;
      case 1: ret += 2; break;
      case 2: ret += 4; break;
      case 3: ret += 5; break;
      case 4: ret += 7; break;
      case 5: ret += 9; break;
      case 6: ret += 11; break;
    }
    ret += pitch.getAlter();
    ret = clamp(ret, 0, 127);
    return ret;
  }

}

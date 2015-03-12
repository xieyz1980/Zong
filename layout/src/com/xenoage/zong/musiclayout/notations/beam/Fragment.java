package com.xenoage.zong.musiclayout.notations.beam;

/**
 * Style of a beam line at one point.
 * 
 * For example, a beam might have at a specific
 * stem a continous line on the 8th line, but a
 * left hook on the 16th line.
 * 
 * @author Andreas Wenger
 */
public enum Fragment {
	/** No beam line or (TODO: really ? ) within a continuous line. */
	None,
	/** A continuous line starts here. */
	Start,
	/** A continuous line ends here. */
	Stop,
	/** Just a hook at the left side. */
	HookLeft,
	/** Just a hook at the right side. */
	HookRight,
	/** End of continuous line, but extend the line to the right hook on the next level. */
	StopHookRight
}

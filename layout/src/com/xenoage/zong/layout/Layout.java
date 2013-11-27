package com.xenoage.zong.layout;

import static com.xenoage.utils.collections.CollectionUtils.alist;
import static com.xenoage.utils.kernel.Range.range;
import static com.xenoage.utils.pdlib.PMap.pmap;
import static com.xenoage.zong.layout.LP.lp;
import static com.xenoage.zong.musiclayout.layouter.ScoreLayoutArea.area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import lombok.Data;

import org.pcollections.PVector;

import com.xenoage.utils.annotations.Untested;
import com.xenoage.utils.collections.CollectionUtils;
import com.xenoage.utils.kernel.Tuple2;
import com.xenoage.utils.math.geom.Point2f;
import com.xenoage.utils.math.geom.Rectangle2f;
import com.xenoage.utils.pdlib.PMap;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.core.format.LayoutFormat;
import com.xenoage.zong.layout.frames.FP;
import com.xenoage.zong.layout.frames.Frame;
import com.xenoage.zong.layout.frames.GroupFrame;
import com.xenoage.zong.layout.frames.ScoreFrame;
import com.xenoage.zong.layout.frames.ScoreFrameChain;
import com.xenoage.zong.musiclayout.ScoreFrameLayout;
import com.xenoage.zong.musiclayout.ScoreLP;
import com.xenoage.zong.musiclayout.ScoreLayout;
import com.xenoage.zong.musiclayout.layouter.ScoreLayoutArea;
import com.xenoage.zong.musiclayout.layouter.ScoreLayouter;
import com.xenoage.zong.musiclayout.settings.LayoutSettings;
import com.xenoage.zong.symbols.SymbolPool;
import com.xenoage.zong.util.event.ScoreChangedEvent;

/**
 * Class for the layout of a score document.
 * 
 * It consists of several pages, each containing several frames.
 * 
 * @author Andreas Wenger
 * @author Uli Teschemacher
 */
@Data public class Layout {

	/** Default settings */
	private LayoutDefaults defaults;

	/** The list of pages */
	private ArrayList<Page> pages;

	/** The list of selected frames */
	private ArrayList<Frame> selectedFrames;


	/**
	 * Initializes an empty {@link Layout}.
	 */
	public Layout(LayoutDefaults defaults) {
		this.defaults = defaults;
		this.pages = alist();
		this.selectedFrames = alist();
	}

	/**
	 * Adds a new page to this layout.
	 */
	public void addPage(Page page) {
		pages.add(page);
	}

	/**
	 * Transforms the given {@link LP} to a {@link FP} of the frame at this position.
	 * If there is no frame, null is returned.
	 */
	public FP getFP(LP lp) {
		if (lp == null)
			return null;
		Page page = pages.get(lp.pageIndex);
		return page.getFP(lp.position);
	}

	/**
	 * Call this method when the given score has been changed.
	 */
	public void scoreChanged(ScoreChangedEvent event) {
		updateScoreLayouts(event.oldScore, event.newScore);
	}

	/**
	 * Computes the adjustment handle at the given position and returns it. If
	 * there is none, null is returned.
	 * @param layoutPosition the position where to look for a handle
	 * @param scaling the current scaling factor
	 */
	/* TODO
	public FrameHandle computeFrameHandleAt(LayoutPosition layoutPosition, float scaling)
	{
		if (layoutPosition == null)
			return null;
		// find handle on the given page
		FrameHandle handle = null;
		Page givenPage = pages.get(layoutPosition.getPageIndex());
		handle = givenPage.computeFrameHandleAt(layoutPosition.getPosition(), scaling);
		return handle;
	} */

	/**
	 * Gets the axis-aligned bounding rectangle of the given system, specified
	 * by its {@link ScoreLayout}, the index of the frame and the index of the
	 * system (relative to the frame) in page coordinates together with the
	 * index of the page. If not found, null is returned.
	 */
	/* TODO
	public Tuple2<Integer, Rectangle2f> getSystemBoundingRect(ScoreLayout scoreLayout,
		int frameIndex, int systemIndex) {
		
		//find the frame
		ScoreFrameChain chain = scoreLayouts.getKeyByValue(scoreLayout);
		ScoreFrame scoreFrame = chain.frames.get(frameIndex);

		//get system boundaries in mm
		ScoreFrameLayout scoreFrameLayout = scoreLayout.frames.get(frameIndex);
		Rectangle2f rectMm = scoreFrameLayout.getSystemBoundaries(systemIndex);
		if (rectMm == null)
			return null;

		//compute corner points (because frame may be rotated) and transform them
		float x = rectMm.position.x - scoreFrame.getSize().width / 2;
		float y = rectMm.position.y - scoreFrame.getSize().height / 2;
		float w = rectMm.size.width;
		float h = rectMm.size.height;
		Point2f nw = scoreFrame.computePagePosition(new Point2f(x, y), this);
		Point2f ne = scoreFrame.computePagePosition(new Point2f(x + w, y), this);
		Point2f se = scoreFrame.computePagePosition(new Point2f(x + w, y + h), this);
		Point2f sw = scoreFrame.computePagePosition(new Point2f(x, y + h), this);

		// compute axis-aligned bounding box and return it
		Rectangle2f ret = new Rectangle2f(nw.x, nw.y, 0, 0);
		ret = ret.extend(ne);
		ret = ret.extend(se);
		ret = ret.extend(sw);

		int pageIndex = pages.indexOf(getPage(scoreFrame));
		return new Tuple2<Integer, Rectangle2f>(pageIndex, ret);
	} */

	/**
	 * Gets the {@link ScoreFrame} which contains the given measure within
	 * the given score. If it can not be found, null is returned.
	 */
	@Untested public ScoreFrame getScoreFrame(Score score, int measure) {
		ScoreFrameChain chain = getScoreFrameChain(score);
		if (chain == null || chain.getScoreLayout() == null)
			return null;
		int frameIndex = chain.getScoreLayout().getFrameIndexOf(measure);
		if (frameIndex >= 0 && frameIndex < chain.getFrames().size())
			return chain.getFrames().get(frameIndex);
		return null;
	}

	/**
	 * Updates the {@link ScoreLayout}s belonging to the given {@link Score}.
	 */
	public Layout updateScoreLayouts(Score score) {
		ScoreFrameChain chain = getScoreFrameChain(score);
		if (chain == null)
			return null;
		ScoreLayout oldScoreLayout = chain.getScoreLayout();

		//select symbol pool and layout settings
		SymbolPool symbolPool = oldScoreLayout != null ?
			oldScoreLayout.symbolPool : defaults.getSymbolPool();
		LayoutSettings layoutSettings = oldScoreLayout != null ?
			oldScoreLayout.layoutSettings : defaults.getLayoutSettings();

		ArrayList<ScoreLayoutArea> areas = alist();
		for (ScoreFrame scoreFrame : chain.getFrames()) {
			areas.add(new ScoreLayoutArea(scoreFrame.getSize(), scoreFrame.getHFill(), scoreFrame.getVFill()));
		}
		ScoreLayout scoreLayout = new ScoreLayouter(score, symbolPool, layoutSettings, false,
			areas, areas.getLast()).createLayout();

		//create updated layout
		PMap<ScoreFrameChain, ScoreLayout> scoreLayouts = this.scoreLayouts.plus(chain, scoreLayout);
		PMap<Score, ScoreFrameChain> scoreFrameChains = this.scoreFrameChains.minus(oldScore).plus(
			newScore, chain);
		return new Layout(defaults, pages, scoreFrameChains, scoreLayouts, selectedFrames);
			
		return layout;
	}

	public Layout withDefaults(LayoutDefaults defaults) {
		return new Layout(defaults, pages, scoreFrameChains, scoreLayouts, selectedFrames);
	}

	/**
	 * Sets all pages to the given format.
	 * The defaults values are not changed.
	 */
	public Layout withLayoutFormat(LayoutFormat layoutFormat) {
		PVector<Page> pages = this.pages;
		for (int i : range(pages)) {
			pages = pages.with(i, pages.get(i).withFormat(layoutFormat.getPageFormat(i)));
		}
		return new Layout(defaults, pages, scoreFrameChains, scoreLayouts, selectedFrames);
	}

	public Layout withSelectedFrames(PVector<Frame> selectedFrames) {
		return new Layout(defaults, pages, scoreFrameChains, scoreLayouts, selectedFrames);
	}

	public Layout chainUpScoreFrame(ScoreFrame frameFrom, ScoreFrame frameTo) {
		if (frameFrom == frameTo)
			throw new IllegalArgumentException("Same frames");
		PMap<Score, ScoreFrameChain> scoreFrameChains = this.scoreFrameChains;
		//score layouts have to be recomputed
		PMap<ScoreFrameChain, ScoreLayout> scoreLayouts = this.scoreLayouts;

		//remove target frame from other chain, if there is one
		Score toOldScore = getScore(frameTo);
		if (toOldScore != null) {
			ScoreFrameChain toOldChain = scoreFrameChains.get(toOldScore);
			scoreLayouts = scoreLayouts.minus(toOldChain);
			toOldChain = toOldChain.replaceFrame(frameTo, null);
			if (toOldChain != null)
				scoreFrameChains = scoreFrameChains.plus(toOldScore, toOldChain);
			else
				scoreFrameChains = scoreFrameChains.minus(toOldScore);
		}

		//add target frame to the chain of the the source frame
		Score fromScore = getScore(frameFrom);
		ScoreFrameChain fromChain = scoreFrameChains.get(fromScore);
		scoreLayouts = scoreLayouts.minus(fromChain);
		fromChain = fromChain.plusFrame(frameFrom, frameTo);
		scoreFrameChains = scoreFrameChains.plus(fromScore, fromChain);

		return new Layout(defaults, pages, scoreFrameChains, scoreLayouts, selectedFrames);
	}

	/**
	 * Returns the {@link LP} of the given {@link BMP} within the given {@link Score}
	 * at the given line position, or null if unknown.
	 */
	public LP computeLP(Score score, BMP bmp, float lp) {
		ScoreFrameChain chain = scoreFrameChains.get(score);
		if (chain != null) {
			ScoreLayout sl = scoreLayouts.get(chain);
			ScoreLP slp = sl.computeScoreLP(bmp, lp);
			if (slp != null) {
				ScoreFrame frame = chain.frames.get(slp.frameIndex);
				Page page = getPage(frame);
				if (page != null) {
					Point2f frameP = slp.pMm.sub(frame.getSize().width / 2, frame.getSize().height / 2);
					int pageIndex = pages.indexOf(page);
					Point2f pMm = frame.computePagePosition(frameP, this);
					return lp(this, pageIndex, pMm);
				}
			}
		}
		return null;
	}

	/**
	 * Gets the {@link ScoreFrameChain} for the given {@link Score}, or null
	 * if it can not be found.
	 */
	private ScoreFrameChain getScoreFrameChain(Score score) {
		for (ScoreFrame frame : iterateScoreFrames()) {
			ScoreFrameChain chain = frame.getScoreFrameChain();
			if (chain != null && chain.getScore() == score)
				return chain;
		}
		return null;
	}

	/**
	 * Gets a lazy iterator for all {@link ScoreFrame}s in this layout.
	 */
	private Iterable<ScoreFrame> iterateScoreFrames() { //GOON
		return new Iterable<ScoreFrame>() {

			@Override public Iterator<ScoreFrame> iterator() {
				return new Iterator<ScoreFrame>() {

					@Override public boolean hasNext() {
						return false;
					}

					@Override public ScoreFrame next() {
						return null;
					}

					@Override public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

}
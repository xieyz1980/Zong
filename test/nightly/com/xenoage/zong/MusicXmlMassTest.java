package com.xenoage.zong;

import com.xenoage.utils.iterators.It;
import com.xenoage.utils.jse.io.JseInputStream;
import com.xenoage.utils.jse.io.JseOutputStream;
import com.xenoage.utils.jse.log.DesktopLogProcessing;
import com.xenoage.utils.log.Log;
import com.xenoage.zong.desktop.io.midi.out.MidiScoreDocFileOutput;
import com.xenoage.zong.desktop.io.musicxml.in.MusicXmlScoreDocFileInput;
import com.xenoage.zong.desktop.io.pdf.out.PdfScoreDocFileOutput;
import com.xenoage.zong.desktop.utils.JseZongPlatformUtils;
import com.xenoage.zong.documents.ScoreDoc;
import com.xenoage.zong.io.ScoreDocFactory;
import com.xenoage.zong.layout.Layout;
import com.xenoage.zong.layout.frames.ScoreFrame;
import com.xenoage.zong.musiclayout.stampings.NoteheadStamping;
import com.xenoage.zong.musiclayout.stampings.StaffSymbolStamping;
import com.xenoage.zong.musiclayout.stampings.Stamping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import static com.xenoage.utils.iterators.It.it;
import static com.xenoage.utils.jse.io.JseFileUtils.listFilesDeep;
import static com.xenoage.zong.musicxml.util.MusicXMLFilenameFilter.musicXMLFilenameFilter;
import static java.util.Comparator.comparing;

/**
 * This test tries to load and layout a huge range of MusicXML files.
 * <p>
 * If the files do not exist, nothing is tested. This class allows to test
 * a large number of files locally, which can not be uploaded to the
 * public repository because of copyright restrictions.
 * <p>
 * This test should be excluded from the normal test suite and
 * should only be started manually.
 *
 * @author Andreas Wenger
 */
public class MusicXmlMassTest {

	private static final String dir = "../../Zong-Test/";
	private static final String tempDir = "../../Zong-Test/Temp/";

	//configure the test: just load the MusicXML or do furter checks, like
	//layout checking or trying to save it in different formats and load it again?
	private static boolean checkLayout = true;
	private static boolean saveAsMxl = false; //not supported yet
	private static boolean saveAsPdf = true;
	private static boolean saveAsMid = true;
	private static boolean loadFromSavedMxl = saveAsMxl && true; //not supported yet


	@Before public void setUp() {
		//Logging
		JseZongPlatformUtils.init(getClass().getSimpleName());
		Log.init(new DesktopLogProcessing(getClass().getSimpleName()));
	}


	/**
	 * We check all .xml files in the {@link #dir} directory
	 * (and its subdirectories recursively).
	 * Files and folders containing "__" are ignored.
	 */
	@Test public void testSampleFiles() {
		int ok = 0;
		List<File> files = listFilesDeep(new File(dir), musicXMLFilenameFilter);
		//remove all files called "mei.xml", they include not MusicXML but MEI
		files.removeIf(f -> f.getName().equals("mei.xml"));
		//remove all files and directories with prefix "__"
		files.removeIf(f -> f.getAbsolutePath().contains("__"));
		//sort alphabetically by filepath
		files.sort(comparing(File::getAbsolutePath));
		System.out.println("Processing " + files.size() + " files...");
		It<File> filesIt = it(files);
		for (File file : filesIt) {
			if (testFile(file))
				ok++;
			else Assert.fail();
			if (filesIt.getIndex() % 10 == 0)
				System.out.println("Progress: " + (100 * filesIt.getIndex() / files.size()) + "%");
		}
		System.out.println("Could load " + ok + " of " + files.size() + " files (" +
				new DecimalFormat("#.##").format(100f * ok / files.size()) + "%)");
		if (ok < files.size())
			Assert.fail();
	}

	//*
	@Test public void testSingleFile() {
		File file = new File(dir + "MusicXML/hausmusik.ch/b/badarczewska-Thekla von (1834-1861)/gebet-einer-jungfrau/gebet-einer-jungfrau.xml");
		if (!testFile(file)) Assert.fail();
	} //*/

	private boolean testFile(File file) {
		try {

			//Load the file
			ScoreDocFactory.setErrorLayoutEnabled(false); //TIDY
			ScoreDoc score = new MusicXmlScoreDocFileInput().read(
					new JseInputStream(file), file.getAbsolutePath());
			ScoreDocFactory.setErrorLayoutEnabled(true); //TIDY

			//Check layout of loaded file
			if (checkLayout) {
				checkLayout(score, file.getName());
			}

			//Save it as MusicXML
			File mxlSavedFile = getTempOutputPath(file, "-saved.mxl");
			if (saveAsMxl) {
				//new MusicXMLScoreDocFileOutput().write(score, new FileOutputStream(mxlSavedFile), mxlSavedFile);
			}

			//Save it as PDF
			if (saveAsPdf) {
				File pdfFile = getTempOutputPath(file, ".pdf");
				new PdfScoreDocFileOutput().write(score, 0, new JseOutputStream(pdfFile));
			}

			//Save it as MIDI
			if (saveAsMid) {
				File midFile = getTempOutputPath(file, ".mid");
				new MidiScoreDocFileOutput().write(score, 0, new JseOutputStream(midFile));
			}

			//Load it from saved MusicXML
			if (loadFromSavedMxl) {
				//TODO
			}

			//Success
			System.out.print("OK:   " + file.toString().substring(dir.length()) + " (" +
					score.getScore().getInfo().getTitle() + ")");
			@SuppressWarnings("unchecked") List<String> errorMessages =
					(List<String>) score.getScore().getMetaData().get("mxlerrors");
			if (errorMessages != null)
				System.out.print("  ! " + errorMessages.size() + " warning(s)");
			System.out.println();

			return true;
		} catch (Throwable ex) {
			ex.printStackTrace();
			//fail("Failed to load file: " + file);
			System.out.println("fail: " + file.toString().substring(dir.length()));
			return false;
		}
	}

	/**
	 * Creates a filepath for an output file in the {@value #tempDir} directory,
	 * with the same relative path as the original file and the given extension (like ".pdf") added.
	 * If the parent directory for that file does not exist yet, it is created.
	 */
	private static File getTempOutputPath(File originalFile, String ext) {
		File file = new File(tempDir + originalFile.getPath().substring(dir.length()) + ext);
		file.getParentFile().mkdirs();
		return file;
	}

	/**
	 * Checks that the layout contains at least one score frame with at least
	 * one {@link NoteheadStamping}. Otherwise an {@link AssertionError} is thrown.
	 */
	private void checkLayout(ScoreDoc doc, String filename) {
		Layout layout = doc.getLayout();
		//at least one score frame?
		if (layout.getScoreFrames().size() == 0)
			throw new AssertionError("No score frames in layout");
		//at least one staff symbol stamping?
		if (false == filename.contains("[no notes]")) {
			boolean stampingFound = false;
			noteheadSearch:
			for (ScoreFrame scoreFrame : layout.getScoreFrames()) {
				for (Stamping stamping : scoreFrame.getScoreFrameLayout().getMusicalStampings()) {
					if (stamping instanceof NoteheadStamping || stamping instanceof StaffSymbolStamping) {
						stampingFound = true;
						break noteheadSearch;
					}
				}
			}
			if (false == stampingFound)
				throw new AssertionError("No staff symbol stamping found in layout");
		}
	}

}

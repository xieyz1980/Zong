package com.xenoage.zong.io.musicxml.in;

import static com.xenoage.utils.PlatformUtils.platformUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.xenoage.utils.PlatformUtils;
import com.xenoage.utils.annotations.NonNull;
import com.xenoage.utils.io.InputStream;
import com.xenoage.utils.io.ZipReader;
import com.xenoage.utils.xml.XmlReader;
import com.xenoage.zong.io.musicxml.FileType;
import com.xenoage.zong.io.musicxml.link.LinkAttributes;
import com.xenoage.zong.io.musicxml.opus.Opus;
import com.xenoage.zong.io.musicxml.opus.OpusItem;
import com.xenoage.zong.io.musicxml.opus.Score;

/**
 * This class reads a compressed MusicXML file.
 * 
 * Therefore, it is extracted in the main memory. Then,
 * the list of files (opus) or single files can be loaded.
 * When the class instance is cleaned up, the zip content
 * in the memory is cleaned up, too.
 * 
 * This class can also handle nested compressed MusicXML files,
 * as long as they contain a single score and not an opus.
 * 
 * @author Andreas Wenger
 */
public class CompressedFileInput {

	private OpusItem rootItem;


	/**
	 * Creates a {@link CompressedFileInput} instance for the given
	 * compressed MusicXML data.
	 */
	public CompressedFileInput(InputStream inputStream)
		throws IOException {
		
		//load zip contents
		ZipReader reader = platformUtils().createZipReader(inputStream);
		
		//parse META-INF/container.xml
		String rootfilePath = null;
		try {
			InputStream containerStream = reader.openFile("META-INF/container.xml");
			XmlReader containerReader = platformUtils().createXmlReader(containerStream);
			rootfilePath = readRootFilePath(containerReader);
			containerStream.close();
		} catch (Exception ex) {
			throw new IllegalStateException(
				"Compressed MusicXML file has no (well-formed) META-INF/container.xml", ex);
		}

		//load root file
		try {
			InputStream rootStream = reader.openFile(rootfilePath);
			FileType type = FileTypeReader.getFileType(rootStream);
			rootStream.close();
			if (type == null)
				throw new IllegalStateException("Unknown root file type");
			switch (type) {
				case Compressed:
					throw new IllegalStateException("Root file may (currently) not be compressed");
				case XMLOpus:
					rootItem = new OpusFileInput().readOpusFile(bis);
					break;
				case XMLScorePartwise:
				case XMLScoreTimewise:
					rootItem = new Score(new LinkAttributes(rootfilePath), null);
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Could not load root file", ex);
		}
	}
	
	@NonNull private String readRootFilePath(XmlReader containerReader)
		throws IOException {
		XmlReader r = containerReader;
		r.openNextChildElement(); //root element
		while (r.openNextChildElement()) {
			if (r.getElementName().equals("rootfiles")) { //rootfiles element
				while (r.openNextChildElement()) {
					if (r.getElementName().equals("rootfile")) { //rootfile element
						String fullPath = r.getAttribute("full-path");
						if (fullPath == null)
							throw new IOException("full-path of rootfile not found");
						return fullPath;
					}
					r.closeElement();
				}
			}
			r.closeElement();
		}
		throw new IOException("rootfile not found");
	}

	/**
	 * Gets the item which is the main document in this compressed
	 * MusicXML file: Either a {@link Score} or an {@link Opus}.
	 */
	public OpusItem getRootItem() {
		return rootItem;
	}

	/**
	 * Returns true, if the file does not only contain a single score but
	 * a whole opus.
	 */
	public boolean isOpus() {
		return (rootItem instanceof Opus);
	}

	/**
	 * Gets a (flattened) list of all filenames in this opus. If this file
	 * contains no opus but a single score, the filename of the single score
	 * is returned.
	 */
	public List<String> getScoreFilenames()
		throws IOException {
		LinkedList<String> ret = new LinkedList<String>();
		if (isOpus()) {
			getScoreFilenames(
				new OpusFileInput().resolveOpusLinks((Opus) rootItem, tempFolder.getAbsolutePath()), ret);
		}
		else {
			ret.add(((Score) rootItem).getHref());
		}
		return ret;
	}

	/**
	 * Loads and returns the {@link com.xenoage.zong.core.Score} at the given path.
	 */
	public com.xenoage.zong.core.Score loadScore(String path)
		throws InvalidFormatException, IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
			new File(tempFolder, path)));
		StreamUtils.markInputStream(bis);
		//XML or compressed?
		FileType fileType = FileTypeReader.getFileType(bis);
		bis.reset();
		if (fileType == null)
			throw new InvalidFormatException("Score has invalid format: " + path);
		switch (fileType) {
			case Compressed:
				return loadCompressedScore(path);
			case XMLScorePartwise:
				return new MusicXMLScoreFileInput().read(bis, path);
			case XMLScoreTimewise:
				throw new IllegalStateException("score-timewise is currently not implemented");
			default:
				throw new InvalidFormatException("Score has invalid format: " + path);
		}
	}

	private com.xenoage.zong.core.Score loadCompressedScore(String path)
		throws IOException {
		CompressedFileInput zip = new CompressedFileInput(new FileInputStream(
			new File(tempFolder, path)), osTempFolder);
		com.xenoage.zong.core.Score ret = zip.loadScore(((Score) zip.getRootItem()).getHref());
		zip.close();
		return ret;
	}

	private void getScoreFilenames(Opus resolvedOpus, LinkedList<String> acc) {
		for (OpusItem item : resolvedOpus.getItems()) {
			if (item instanceof Score)
				acc.add(((Score) item).getHref());
			else if (item instanceof Opus)
				getScoreFilenames((Opus) item, acc);
		}
	}

}
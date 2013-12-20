package com.xenoage.zong.io.musicxml.in;

import static com.xenoage.utils.PlatformUtils.platformUtils;
import static com.xenoage.utils.collections.CollectionUtils.alist;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.xenoage.utils.Parser;
import com.xenoage.utils.annotations.MaybeNull;
import com.xenoage.utils.exceptions.InvalidFormatException;
import com.xenoage.utils.io.InputStream;
import com.xenoage.utils.io.ZipReader;
import com.xenoage.utils.xml.XmlReader;
import com.xenoage.zong.io.musicxml.link.LinkAttributes;
import com.xenoage.zong.io.musicxml.opus.Opus;
import com.xenoage.zong.io.musicxml.opus.OpusItem;
import com.xenoage.zong.io.musicxml.opus.OpusLink;
import com.xenoage.zong.io.musicxml.opus.Score;

/**
 * This class reads a MusicXML 2.0 opus file.
 * 
 * @author Andreas Wenger
 */
public class OpusFileInput {

	/**
	 * Creates an {@link Opus} instance from the document
	 * behind the given {@link InputStream}.
	 */
	public Opus readOpusFile(InputStream inputStream)
		throws InvalidFormatException, IOException {
		XmlReader reader = platformUtils().createXmlReader(inputStream);
		//first element must be "opus"
		if (false == reader.openNextChildElement() || false == reader.getElementName().equals("opus"))
			throw new InvalidFormatException("No opus document");
		return readOpus(reader);
	}

	private Opus readOpus(XmlReader reader) {
		String title = null;
		List<OpusItem> items = alist();
		while (reader.openNextChildElement()) {
			String n = reader.getElementName();
			if (reader.getElementName().equals("title"))
				title = reader.getTextNotNull();
			else if (n.equals("opus"))
				items.add(readOpus(reader));
			else if (n.equals("opus-link"))
				items.add(readOpusLink(reader));
			else if (n.equals("score"))
				items.add(readScore(reader));
			reader.closeElement();
		}
		return new Opus(title, items);
	}

	private OpusLink readOpusLink(XmlReader reader) {
		String href = reader.getAttributeNotNull("href");
		return new OpusLink(new LinkAttributes(href));
	}

	private Score readScore(XmlReader reader) {
		String href = reader.getAttribute("href");
		Boolean newPage = Parser.parseBooleanNullYesNo(reader.getAttribute("new-page"));
		return new Score(new LinkAttributes(href), newPage);
	}

	/**
	 * Resolves all {@link OpusLink} items within the given {@link Opus} to
	 * instances of {@link Opus} and returns the result.
	 * For a compressed MusicXML file, a {@link ZipReader} has to be given, otherwise
	 * the given base path is used.
	 */
	public Opus resolveOpusLinks(Opus opus, @MaybeNull ZipReader zip, @MaybeNull String basePath)
		throws InvalidFormatException, IOException {
		List<OpusItem> resolvedItems = alist();
		for (OpusItem item : opus.getItems()) {
			OpusItem resolvedItem = item;
			if (item instanceof OpusLink) {
				String filePath = ((OpusLink) item).getLink().getHref();
				InputStream opusStream = null;
				if (zip != null)
					opusStream = zip.openFile(filePath);
				else if (basePath != null)
					opusStream = platformUtils().openFile(basePath + "/" + filePath);
				else
					throw new IOException("neither zip nor basePath is given");
				if (opusStream == null)
					throw new FileNotFoundException(filePath);
				Opus newOpus = readOpusFile(opusStream);
				resolvedItem = resolveOpusLinks(newOpus, zip, basePath);
			}
			resolvedItems.add(resolvedItem);
		}
		return new Opus(opus.getTitle(), resolvedItems);
	}

}

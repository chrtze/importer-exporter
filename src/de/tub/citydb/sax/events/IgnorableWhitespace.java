package de.tub.citydb.sax.events;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public final class IgnorableWhitespace implements SAXEvent {
	private final char[] ch;
	private final DocumentLocation documentLocation;

	public IgnorableWhitespace(char[] ch, int start, int length, DocumentLocation documentLocation) {
		// make a copy of the char array ch.
		// we do not want to have a reference to potentially large arrays

		this.ch = new char[length];
		System.arraycopy(ch, start, this.ch, 0, length);
		this.documentLocation = documentLocation;
	}

	@Override
	public void send(ContentHandler contentHandler) throws SAXException {
		contentHandler.ignorableWhitespace(ch, 0, ch.length);
	}
	
	@Override
	public DocumentLocation getDocumentLocation() {
		return documentLocation;
	}

}

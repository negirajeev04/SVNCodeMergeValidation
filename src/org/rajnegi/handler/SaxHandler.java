package org.rajnegi.handler;

import java.util.ArrayList;
import java.util.List;

import org.rajnegi.SVNCodeMergeValidation;
import org.rajnegi.vo.FilePath;
import org.rajnegi.vo.Revision;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {
	public SaxHandler() {
	}

	boolean logentry = false;
	boolean author = false;
	boolean date = false;
	boolean path = false;
	boolean comments = false;
	boolean ignoreRevision = false;
	private StringBuilder fileChars = new StringBuilder();

	List<Revision> revList = null;
	Revision rev = null;
	FilePath filePath = null;

	public List<Revision> getRevisionList() {
		return revList;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("logentry")) {
			logentry = true;

			rev = new Revision();
			ignoreRevision = false;
			String revisionNumber = attributes.getValue("revision");
			rev.setRevision(revisionNumber);
			if (revList == null) {
				revList = new ArrayList();
			}
		}

		if (qName.equalsIgnoreCase("author")) {
			author = true;
		}

		if (qName.equalsIgnoreCase("date")) {
			date = true;
		}

		if (qName.equalsIgnoreCase("path")) {
			path = true;
			String action = attributes.getValue("action");
			String copyRev = attributes.getValue("copyfrom-rev");
			if (("A".equalsIgnoreCase(action)) && (copyRev != null)) {
				ignoreRevision = true;
			}
			filePath = new FilePath();
			fileChars.setLength(0);
		}

		if (qName.equalsIgnoreCase("msg")) {
			comments = true;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ((qName.equalsIgnoreCase("logentry")) && (!ignoreRevision)) {
			revList.add(rev);
		}
		if (qName.equalsIgnoreCase("path")) {
			String content = fileChars.toString();
			filePath.setFilePath(getRelativePath(content));
			rev.addFile(content);
			rev.addFilePath(filePath);
			path = false;
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (logentry) {
			logentry = false;
		}

		if (author) {
			author = false;
			rev.setAuthor(new String(ch, start, length));
		}

		if (date) {
			date = false;
			rev.setModifiedDate(new String(ch, start, length));
		}

		if (path) {
			fileChars.append(ch, start, length);
		}

		if (comments) {
			comments = false;
			rev.setComments(new String(ch, start, length));
		}
	}

	private static String getRelativePath(String path) {
		String returnPath = "";
		int startIndex = 0;

		for (String str : path.split("/")) {
			str = str.trim();

			if ((!"".equalsIgnoreCase(str)) && (!ignoreKey(str))) {
				startIndex = path.indexOf(str);
				break;
			}
		}
		returnPath = path.substring(startIndex);
		if (returnPath.indexOf("/") != 0) {
			returnPath = "/" + returnPath;
		}

		return returnPath;
	}

	private static boolean ignoreKey(String folderName) {
		String keysToIgnore = SVNCodeMergeValidation.getIgnoreKeysFromName();

		if (keysToIgnore.length() != 0) {
			for (String key : keysToIgnore.split(",")) {
				key = key.trim();
				if (folderName.indexOf(key) != -1) {
					return true;
				}
			}
		}

		return false;
	}
}

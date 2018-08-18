package org.rajnegi.vo;

import java.util.List;

public class Revision {
	
	private String revision;
	private String author;
	private String modifiedDate;
	private List<String> filesPath;
	private boolean isDiscrepant;
	private String discrepantTags;
	private List<FilePath> filePathList;
	private String comments;

	public Revision() {
	}

	public String toString() {
		return revision + " ; " + author + " ; " + modifiedDate + " ; " + filePathList;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public void setFilePath(List<String> filesPath) {
		this.filesPath = filesPath;
	}

	public void addFile(String file) {
		if (filesPath == null) {
			filesPath = new java.util.ArrayList();
		}
		filesPath.add(file);
	}

	public void addFilePath(FilePath filePath) {
		if (filePathList == null) {
			filePathList = new java.util.ArrayList();
		}
		filePathList.add(filePath);
	}

	public boolean isDiscrepant() {
		return isDiscrepant;
	}

	public void setDiscrepant(boolean isDiscrepant) {
		this.isDiscrepant = isDiscrepant;
	}

	public String getDiscrepantTags() {
		return discrepantTags;
	}

	public void setDiscrepantTags(String discrepantTags) {
		this.discrepantTags = discrepantTags;
	}

	public List<FilePath> getFilePathList() {
		return filePathList;
	}

	public void setFilePathList(List<FilePath> filePathList) {
		this.filePathList = filePathList;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}

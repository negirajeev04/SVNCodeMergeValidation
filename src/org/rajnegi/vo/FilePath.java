package org.rajnegi.vo;

public class FilePath {
	private String filePath;
	private boolean isDiscrepant;

	public FilePath() {
	}

	public String toString() {
		return filePath;
	}

	public boolean isDiscrepant() {
		return isDiscrepant;
	}

	public void setDiscrepant(boolean isDiscrepant) {
		this.isDiscrepant = isDiscrepant;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}

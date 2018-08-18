package org.rajnegi.vo;

import java.util.List;

public class SVNTag {
	private String repository;
	private String dependency;
	private String url;
	private List<Revision> revisionList;
	private boolean isDiscrepant;

	public SVNTag() {
	}

	public String toString() {
		return repository + " ; " + dependency + " ; " + url;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getDependency() {
		return dependency;
	}

	public void setDependency(String dependency) {
		this.dependency = dependency;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<Revision> getRevisionList() {
		return revisionList;
	}

	public void setRevisionList(List<Revision> revisionList) {
		this.revisionList = revisionList;
	}

	public boolean isDiscrepant() {
		return isDiscrepant;
	}

	public void setDiscrepant(boolean isDiscrepant) {
		this.isDiscrepant = isDiscrepant;
	}
}

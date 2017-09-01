package org.ybuka.confluence.plugins.sitemap;

import java.util.List;

public class SiteMapConfigBean {

	private String fileLocation;
	private String template;
	private String changefreq;
	private String priority;
	private boolean includePersonalSpaces;
	private boolean includeArchivedSpaces;
	private List<String> spaces;
	private List<String> categories;

	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getChangefreq() {
		return changefreq;
	}

	public void setChangefreq(String changefreq) {
		this.changefreq = changefreq;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public boolean isIncludePersonalSpaces() {
		return includePersonalSpaces;
	}

	public void setIncludePersonalSpaces(boolean includePersonalSpaces) {
		this.includePersonalSpaces = includePersonalSpaces;
	}

	public boolean isIncludeArchivedSpaces() {
		return includeArchivedSpaces;
	}

	public void setIncludeArchivedSpaces(boolean includeArchivedSpaces) {
		this.includeArchivedSpaces = includeArchivedSpaces;
	}

	public List<String> getSpaces() {
		return spaces;
	}

	public void setSpaces(List<String> spaces) {
		this.spaces = spaces;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
	
}

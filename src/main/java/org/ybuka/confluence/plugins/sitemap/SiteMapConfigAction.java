package org.ybuka.confluence.plugins.sitemap;

import java.util.List;

import org.ybuka.confluence.plugins.sitemap.SiteMapComponent.TaskStatus;

public class SiteMapConfigAction extends SiteMapAdminAction {
	private static final long serialVersionUID = 1L;

	private SiteMapConfigBean configBean;

	public SiteMapConfigAction() {
		configBean = new SiteMapConfigBean();
	}

	@Override
	public String execute() throws Exception {
		configBean = siteMapGenerator.getConfiguration();
		return SUCCESS;
	}

	public String save() {
		siteMapGenerator.setConfiguration(configBean);
		return SUCCESS;
	}

	public TaskStatus getStatus() {
		return siteMapGenerator.getStatus();
	}

	public String getFileLocation() {
		return configBean.getFileLocation();
	}

	public void setFileLocation(String fileLocation) {
		configBean.setFileLocation(fileLocation);
	}

	public String getTemplate() {
		return configBean.getTemplate();
	}

	public void setTemplate(String template) {
		configBean.setTemplate(template);
	}

	public String getChangefreq() {
		return configBean.getChangefreq();
	}

	public void setChangefreq(String changefreq) {
		configBean.setChangefreq(changefreq);
	}

	public String getPriority() {
		return configBean.getPriority();
	}

	public void setPriority(String priority) {
		configBean.setPriority(priority);
	}

	public boolean isIncludePersonalSpaces() {
		return configBean.isIncludePersonalSpaces();
	}

	public void setIncludePersonalSpaces(boolean includePersonalSpaces) {
		configBean.setIncludePersonalSpaces(includePersonalSpaces);
	}

	public boolean isIncludeArchivedSpaces() {
		return configBean.isIncludeArchivedSpaces();
	}

	public void setIncludeArchivedSpaces(boolean includeArchivedSpaces) {
		configBean.setIncludeArchivedSpaces(includeArchivedSpaces);
	}

	public List<String> getSpaces() {
		return configBean.getSpaces();
	}

	public void setSpaces(List<String> spaces) {
		configBean.setSpaces(spaces);
	}

}

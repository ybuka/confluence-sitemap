package org.ybuka.confluence.plugins.sitemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

	public String getSpaces() {
		return merge( configBean.getSpaces() );
	}

	public void setSpaces(String spaces) {
		configBean.setSpaces(split(spaces));
	}

	public String getCategories() {
		return merge(configBean.getCategories());
	}

	public void setCategories(String categories) {
		configBean.setCategories(split(categories));
	}

	private List<String> split(String str) {
		List<String> result = null;
		if (StringUtils.isNotBlank(str)) {
			
			String[] values = str.split(" ");
			result = new ArrayList<>();
			
			for (String v : values) {
				if (StringUtils.isNotBlank(v)) {
					result.add(v);
				}
			}
		} else {
			result = Collections.emptyList();
		}
		return result;
	}
	
	private String merge(List<String> strs){
		StringBuilder result = new StringBuilder();
		if(strs!=null && !strs.isEmpty() ){
			for(String s : strs){
				result.append(s).append(" ");
			}
		}		
		return result.toString();
	}

}

package org.ybuka.confluence.plugins.sitemap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermission;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.SpaceStatus;
import com.atlassian.confluence.spaces.SpacesQuery;
import com.atlassian.confluence.spaces.SpacesQuery.Builder;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

public class SiteMapComponent {

	private static final Logger log = LoggerFactory.getLogger(SiteMapComponent.class);

	public static final String BANDANA_CONTEXT = "org.ybuka.confluence.plugins.sitemap";
	public static final String BANDANA_KEY = "site-map-config";
	public static final String BANDANA_KEY_LAST_EXECUTION = "last-execution";

	protected static final DateFormat SITEMAP_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	protected static final DateFormat SIMPLE_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final SpaceManager spaceManager;
	private final PageManager pageManager;
	private final PermissionManager permissionManager;
	private final SettingsManager settingsManager;
	private final ApplicationProperties applicationProperties;
	private final BandanaManager bandanaManager;	
	private final TransactionTemplate transactionTemplate;
	
	private TaskStatus status;
	private SiteMapConfigBean siteMapConfiguration;

	public SiteMapComponent(SpaceManager spaceManager, PageManager pageManager, PermissionManager permissionManager, SettingsManager settingsManager,
			ApplicationProperties applicationProperties, TransactionTemplate transactionTemplate, BandanaManager bandanaManager) {
		super();
		this.spaceManager = spaceManager;
		this.pageManager = pageManager;

		this.permissionManager = permissionManager;
		this.settingsManager = settingsManager;
		this.applicationProperties = applicationProperties;
		this.transactionTemplate = transactionTemplate;
		this.bandanaManager = bandanaManager;

	}

	public void generateSiteMap() {
		List<SiteMapEntry> items = prepareEntries();
		saveSiteMap(items);
	}

	@SuppressWarnings("unchecked")
	private List<SiteMapEntry> prepareEntries() {
		updateSiteMapConfiguration();
		return (List<SiteMapEntry>) transactionTemplate.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction() {
				List<SiteMapEntry> itemsList = generateSiteMapEntries();
				return itemsList;
			}
		});

	}

	public TaskStatus getStatus() {
		return status;
	}

	public TaskStatus getLastFinishedStatus() {
		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		Object tmp = bandanaManager.getValue(context, BANDANA_KEY_LAST_EXECUTION);
		TaskStatus ts = (tmp != null) ? (TaskStatus) tmp : null;
		return ts;
	}

	private List<SiteMapEntry> generateSiteMapEntries() {

		List<SiteMapEntry> itemsList = new ArrayList<>(20000);
		status = new TaskStatus();
		status.setStarted(System.currentTimeMillis());
		SiteMapConfigBean c = getConfiguration();

		String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
		itemsList = new ArrayList<SiteMapComponent.SiteMapEntry>();

		List<Space> allSpaces = spaceManager.getAllSpaces(createSpaceQueryBuilder().build());
		status.setSpaceCount(allSpaces.size());

		log.info("Spaces count " + allSpaces.size());
		long rememCount = 0;

		for (Space space : allSpaces) {
			rememCount = itemsList.size();
			log.info("Space {} in process ", space.getKey());
			List<Page> pages = pageManager.getPages(space, true);
			for (Page ps : pages) {

				if (permissionManager.hasPermission(null, Permission.VIEW, ps)) {
					itemsList.add(new SiteMapEntry(baseUrl + ps.getUrlPath(), ps.getLastModificationDate(), c.getChangefreq(), c.getPriority()));
					status.itemsAdded++;
				}
				status.itemsProcessed++;
			}

			for (BlogPost blog : pageManager.getBlogPosts(space, false)) {
				if (permissionManager.hasPermission(null, Permission.VIEW, blog)) {
					itemsList.add(new SiteMapEntry(baseUrl + blog.getUrlPath(), blog.getLastModificationDate(), c.getChangefreq(), c.getPriority()));
					status.itemsAdded++;
				}
				status.itemsProcessed++;
			}
			log.info("Space {} completed. Processed {} items , added {} ", space.getKey(), pages.size(), itemsList.size() - rememCount);
			status.spaceProcesed++;
		}
		return itemsList;
	}

	private void updateSiteMapConfiguration() {

		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		Object obj = this.bandanaManager.getValue(context, BANDANA_KEY);
		if (obj == null) {
			siteMapConfiguration = getDefaultConfiguration();
			bandanaManager.setValue(context, BANDANA_KEY, siteMapConfiguration);
		} else {
			siteMapConfiguration = (SiteMapConfigBean) obj;
		}
	}

	private Builder createSpaceQueryBuilder() {

		SiteMapConfigBean c = siteMapConfiguration;
		Builder builder = SpacesQuery.newQuery();

		if (c.getSpaces() != null && !c.getSpaces().isEmpty()) {
			builder = builder.withSpaceKeys(c.getSpaces());
		} else {

			if (!c.isIncludePersonalSpaces()) {
				builder = builder.withSpaceType(com.atlassian.confluence.spaces.SpaceType.GLOBAL);
			}

			if (!c.isIncludeArchivedSpaces()) {
				builder = builder.withSpaceStatus(SpaceStatus.CURRENT);
			}
		}

		builder = builder.withPermission(SpacePermission.VIEWSPACE_PERMISSION).forUser(null);

		return builder;
	}

	public SiteMapConfigBean getConfiguration() {
		updateSiteMapConfiguration();
		return siteMapConfiguration;
	}

	public void setConfiguration(SiteMapConfigBean siteMapConfiguration) {
		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		bandanaManager.setValue(context, BANDANA_KEY, siteMapConfiguration);
	}

	private SiteMapConfigBean getDefaultConfiguration() {

		SiteMapConfigBean c = new SiteMapConfigBean();
		c.setFileLocation("{confluence_home}/sitemap.xml");
		c.setChangefreq("weekly");
		c.setPriority("0.7");
		c.setIncludeArchivedSpaces(false);
		c.setIncludePersonalSpaces(false);
		return c;
	}

	private String retrieveAbsoluteFileLocation(SiteMapConfigBean cb) {
		String homeDir = applicationProperties.getHomeDirectory().getAbsolutePath();
		if (homeDir.endsWith(File.separator)) {
			homeDir = homeDir.substring(0, homeDir.length() - 1);
		}
		return cb.getFileLocation().replace("{confluence_home}", homeDir);
	}

	public String retrieveAbsoluteFileLocation() {
		return retrieveAbsoluteFileLocation(this.getConfiguration());
	}

	private void saveSiteMap(List<SiteMapEntry> itemsList) {
		Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
		contextMap.put("items", itemsList);
		File f = new File(retrieveAbsoluteFileLocation(siteMapConfiguration));
		FileWriter w = null;
		try {
			f.createNewFile();
			w = new FileWriter(f);
			VelocityUtils.writeRenderedTemplate(w, "templates/sitemap.vm", contextMap);
			w.flush();
		} catch (IOException e) {
			log.error(e.toString(), e);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) {
				}
			}
		}
		status.setEnded(System.currentTimeMillis());
		bandanaManager.setValue(new ConfluenceBandanaContext(BANDANA_CONTEXT), BANDANA_KEY_LAST_EXECUTION, status);
		status=null;
	}

	public class SiteMapEntry {
		private String url;
		private String lastmod;
		private String changefreq;
		private String priority;
		
		public SiteMapEntry() {

		}

		public SiteMapEntry(String url, String lastmod, String changefreq, String priority) {

			this.url = url;
			this.lastmod = lastmod;
			this.changefreq = changefreq;
			this.priority = priority;
		}

		public SiteMapEntry(String url, Date lastmod, String changefreq, String priority) {
			this.url = url;
			String dateAsText = SITEMAP_DATEFORMAT.format(lastmod);
			this.lastmod = dateAsText.substring(0, 22) + ":" + dateAsText.substring(22);
			this.changefreq = changefreq;
			this.priority = priority;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getLastmod() {
			return lastmod;
		}

		public void setLastmod(String lastmod) {
			this.lastmod = lastmod;
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

	}

	public static class TaskStatus implements Cloneable {

		protected long started;
		protected long ended = -1;
		protected long spaceCount = 0;
		protected long spaceProcesed = 0;
		protected long itemsProcessed = 0;
		protected long itemsAdded = 0;

		@Override
		protected TaskStatus clone() throws CloneNotSupportedException {
			TaskStatus clone = new TaskStatus();
			clone.started = started;
			clone.spaceCount = spaceCount;
			clone.spaceProcesed = spaceProcesed;
			clone.itemsProcessed = itemsProcessed;
			clone.itemsAdded = itemsAdded;
			clone.ended = ended;
			return clone;
		}

		@Override
		public String toString() {
			return "TaskStatus [started=" + started + ", ended=" + ended + ", spaceCount=" + spaceCount + ", spaceProcesed=" + spaceProcesed + ", itemsProcessed=" + itemsProcessed
					+ ", itemsAdded=" + itemsAdded + "]";
		}

		public String asString() {

			String finished = (ended > 0) ? SIMPLE_DATEFORMAT.format(ended) : " --- ";

			return "Started=" + SIMPLE_DATEFORMAT.format(started) + ", Finished=" + finished + ", Spaces Count=" + spaceCount + ", Spaces processed=" + spaceProcesed
					+ ", Items processed=" + itemsProcessed + ", Items added=" + itemsAdded;
		}

		public long getStarted() {
			return started;
		}

		public void setStarted(long started) {
			this.started = started;
		}

		public long getSpaceCount() {
			return spaceCount;
		}

		public void setSpaceCount(long spaceCount) {
			this.spaceCount = spaceCount;
		}

		public long getItemsProcessed() {
			return itemsProcessed;
		}

		public void setItemsProcessed(long pagesProcessed) {
			this.itemsProcessed = pagesProcessed;
		}

		public long getSpaceProcesed() {
			return spaceProcesed;
		}

		public void setSpaceProcesed(long spaceFinished) {
			this.spaceProcesed = spaceFinished;
		}

		public long getItemsAdded() {
			return itemsAdded;
		}

		public void setItemsAdded(long itemsAdded) {
			this.itemsAdded = itemsAdded;
		}

		public long getEnded() {
			return ended;
		}

		public void setEnded(long ended) {
			this.ended = ended;
		}
	}
}

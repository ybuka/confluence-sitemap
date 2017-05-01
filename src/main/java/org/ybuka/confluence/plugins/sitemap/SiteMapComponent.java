package org.ybuka.confluence.plugins.sitemap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
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
	public static final String BANDANA_CONF_KEY = "site-map-config";
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
	private SitemapGenerator generator;

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

		transactionTemplate.execute(new TransactionCallback<Object>() {

			@Override
			public Object doInTransaction() {
				generateSiteMapInt();
				return null;
			}
		});

	}

	public synchronized TaskStatus getStatus() {
		TaskStatus ts = null;
		if (getSitemapGenerator() != null) {
			ts = getSitemapGenerator().taskStatus.clone();
		}

		return ts;
	}

	public TaskStatus getLastFinishedStatus() {
		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		Object tmp = bandanaManager.getValue(context, BANDANA_KEY_LAST_EXECUTION);
		TaskStatus ts = (tmp != null) ? (TaskStatus) tmp : null;
		return ts;
	}

	public String retrieveOutputFileLocation() {
		return retrieveOutFilePath(this.getConfiguration());
	}

	public SiteMapConfigBean getConfiguration() {
		SiteMapConfigBean result = null;
		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		Object obj = this.bandanaManager.getValue(context, BANDANA_CONF_KEY);
		if (obj == null) {
			result = getDefaultConfiguration();
			bandanaManager.setValue(context, BANDANA_CONF_KEY, result);
		} else {
			result = (SiteMapConfigBean) obj;
		}
		return result;
	}

	public void setConfiguration(SiteMapConfigBean siteMapConfiguration) {
		ConfluenceBandanaContext context = new ConfluenceBandanaContext(BANDANA_CONTEXT);
		bandanaManager.setValue(context, BANDANA_CONF_KEY, siteMapConfiguration);
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

	private void generateSiteMapInt() {
		try {
			createNewGenerator();
			getSitemapGenerator().generateSitemap();

			File f = new File(retrieveOutputFileLocation());
			if (f.exists()) {
				log.info("Remove old sitemap file '" + f.getAbsolutePath() +"'");
				f.delete();
			}

			if (!getSitemapGenerator().outFile.renameTo(f)) {				
				throw new IOException("Could not rename to file '" + f.getAbsolutePath() + "'");
			}else{
				log.info("Save sitemap file '" + f.getAbsolutePath() +"'");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			getSitemapGenerator().taskStatus.setErrorMssg(e.getMessage());
		} finally {
			getSitemapGenerator().taskStatus.setEnded(System.currentTimeMillis());
			TaskStatus ts = getSitemapGenerator().taskStatus.clone();
			killSitemapGenerator();
			bandanaManager.setValue(new ConfluenceBandanaContext(BANDANA_CONTEXT), BANDANA_KEY_LAST_EXECUTION, ts);

		}

	}

	private String retrieveOutFilePath(SiteMapConfigBean cb) {
		String homeDir = applicationProperties.getHomeDirectory().getAbsolutePath();
		if (homeDir.endsWith(File.separator)) {
			homeDir = homeDir.substring(0, homeDir.length() - 1);
		}
		return cb.getFileLocation().replace("{confluence_home}", homeDir);
	}

	private File createNewFile(String path) throws IOException {
		File f = new File(path);
		if (f.exists()) {
			throw new IOException("File '" + path + "' already exist");
		}
		File parent = f.getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new IOException("Could not create directory '" + parent.getAbsolutePath() + "'");
			}
			if ( ! f.createNewFile() ){
				throw new IOException("Could not create file '" + f.getAbsolutePath() + "'");
			}
		}
		return f;
	}

	private synchronized SitemapGenerator getSitemapGenerator() {
		return generator;
	}

	private synchronized void killSitemapGenerator() {
		generator = null;
	}

	private synchronized void createNewGenerator() throws IllegalStateException {
		if (generator != null) {
			throw new IllegalStateException("Could not create new instance of SiteMapComponent.SitemapGenerator -  already exist. st-hd");
		}
		SiteMapConfigBean configBean = getConfiguration();
		generator = new SitemapGenerator(configBean);
	}

	class SitemapGenerator {
		private final SiteMapConfigBean config;
		private TaskStatus taskStatus;
		private SiteMapWriter writer;
		private File outFile;

		public SitemapGenerator(SiteMapConfigBean config) {
			this.config = config;
			taskStatus = new TaskStatus();
			taskStatus.setStarted(System.currentTimeMillis());
		}

		public void generateSitemap() throws Exception {
			try {
				init();
				writer.writeXMLHeader();
				writeSitemapEntries();
				writer.writeXMLEnd();

			} finally {
				if (writer != null) {
					writer.closeWriter();
				}
			}
		}

		private void init() throws Exception {
			outFile = createTemporaryOutFile();
			FileWriter fw = new FileWriter(outFile);
			writer = new SiteMapWriter(fw);
		}

		private File createTemporaryOutFile() throws IOException {
			File f = new File(retrieveOutFilePath(config));
			// create tmp file in the same directory as output
			File tmpFile = new File(f.getParentFile(), "sitemap_tmp_" + System.currentTimeMillis() + ".xml");
			createNewFile(tmpFile.getAbsolutePath());
			return tmpFile;
		}

		private void writeSitemapEntries() throws IOException {

			String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();

			List<Space> spaces = spaceManager.getAllSpaces(createSpaceQueryBuilder().build());
			taskStatus.setSpaceCount(spaces.size());

			log.info("Spaces count " + spaces.size());
			long rememItemsProcessed = 0;
			long rememItemsAdded = 0;

			for (Space space : spaces) {
				rememItemsProcessed = taskStatus.itemsProcessed;
				rememItemsAdded = taskStatus.itemsAdded;
				log.info("Space {} in process ", space.getKey());
				List<Page> pages = pageManager.getPages(space, true);
				for (Page ps : pages) {

					if (permissionManager.hasPermission(null, Permission.VIEW, ps)) {
						writer.write(new SiteMapEntry(baseUrl + ps.getUrlPath(), ps.getLastModificationDate(), config.getChangefreq(), config.getPriority()));
						taskStatus.itemsAdded++;
					}
					taskStatus.itemsProcessed++;
				}

				for (BlogPost blog : pageManager.getBlogPosts(space, false)) {
					if (permissionManager.hasPermission(null, Permission.VIEW, blog)) {
						writer.write(new SiteMapEntry(baseUrl + blog.getUrlPath(), blog.getLastModificationDate(), config.getChangefreq(), config.getPriority()));
						taskStatus.itemsAdded++;
					}
					taskStatus.itemsProcessed++;
				}

				log.info("Space {} completed. Processed {} items , added {} ", space.getKey(), taskStatus.itemsProcessed - rememItemsProcessed,
						taskStatus.itemsAdded - rememItemsAdded);
				taskStatus.spaceProcesed++;
			}
			taskStatus.setEnded(System.currentTimeMillis());
		}

		/**
		 * Create query builder based on configuration
		 * 
		 * @return
		 */
		private Builder createSpaceQueryBuilder() {

			Builder builder = SpacesQuery.newQuery();

			if (config.getSpaces() != null && !config.getSpaces().isEmpty()) {
				builder = builder.withSpaceKeys(config.getSpaces());
			} else {

				if (!config.isIncludePersonalSpaces()) {
					builder = builder.withSpaceType(com.atlassian.confluence.spaces.SpaceType.GLOBAL);
				}

				if (!config.isIncludeArchivedSpaces()) {
					builder = builder.withSpaceStatus(SpaceStatus.CURRENT);
				}
			}

			builder = builder.withPermission(SpacePermission.VIEWSPACE_PERMISSION).forUser(null);

			return builder;
		}

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

	public static class TaskStatus implements Cloneable, Serializable {
		private static final long serialVersionUID = 1L;

		protected long started;
		protected long ended = -1;
		protected long spaceCount = 0;
		protected long spaceProcesed = 0;
		protected long itemsProcessed = 0;
		protected long itemsAdded = 0;
		// Not null is required for Bandana Context serialization
		protected String errorMssg = "";

		protected TaskStatus clone() {
			TaskStatus clone = new TaskStatus();
			clone.started = started;
			clone.spaceCount = spaceCount;
			clone.spaceProcesed = spaceProcesed;
			clone.itemsProcessed = itemsProcessed;
			clone.itemsAdded = itemsAdded;
			clone.ended = ended;
			clone.errorMssg = errorMssg;
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

		public String getErrorMssg() {
			return errorMssg;
		}

		public void setErrorMssg(String errorMssg) {
			this.errorMssg = errorMssg;
		}
	}

	class SiteMapWriter {

		private static final int BUFFER_SIZE = 3;
		private OutputStreamWriter writer;
		private Map<String, Object> contextMap;
		private List<SiteMapEntry> buffer;

		public SiteMapWriter(OutputStreamWriter writer) throws Exception {
			this.writer = writer;
			contextMap = MacroUtils.defaultVelocityContext();
			buffer = new ArrayList<>(BUFFER_SIZE);
		}

		public void writeXMLHeader() {
			VelocityUtils.writeRenderedTemplate(writer, "templates/sitemap-header.vm", contextMap);
		}

		public void writeXMLEnd() throws IOException {
			flushBuffer();
			// write xml end
			VelocityUtils.writeRenderedContent(writer, (CharSequence) "</urlset>", contextMap);

		}

		public void write(SiteMapEntry siteMapEntry) throws IOException {
			buffer.add(siteMapEntry);
			if (buffer.size() == BUFFER_SIZE) {
				flushBuffer();
			}
		}

		public void flushBuffer() throws IOException {
			contextMap.put("items", buffer);
			VelocityUtils.writeRenderedTemplate(writer, "templates/sitemap-entries.vm", contextMap);
			writer.flush();
			buffer.clear();
		}

		public void closeWriter() throws IOException {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
}

package de.tub.citydb.config.internal;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ResourceBundle;

public class Internal {
	public static final int ORACLE_MAX_BATCH_SIZE = 65535;
	public static ResourceBundle I18N;	
	
	private String userDir = System.getProperty("user.home") + File.separator + "3DCityDB-Importer-Exporter";	
	private String currentGmlIdCodespace = "";
	private String currentDbPassword = "";
	private String dbSrsName = "urn:ogc:def:crs,crs:EPSG:6.12:3068,crs:EPSG:6.12:5783";
	private int dbSrid = 0;
	private DBVersioning dbVersioning = DBVersioning.OFF;
	private String gmlNameDelimiter = " --/\\-- ";
	private String exportPath = "";
	private String exportTextureFilePath = "";
	private String importPath = "";
	private String exportFileName = "";
	private String importFileName = "";
	private String currentImportFileName = "";
	private String locale = "de";
	private String configPath =  userDir + File.separator + "config";
	private String configProject = "project.xml";
	private String configGui = "gui.xml";
	private String logPath = userDir + File.separator + "log";
	private boolean dbIsConnected = false;
	private boolean useXMLValidation = false;
	private boolean useInternalBBoxFilter = false;
	private String currentLogPath = "";
	
	private PropertyChangeSupport changes = new PropertyChangeSupport(this);

	public Internal() {
	}

	public String getUserDir() {
		return userDir;
	}

	public void setUserDir(String userDir) {
		this.userDir = userDir;
	}

	public String getCurrentGmlIdCodespace() {
		return currentGmlIdCodespace;
	}

	public void setCurrentGmlIdCodespace(String currentGmlIdCodespace) {
		this.currentGmlIdCodespace = currentGmlIdCodespace;
	}

	public String getCurrentDbPassword() {
		return currentDbPassword;
	}

	public void setCurrentDbPassword(String currentDbPassword) {
		this.currentDbPassword = currentDbPassword;
	}

	public String getDbSrsName() {
		return dbSrsName;
	}

	public void setDbSrsName(String dbSrsName) {
		this.dbSrsName = dbSrsName;
	}

	public int getDbSrid() {
		return dbSrid;
	}

	public void setDbSrid(int dbSrid) {
		this.dbSrid = dbSrid;
	}

	public DBVersioning getDbVersioning() {
		return dbVersioning;
	}

	public void setDbVersioning(DBVersioning dbVersioning) {
		this.dbVersioning = dbVersioning;
	}

	public String getExportPath() {
		return exportPath;
	}

	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	public String getImportPath() {
		return importPath;
	}

	public void setImportPath(String importPath) {
		this.importPath = importPath;
	}

	public String getExportFileName() {
		return exportFileName;
	}

	public void setExportFileName(String exportFileName) {
		this.exportFileName = exportFileName;
	}

	public String getImportFileName() {
		return importFileName;
	}

	public void setImportFileName(String importFileName) {
		this.importFileName = importFileName;
	}

	public String getCurrentImportFileName() {
		return currentImportFileName;
	}

	public void setCurrentImportFileName(String currentImportFileName) {
		this.currentImportFileName = currentImportFileName;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public boolean isDbIsConnected() {
		return dbIsConnected;
	}

	public void setDbIsConnected(boolean dbIsConnected) {
		boolean oldDbIsConnected = this.dbIsConnected;
		this.dbIsConnected = dbIsConnected;
		changes.firePropertyChange("internal.dbIsConnected", oldDbIsConnected, dbIsConnected);
	}

	public String getExportTextureFilePath() {
		return exportTextureFilePath;
	}

	public void setExportTextureFilePath(String exportTextureFilePath) {
		this.exportTextureFilePath = exportTextureFilePath;
	}

	public String getGmlNameDelimiter() {
		return gmlNameDelimiter;
	}

	public void setGmlNameDelimiter(String gmlNameDelimiter) {
		this.gmlNameDelimiter = gmlNameDelimiter;
	}

	public String getConfigProject() {
		return configProject;
	}

	public void setConfigProject(String configProject) {
		this.configProject = configProject;
	}

	public String getConfigGui() {
		return configGui;
	}

	public void setConfigGui(String configGui) {
		this.configGui = configGui;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public boolean isUseXMLValidation() {
		return useXMLValidation;
	}

	public void setUseXMLValidation(boolean useXMLValidation) {
		this.useXMLValidation = useXMLValidation;
	}

	public boolean isUseInternalBBoxFilter() {
		return useInternalBBoxFilter;
	}

	public void setUseInternalBBoxFilter(boolean useInternalBBoxFilter) {
		this.useInternalBBoxFilter = useInternalBBoxFilter;
	}

	public String getLogPath() {
		return logPath;
	}

	public String getCurrentLogPath() {
		return currentLogPath;
	}

	public void setCurrentLogPath(String currentLogPath) {
		this.currentLogPath = currentLogPath;
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		changes.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		changes.removePropertyChangeListener(l);
	}
}

/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2015,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.modules.citygml.importer.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.citydb.api.event.Event;
import org.citydb.api.event.EventDispatcher;
import org.citydb.api.event.EventHandler;
import org.citydb.api.log.LogLevel;
import org.citydb.config.Config;
import org.citydb.config.internal.Internal;
import org.citydb.config.language.Language;
import org.citydb.io.DirectoryScanner;
import org.citydb.io.DirectoryScanner.CityGMLFilenameFilter;
import org.citydb.log.Logger;
import org.citydb.modules.common.event.CounterEvent;
import org.citydb.modules.common.event.CounterType;
import org.citydb.modules.common.event.EventType;
import org.citydb.modules.common.event.InterruptEvent;
import org.citydb.modules.common.event.StatusDialogMessage;
import org.citydb.modules.common.event.StatusDialogProgressBar;
import org.citydb.modules.common.event.StatusDialogTitle;
import org.citydb.util.Util;
import org.citygml4j.xml.schema.SchemaHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLValidator implements EventHandler {
	private final Logger LOG = Logger.getInstance();

	private final Config config;
	private final EventDispatcher eventDispatcher;

	private volatile boolean shouldRun = true;
	private DirectoryScanner directoryScanner;
	private boolean reportAllErrors;
	private InputStream inputStream;
	
	public XMLValidator(Config config, EventDispatcher eventDispatcher) {
		this.config = config;
		this.eventDispatcher = eventDispatcher;
	}

	public void cleanup() {
		eventDispatcher.removeEventHandler(this);
	}

	public boolean doProcess() {
		// adding listeners
		eventDispatcher.addEventHandler(EventType.INTERRUPT, this);

		// build list of files to be validated
		Internal intConfig = config.getInternal();
		LOG.info("Creating list of CityGML files to be validated...");
		directoryScanner = new DirectoryScanner(true);
		directoryScanner.addFilenameFilter(new CityGMLFilenameFilter());
		List<File> importFiles = directoryScanner.getFiles(intConfig.getImportFiles());

		if (!shouldRun)
			return true;

		if (importFiles.size() == 0) {
			LOG.warn("Failed to find CityGML files at the specified locations.");
			return false;
		}

		int fileCounter = 0;
		int remainingFiles = importFiles.size();
		LOG.info("List of import files successfully created.");
		LOG.info(remainingFiles + " file(s) will be validated.");

		// prepare XML validation
		reportAllErrors = !config.getProject().getImporter().getXMLValidation().isSetReportOneErrorPerFeature();
		Validator validator = null;
		ValidationErrorHandler errorHandler = new ValidationErrorHandler();
		try {
			SchemaHandler schemaHandler = SchemaHandler.newInstance();
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);		
			Schema schema = schemaFactory.newSchema(schemaHandler.getSchemaSources());	
			validator = schema.newValidator();
		} catch (SAXException e) {
			LOG.error("Failed to create CityGML schema context: " + e.getMessage());
			return false;
		}

		long start = System.currentTimeMillis();
		
		while (shouldRun && fileCounter < importFiles.size()) {			
			File file = importFiles.get(fileCounter++);
			intConfig.setImportPath(file.getParent());

			eventDispatcher.triggerEvent(new StatusDialogTitle(file.getName(), this));
			eventDispatcher.triggerEvent(new StatusDialogMessage(Language.I18N.getString("validate.dialog.validate.msg"), this));
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(true, this));
			eventDispatcher.triggerEvent(new CounterEvent(CounterType.FILE, --remainingFiles, this));

			// ok, preparation done. inform user and start validating the input file
			try {
				LOG.info("Validating file: " + file.toString());
				
				validator.reset(); 
				validator.setErrorHandler(errorHandler);
				errorHandler.reset();
				
				inputStream = new FileInputStream(file);
				validator.validate(new StreamSource(inputStream));	
			} catch (SAXException | IOException e) {
				if (!errorHandler.isAborted && shouldRun)
					LOG.error("Failed to validate CityGML file: " + e.getMessage());
			}

			eventDispatcher.triggerEvent(new StatusDialogMessage(Language.I18N.getString("validate.dialog.finish.msg"), this));
			eventDispatcher.triggerEvent(new StatusDialogProgressBar(true, this));

			// show XML validation errors
			if (errorHandler.errors > 0)
				LOG.warn(errorHandler.errors + " error(s) reported while validating the document.");
			else if (errorHandler.errors == 0 && shouldRun)
				LOG.info("The CityGML file is valid.");
		}
		
		if (shouldRun)
			LOG.info("Total validation time: " + Util.formatElapsedTime(System.currentTimeMillis() - start) + ".");

		return shouldRun;
	}

	@Override
	public void handleEvent(Event e) throws Exception {
		if (e.getEventType() == EventType.INTERRUPT) {
			shouldRun = false;
			InterruptEvent interruptEvent = (InterruptEvent)e;

			if (interruptEvent.getCause() != null) {
				Throwable cause = interruptEvent.getCause();
				LOG.error("An error occured: " + cause.getMessage());
				while ((cause = cause.getCause()) != null)
					LOG.error("Cause: " + cause.getMessage());
			}
			
			String log = interruptEvent.getLogMessage();
			if (log != null)
				LOG.log(interruptEvent.getLogLevelType(), log);
			
			if (directoryScanner != null)
				directoryScanner.stopScanning();

			if (inputStream != null)
				inputStream.close();
		}
	}

	private final class ValidationErrorHandler implements ErrorHandler {
		int errors;
		boolean isAborted;

		public void reset() {
			errors = 0;
			isAborted = false;
		}

		@Override
		public void warning(SAXParseException e) throws SAXException {
			write(e, "Warning", LogLevel.WARN);
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			write(e, "Invalid content", LogLevel.ERROR);
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			write(e, "Invalid content", LogLevel.ERROR);
		}

		public void write(SAXParseException e, String prefix, LogLevel level) throws SAXException {
			if (!isAborted) {
				StringBuilder msg = new StringBuilder()
				.append(prefix).append(" at ")
				.append('[').append(e.getLineNumber()).append(',').append(e.getColumnNumber()).append("]: ")
				.append(e.getMessage());
				LOG.log(level, msg.toString());

				errors++;						

				if (!reportAllErrors) {
					isAborted = true;
					throw new SAXException();
				}
			}
		}
	}

}

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
package org.citydb.modules.citygml.importer.database.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.citygml4j.model.citygml.core.ExternalObject;
import org.citygml4j.model.citygml.core.ExternalReference;

public class DBExternalReference implements DBImporter {
	private final Connection batchConn;
	private final DBImporterManager dbImporterManager;

	private PreparedStatement psExternalReference;
	private int batchCounter;

	public DBExternalReference(Connection batchConn, DBImporterManager dbImporterManager) throws SQLException {
		this.batchConn = batchConn;
		this.dbImporterManager = dbImporterManager;

		init();
	}

	private void init() throws SQLException {
		StringBuilder stmt = new StringBuilder()
		.append("insert into EXTERNAL_REFERENCE (ID, INFOSYS, NAME, URI, CITYOBJECT_ID) values ")
		.append("(").append(dbImporterManager.getDatabaseAdapter().getSQLAdapter().getNextSequenceValue(DBSequencerEnum.EXTERNAL_REFERENCE_ID_SEQ))
		.append(", ?, ?, ?, ?)");
		psExternalReference = batchConn.prepareStatement(stmt.toString());
	}

	public void insert(ExternalReference externalReference, long cityObjectId) throws SQLException {
		// informationSystem
		if (externalReference.isSetInformationSystem()) {
			psExternalReference.setString(1, externalReference.getInformationSystem());
		} else {
			psExternalReference.setNull(1, Types.VARCHAR);
		}

		// ExternalObject
		if (externalReference.isSetExternalObject()) {
			ExternalObject externalObject = externalReference.getExternalObject();

			// name
			if (externalObject.isSetName()) {
				psExternalReference.setString(2, externalObject.getName());
			} else {
				psExternalReference.setNull(2, Types.VARCHAR);
			}

			// uri
			if (externalObject.isSetUri()) {
				psExternalReference.setString(3, externalObject.getUri());
			} else {
				psExternalReference.setNull(3, Types.VARCHAR);
			}
		} else {
			psExternalReference.setNull(2, Types.VARCHAR);
			psExternalReference.setNull(3, Types.VARCHAR);
		}

		// cityObjectId
		psExternalReference.setLong(4, cityObjectId);

		psExternalReference.addBatch();
		if (++batchCounter == dbImporterManager.getDatabaseAdapter().getMaxBatchSize())
			dbImporterManager.executeBatch(DBImporterEnum.EXTERNAL_REFERENCE);
	}

	@Override
	public void executeBatch() throws SQLException {
		psExternalReference.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psExternalReference.close();
	}

	@Override
	public DBImporterEnum getDBImporterType() {
		return DBImporterEnum.EXTERNAL_REFERENCE;
	}

}

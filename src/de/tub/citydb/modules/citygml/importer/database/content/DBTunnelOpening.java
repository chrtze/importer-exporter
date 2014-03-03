/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package de.tub.citydb.modules.citygml.importer.database.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.citygml4j.geometry.Matrix;
import org.citygml4j.model.citygml.core.ImplicitGeometry;
import org.citygml4j.model.citygml.core.ImplicitRepresentationProperty;
import org.citygml4j.model.citygml.tunnel.AbstractOpening;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;

import de.tub.citydb.api.geometry.GeometryObject;
import de.tub.citydb.config.Config;
import de.tub.citydb.database.TableEnum;
import de.tub.citydb.modules.citygml.common.database.xlink.DBXlinkSurfaceGeometry;
import de.tub.citydb.util.Util;

public class DBTunnelOpening implements DBImporter {
	private final Connection batchConn;
	private final DBImporterManager dbImporterManager;

	private PreparedStatement psOpening;
	private DBCityObject cityObjectImporter;
	private DBSurfaceGeometry surfaceGeometryImporter;
	private DBOtherGeometry otherGeometryImporter;
	private DBImplicitGeometry implicitGeometryImporter;
	private DBTunnelOpenToThemSrf openingToThemSurfaceImporter;

	private boolean affineTransformation;
	private int batchCounter;

	public DBTunnelOpening(Connection batchConn, Config config, DBImporterManager dbImporterManager) throws SQLException {
		this.batchConn = batchConn;
		this.dbImporterManager = dbImporterManager;

		affineTransformation = config.getProject().getImporter().getAffineTransformation().isSetUseAffineTransformation();
		init();
	}

	private void init() throws SQLException {
		StringBuilder stmt = new StringBuilder()
		.append("insert into TUNNEL_OPENING (ID, OBJECTCLASS_ID, LOD3_MULTI_SURFACE_ID, LOD4_MULTI_SURFACE_ID, ")
		.append("LOD3_IMPLICIT_REP_ID, LOD4_IMPLICIT_REP_ID, ")
		.append("LOD3_IMPLICIT_REF_POINT, LOD4_IMPLICIT_REF_POINT, ")
		.append("LOD3_IMPLICIT_TRANSFORMATION, LOD4_IMPLICIT_TRANSFORMATION) values ")
		.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		psOpening = batchConn.prepareStatement(stmt.toString());

		surfaceGeometryImporter = (DBSurfaceGeometry)dbImporterManager.getDBImporter(DBImporterEnum.SURFACE_GEOMETRY);
		otherGeometryImporter = (DBOtherGeometry)dbImporterManager.getDBImporter(DBImporterEnum.OTHER_GEOMETRY);
		implicitGeometryImporter = (DBImplicitGeometry)dbImporterManager.getDBImporter(DBImporterEnum.IMPLICIT_GEOMETRY);
		cityObjectImporter = (DBCityObject)dbImporterManager.getDBImporter(DBImporterEnum.CITYOBJECT);
		openingToThemSurfaceImporter = (DBTunnelOpenToThemSrf)dbImporterManager.getDBImporter(DBImporterEnum.TUNNEL_OPEN_TO_THEM_SRF);
	}

	public long insert(AbstractOpening opening, long parentId) throws SQLException {
		long openingId = dbImporterManager.getDBId(DBSequencerEnum.CITYOBJECT_ID_SEQ);
		if (openingId == 0)
			return 0;

		// CityObject
		cityObjectImporter.insert(opening, openingId);

		// Opening
		// ID
		psOpening.setLong(1, openingId);

		// OBJECTCLASS_ID
		psOpening.setInt(2, Util.cityObject2classId(opening.getCityGMLClass()));

		// Geometry
		for (int i = 0; i < 2; i++) {
			MultiSurfaceProperty multiSurfaceProperty = null;
			long multiSurfaceId = 0;

			switch (i) {
			case 0:
				multiSurfaceProperty = opening.getLod3MultiSurface();
				break;
			case 1:
				multiSurfaceProperty = opening.getLod4MultiSurface();
				break;
			}

			if (multiSurfaceProperty != null) {
				if (multiSurfaceProperty.isSetMultiSurface()) {
					multiSurfaceId = surfaceGeometryImporter.insert(multiSurfaceProperty.getMultiSurface(), openingId);
					multiSurfaceProperty.unsetMultiSurface();
				} else {
					// xlink
					String href = multiSurfaceProperty.getHref();

					if (href != null && href.length() != 0) {
						dbImporterManager.propagateXlink(new DBXlinkSurfaceGeometry(
								href, 
								openingId, 
								TableEnum.TUNNEL_OPENING, 
								"LOD" + (i + 3) + "_MULTI_SURFACE_ID"));
					}
				}
			}

			if (multiSurfaceId != 0)
				psOpening.setLong(3 + i, multiSurfaceId);
			else
				psOpening.setNull(3 + i, Types.NULL);
		}

		// implicit geometry
		for (int i = 0; i < 2; i++) {
			ImplicitRepresentationProperty implicit = null;
			GeometryObject pointGeom = null;
			String matrixString = null;
			long implicitId = 0;

			switch (i) {
			case 0:
				implicit = opening.getLod3ImplicitRepresentation();
				break;
			case 1:
				implicit = opening.getLod4ImplicitRepresentation();
				break;
			}

			if (implicit != null) {
				if (implicit.isSetObject()) {
					ImplicitGeometry geometry = implicit.getObject();

					// reference Point
					if (geometry.isSetReferencePoint())
						pointGeom = otherGeometryImporter.getPoint(geometry.getReferencePoint());

					// transformation matrix
					if (geometry.isSetTransformationMatrix()) {
						Matrix matrix = geometry.getTransformationMatrix().getMatrix();
						if (affineTransformation)
							matrix = dbImporterManager.getAffineTransformer().transformImplicitGeometryTransformationMatrix(matrix);

						matrixString = Util.collection2string(matrix.toRowPackedList(), " ");
					}

					// reference to IMPLICIT_GEOMETRY
					implicitId = implicitGeometryImporter.insert(geometry, openingId);
				}
			}

			if (implicitId != 0)
				psOpening.setLong(5 + i, implicitId);
			else
				psOpening.setNull(5 + i, Types.NULL);

			if (pointGeom != null)
				psOpening.setObject(7 + i, dbImporterManager.getDatabaseAdapter().getGeometryConverter().getDatabaseObject(pointGeom, batchConn));
			else
				psOpening.setNull(7 + i, dbImporterManager.getDatabaseAdapter().getGeometryConverter().getNullGeometryType(),
						dbImporterManager.getDatabaseAdapter().getGeometryConverter().getNullGeometryTypeName());

			if (matrixString != null)
				psOpening.setString(9 + i, matrixString);
			else
				psOpening.setNull(9 + i, Types.VARCHAR);
		}

		psOpening.addBatch();
		if (++batchCounter == dbImporterManager.getDatabaseAdapter().getMaxBatchSize())
			dbImporterManager.executeBatch(DBImporterEnum.TUNNEL_OPENING);

		openingToThemSurfaceImporter.insert(openingId, parentId);

		// insert local appearance
		cityObjectImporter.insertAppearance(opening, openingId);

		return openingId;
	}

	@Override
	public void executeBatch() throws SQLException {
		psOpening.executeBatch();
		batchCounter = 0;
	}

	@Override
	public void close() throws SQLException {
		psOpening.close();
	}

	@Override
	public DBImporterEnum getDBImporterType() {
		return DBImporterEnum.TUNNEL_OPENING;
	}

}
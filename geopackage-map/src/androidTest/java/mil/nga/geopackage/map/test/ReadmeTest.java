package mil.nga.geopackage.map.test;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageFactory;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.contents.ContentsDao;
import mil.nga.geopackage.extension.ExtensionsDao;
import mil.nga.geopackage.extension.metadata.MetadataDao;
import mil.nga.geopackage.extension.metadata.MetadataExtension;
import mil.nga.geopackage.extension.metadata.reference.MetadataReferenceDao;
import mil.nga.geopackage.extension.schema.SchemaExtension;
import mil.nga.geopackage.extension.schema.columns.DataColumnsDao;
import mil.nga.geopackage.extension.schema.constraints.DataColumnConstraintsDao;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.index.FeatureIndexType;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.tiles.TileGenerator;
import mil.nga.geopackage.tiles.UrlTileGenerator;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.FeatureTileGenerator;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile;
import mil.nga.geopackage.tiles.matrix.TileMatrixDao;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSetDao;
import mil.nga.geopackage.tiles.user.TileCursor;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.sf.Geometry;
import mil.nga.sf.proj.Projection;
import mil.nga.sf.proj.ProjectionConstants;
import mil.nga.sf.proj.ProjectionFactory;

/**
 * README example tests
 *
 * @author osbornb
 */
public class ReadmeTest extends ImportGeoPackageTestCase {

    /**
     * Test transform
     *
     * @throws IOException  upon error
     * @throws SQLException upon error
     */
    //@Test
    public void testGeoPackage() throws IOException, SQLException {

        geoPackage.close();
        geoPackage = null;
        GeoPackageManager manager = GeoPackageFactory.getManager(activity);
        manager.delete(TestConstants.IMPORT_DB_NAME);

        String importLocation = TestUtils.getAssetFileInternalStorageLocation(
                activity, TestConstants.IMPORT_DB_FILE_NAME);
        File geoPackageFile = new File(importLocation);

        GoogleMap map = null; // test map?

        try {
            testGeoPackage(activity, geoPackageFile, map);
        } finally {
            manager.delete(TestConstants.IMPORT_DB_NAME);
        }

    }

    /**
     * Test GeoPackage
     *
     * @param context        context
     * @param geoPackageFile GeoPackage file
     * @param map            Google map
     * @throws IOException  upon error
     * @throws SQLException upon error
     */
    private void testGeoPackage(Context context, File geoPackageFile, GoogleMap map) throws IOException, SQLException {

        // Context context = ...;
        // File geoPackageFile = ...;
        // GoogleMap map = ...;

        // Get a manager
        GeoPackageManager manager = GeoPackageFactory.getManager(context);

        // Import database
        boolean imported = manager.importGeoPackage(geoPackageFile);

        // Available databases
        List<String> databases = manager.databases();

        // Open database
        GeoPackage geoPackage = manager.open(databases.get(0));

        // GeoPackage Table DAOs
        SpatialReferenceSystemDao srsDao = geoPackage.getSpatialReferenceSystemDao();
        ContentsDao contentsDao = geoPackage.getContentsDao();
        GeometryColumnsDao geomColumnsDao = geoPackage.getGeometryColumnsDao();
        TileMatrixSetDao tileMatrixSetDao = geoPackage.getTileMatrixSetDao();
        TileMatrixDao tileMatrixDao = geoPackage.getTileMatrixDao();
        SchemaExtension schemaExtension = new SchemaExtension(geoPackage);
        DataColumnsDao dao = schemaExtension.getDataColumnsDao();
        DataColumnConstraintsDao dataColumnConstraintsDao = schemaExtension
                .getDataColumnConstraintsDao();
        MetadataExtension metadataExtension = new MetadataExtension(geoPackage);
        MetadataDao metadataDao = metadataExtension.getMetadataDao();
        MetadataReferenceDao metadataReferenceDao = metadataExtension
                .getMetadataReferenceDao();
        ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

        // Feature and tile tables
        List<String> features = geoPackage.getFeatureTables();
        List<String> tiles = geoPackage.getTileTables();

        // Query Features
        String featureTable = features.get(0);
        FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
        GoogleMapShapeConverter converter = new GoogleMapShapeConverter(
                featureDao.getProjection());
        FeatureCursor featureCursor = featureDao.queryForAll();
        try {
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                if (geometryData != null && !geometryData.isEmpty()) {
                    Geometry geometry = geometryData.getGeometry();
                    GoogleMapShape shape = converter.toShape(geometry);
                    GoogleMapShape mapShape = GoogleMapShapeConverter
                            .addShapeToMap(map, shape);
                    // ...
                }
            }
        } finally {
            featureCursor.close();
        }

        // Query Tiles
        String tileTable = tiles.get(0);
        TileDao tileDao = geoPackage.getTileDao(tileTable);
        TileCursor tileCursor = tileDao.queryForAll();
        try {
            while (tileCursor.moveToNext()) {
                TileRow tileRow = tileCursor.getRow();
                byte[] tileBytes = tileRow.getTileData();
                Bitmap tileBitmap = tileRow.getTileDataBitmap();
                // ...
            }
        } finally {
            tileCursor.close();
        }

        // Tile Provider (GeoPackage or Google API)
        TileProvider overlay = GeoPackageOverlayFactory
                .getTileProvider(tileDao);
        TileOverlayOptions overlayOptions = new TileOverlayOptions();
        overlayOptions.tileProvider(overlay);
        overlayOptions.zIndex(-1);
        map.addTileOverlay(overlayOptions);

        // Index Features
        FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
        indexer.setIndexLocation(FeatureIndexType.GEOPACKAGE);
        int indexedCount = indexer.index();

        // Feature Tile Provider (dynamically draw tiles from features)
        FeatureTiles featureTiles = new DefaultFeatureTiles(context, featureDao, context.getResources().getDisplayMetrics().density);
        featureTiles.setMaxFeaturesPerTile(1000); // Set max features to draw per tile
        NumberFeaturesTile numberFeaturesTile = new NumberFeaturesTile(context); // Custom feature tile implementation
        featureTiles.setMaxFeaturesTileDraw(numberFeaturesTile); // Draw feature count tiles when max features passed
        featureTiles.setIndexManager(indexer); // Set index manager to query feature indices
        FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
        featureOverlay.setMinZoom(featureDao.getZoomLevel()); // Set zoom level to start showing tiles
        TileOverlayOptions featureOverlayOptions = new TileOverlayOptions();
        featureOverlayOptions.tileProvider(featureOverlay);
        featureOverlayOptions.zIndex(-1); // Draw the feature tiles behind map markers
        map.addTileOverlay(featureOverlayOptions);

        BoundingBox boundingBox = new BoundingBox();
        Projection projection = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);

        // URL Tile Generator (generate tiles from a URL)
        TileGenerator urlTileGenerator = new UrlTileGenerator(context, geoPackage,
                "url_tile_table", "http://url/{z}/{x}/{y}.png", 1, 2, boundingBox, projection);
        int urlTileCount = urlTileGenerator.generateTiles();

        // Feature Tile Generator (generate tiles from features)
        TileGenerator featureTileGenerator = new FeatureTileGenerator(context, geoPackage,
                featureTable + "_tiles", featureTiles, 1, 2, boundingBox, projection);
        int featureTileCount = featureTileGenerator.generateTiles();

        // Close feature tiles (and indexer)
        featureTiles.close();

        // Close database when done
        geoPackage.close();

    }

}

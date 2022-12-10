package gov.nasa.ziggy.data.management;

import static gov.nasa.ziggy.pipeline.definition.XmlUtils.assertContains;
import static gov.nasa.ziggy.pipeline.definition.XmlUtils.complexTypeContent;
import static gov.nasa.ziggy.pipeline.definition.XmlUtils.simpleTypeContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import gov.nasa.ziggy.data.management.Manifest.ManifestEntry;
import gov.nasa.ziggy.services.config.PropertyNames;
import gov.nasa.ziggy.util.io.FileUtil;

/**
 * Unit tests for {@link Manifest} class.
 *
 * @author PT
 */
public class ManifestTest {

    public static final String TEST_DATA_DIR = "build/test/manifest";
    public static final String TEST_DATA_SRC = "test/data/configuration";

    private Path testDataDir;
    private ChecksumType checksumType = Manifest.CHECKSUM_TYPE;

    @Before
    public void setUp() {
        testDataDir = Paths.get(TEST_DATA_DIR);
        testDataDir.toFile().mkdirs();
        String workingDir = System.getProperty("user.dir");
        Path homeDirPath = Paths.get(workingDir, "build");
        System.setProperty(PropertyNames.ZIGGY_HOME_DIR_PROP_NAME, homeDirPath.toString());
        System.setProperty(PropertyNames.DATASTORE_ROOT_DIR_PROP_NAME, "/dev/null");
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        FileUtils.forceDelete(testDataDir.getParent().toFile());
        System.clearProperty(PropertyNames.ZIGGY_HOME_DIR_PROP_NAME);
        System.clearProperty(PropertyNames.DATASTORE_ROOT_DIR_PROP_NAME);
    }

    @Test
    public void testGenerateFromFiles() throws IOException {

        // Copy all the files from the source directory
        FileUtils.copyDirectory(new File(TEST_DATA_SRC), new File(TEST_DATA_DIR));

        // Create a file that should not be in the manifest
        Files.createFile(testDataDir.resolve(".hidden-file"));

        // Generate a directory with content
        Files.createDirectory(testDataDir.resolve("sub-directory"));
        FileUtils.copyDirectory(new File(TEST_DATA_SRC), new File(TEST_DATA_DIR, "sub-directory"));

        Manifest manifest = Manifest.generateManifest(testDataDir, 100L);
        assertEquals(100L, manifest.getDatasetId());
        assertEquals(32, manifest.getFileCount());
        assertEquals(32, manifest.getManifestEntries().size());
        for (ManifestEntry manifestFile : manifest.getManifestEntries()) {
            validateManifestFile(manifestFile);
        }
    }

    @Test
    public void testGenerateFromSymlinks() throws IOException {

        // Symlink all the files from the source directory
        FileUtil.symlinkDirectoryContents(Paths.get(TEST_DATA_SRC), Paths.get(TEST_DATA_DIR));

        // Create a file that should not be in the manifest
        Files.createFile(testDataDir.resolve(".hidden-file"));

        // Generate a directory with content
        Files.createDirectory(testDataDir.resolve("sub-directory"));
        FileUtil.symlinkDirectoryContents(Paths.get(TEST_DATA_SRC),
            Paths.get(TEST_DATA_DIR, "sub-directory"));

        Manifest manifest = Manifest.generateManifest(testDataDir, 100L);
        assertEquals(100L, manifest.getDatasetId());
        assertEquals(32, manifest.getFileCount());
        assertEquals(32, manifest.getManifestEntries().size());
        for (ManifestEntry manifestFile : manifest.getManifestEntries()) {
            validateManifestFile(manifestFile);
        }
    }

    @Test
    public void testWriteAndReadManifest() throws IOException, InstantiationException,
        IllegalAccessException, SAXException, JAXBException {

        // Copy all the files from the source directory
        FileUtils.copyDirectory(new File(TEST_DATA_SRC), new File(TEST_DATA_DIR));

        // Create a couple of files that should not be in the manifest
        Files.createFile(testDataDir.resolve(".hidden-file"));

        // Generate a directory with content
        Files.createDirectory(testDataDir.resolve("sub-directory"));
        FileUtils.copyDirectory(new File(TEST_DATA_SRC), new File(TEST_DATA_DIR, "sub-directory"));

        Manifest manifest = Manifest.generateManifest(testDataDir, 100L);
        manifest.setName("test-manifest.xml");
        manifest.write(testDataDir);
        assertTrue(Files.exists(testDataDir.resolve("test-manifest.xml")));

        // Read the manifest back in
        Manifest roundTripManifest = Manifest.readManifest(testDataDir);
        assertEquals("test-manifest.xml", roundTripManifest.getName());
        assertEquals(32, roundTripManifest.getFileCount());
        assertEquals(32, roundTripManifest.getManifestEntries().size());
        assertEquals(100L, roundTripManifest.getDatasetId());
        for (ManifestEntry manifestFile : roundTripManifest.getManifestEntries()) {
            assertTrue(manifest.getManifestEntries().contains(manifestFile));
        }
    }

    @Test
    public void testWriteAndReadManifestSymlinks() throws IOException, InstantiationException,
        IllegalAccessException, SAXException, JAXBException {

        // Symlink all the files from the source directory
        FileUtil.symlinkDirectoryContents(Paths.get(TEST_DATA_SRC), Paths.get(TEST_DATA_DIR));

        // Create a file that should not be in the manifest
        Files.createFile(testDataDir.resolve(".hidden-file"));

        // Generate a directory with content
        Files.createDirectory(testDataDir.resolve("sub-directory"));
        FileUtil.symlinkDirectoryContents(Paths.get(TEST_DATA_SRC),
            Paths.get(TEST_DATA_DIR, "sub-directory"));

        Manifest manifest = Manifest.generateManifest(testDataDir, 100L);
        manifest.setName("test-manifest.xml");
        manifest.write(testDataDir);
        assertTrue(Files.exists(testDataDir.resolve("test-manifest.xml")));

        // Read the manifest back in
        Manifest roundTripManifest = Manifest.readManifest(testDataDir);
        assertEquals("test-manifest.xml", roundTripManifest.getName());
        assertEquals(32, roundTripManifest.getFileCount());
        assertEquals(32, roundTripManifest.getManifestEntries().size());
        assertEquals(100L, roundTripManifest.getDatasetId());
        for (ManifestEntry manifestFile : roundTripManifest.getManifestEntries()) {
            assertTrue(manifest.getManifestEntries().contains(manifestFile));
        }
    }

    @Test
    public void testSchema() throws IOException {
        Path schemaPath = Paths.get(System.getProperty(PropertyNames.ZIGGY_HOME_DIR_PROP_NAME),
            "schema", "xml", new Manifest().getXmlSchemaFilename());
        List<String> schemaContent = Files.readAllLines(schemaPath);

        assertContains(schemaContent, "<xs:element name=\"manifest\" type=\"manifest\"/>");

        List<String> complexTypeContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"manifest\">");
        assertContains(complexTypeContent,
            "<xs:element name=\"file\" type=\"manifestEntry\" maxOccurs=\"unbounded\"/>");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"datasetId\" type=\"xs:long\" use=\"required\"/>");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"fileCount\" type=\"xs:int\" use=\"required\"/>");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"checksumType\" type=\"checksumType\" use=\"required\"/>");

        complexTypeContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"manifestEntry\">");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"name\" type=\"xs:string\" use=\"required\"/>");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"size\" type=\"xs:long\" use=\"required\"/>");
        assertContains(complexTypeContent,
            "<xs:attribute name=\"checksum\" type=\"xs:string\" use=\"required\"/>");

        complexTypeContent = simpleTypeContent(schemaContent,
            "<xs:simpleType name=\"checksumType\">");
        assertContains(complexTypeContent, "<xs:restriction base=\"xs:string\">");
        assertContains(complexTypeContent, "<xs:enumeration value=\"MD5\"/>");
        assertContains(complexTypeContent, "<xs:enumeration value=\"SHA1\"/>");
        assertContains(complexTypeContent, "<xs:enumeration value=\"SHA256\"/>");
        assertContains(complexTypeContent, "<xs:enumeration value=\"SHA512\"/>");
    }

    private void validateManifestFile(ManifestEntry manifestFile) throws IOException {
        Path file = DataFileManager.realSourceFile(testDataDir.resolve(manifestFile.getName()));
        assertTrue(Files.exists(file));
        assertEquals(Files.size(file), manifestFile.getSize());
        String sha256 = checksumType.checksum(file);
        assertEquals(sha256, manifestFile.getChecksum());
    }

}

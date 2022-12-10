package gov.nasa.ziggy.pipeline.definition;

import static gov.nasa.ziggy.pipeline.definition.XmlUtils.assertContains;
import static gov.nasa.ziggy.pipeline.definition.XmlUtils.complexTypeContent;
import static gov.nasa.ziggy.pipeline.definition.XmlUtils.nodeContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.nasa.ziggy.pipeline.xml.XmlReference;
import gov.nasa.ziggy.pipeline.xml.XmlReference.InputTypeReference;
import gov.nasa.ziggy.pipeline.xml.XmlReference.ModelTypeReference;
import gov.nasa.ziggy.pipeline.xml.XmlReference.OutputTypeReference;
import gov.nasa.ziggy.uow.SingleUnitOfWorkGenerator;
import gov.nasa.ziggy.util.io.Filenames;

/**
 * Unit tests for {@link PipelineDefinitionNode} class. These are primarily tests of the XML
 * conversion system, since the rest of the class is just getters and setters.
 *
 * @author PT
 */
public class PipelineDefinitionNodeTest {

    private String workingDirName;
    private Node node;
    private File xmlFile;
    private File xmlUnmarshalingFile;
    private File schemaFile;

    @Before
    public void setUp() {

        // Set the working directory
        workingDirName = System.getProperty("user.dir");
        xmlUnmarshalingFile = new File(workingDirName + "/test/data/configuration/node.xml");
        String workingDir = workingDirName + "/build/test";
        new File(workingDir).mkdirs();
        xmlFile = new File(workingDir, "node.xml");
        schemaFile = new File(workingDir, "node.xsd");

        // Construct a new node for the test
        node = new Node(new ModuleName("module 1"), null);
        node.setStartNewUow(true);
        node.setUnitOfWorkGenerator(new ClassWrapper<>(SingleUnitOfWorkGenerator.class));
        node.setChildNodeNames("module 2, module 3");
        Set<XmlReference> xmlReferences = new HashSet<>();
        xmlReferences.add(new ParameterSetName("Remote execution"));
        xmlReferences.add(new ParameterSetName("Convergence criteria"));
        xmlReferences.add(new InputTypeReference("flight L0 data"));
        xmlReferences.add(new OutputTypeReference("flight L1 data"));
        xmlReferences.add(new ModelTypeReference("calibration constants"));
        node.setXmlReferences(xmlReferences);

    }

    @After
    public void tearDown() throws IOException {
        xmlFile.delete();
        schemaFile.delete();
        FileUtils.deleteDirectory(new File(Filenames.BUILD_TEST));
    }

    @Test
    public void testMarshaller() throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(Node.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(node, xmlFile);
        assertTrue(xmlFile.exists());
        List<String> xmlContent = Files.readAllLines(xmlFile.toPath());
        assertEquals(8, xmlContent.size());
        List<String> nodeContent = nodeContent(xmlContent,
            "<node startNewUow=\"true\" "
                + "uowGenerator=\"gov.nasa.ziggy.uow.SingleUnitOfWorkGenerator\" "
                + "moduleName=\"module 1\" childNodeNames=\"module 2, module 3\">");
        String[] xmlLines = new String[] { "<moduleParameter name=\"Convergence criteria\"/>",
            "<moduleParameter name=\"Remote execution\"/>",
            "<inputDataFileType name=\"flight L0 data\"/>",
            "<outputDataFileType name=\"flight L1 data\"/>",
            "<modelType name=\"calibration constants\"/>" };
        for (String xmlLine : xmlLines) {
            assertContains(nodeContent, xmlLine);
        }
    }

    @Test
    public void testUnmarshaller() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Node.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        PipelineDefinitionNode node = (Node) unmarshaller.unmarshal(xmlUnmarshalingFile);
        assertEquals("module 1", node.getModuleName().getName());
        assertTrue(node.isStartNewUow());
        assertEquals("module 2, module 3", node.getChildNodeNames());
        assertEquals(2, node.getParameterSetNames().size());
        assertTrue(node.getParameterSetNames().contains(new ParameterSetName("Remote execution")));
        assertTrue(
            node.getParameterSetNames().contains(new ParameterSetName("Convergence criteria")));
        assertEquals(1, node.getInputDataFileTypeReferences().size());
        assertTrue(node.getInputDataFileTypeReferences()
            .contains(new InputTypeReference("flight L0 data")));
        assertEquals(1, node.getOutputDataFileTypeReferences().size());
        assertTrue(node.getOutputDataFileTypeReferences()
            .contains(new OutputTypeReference("flight L1 data")));
        assertEquals(1, node.getModelTypeReferences().size());
        assertTrue(node.getModelTypeReferences()
            .contains(new ModelTypeReference("calibration constants")));
    }

    @Test
    public void testGenerateSchema() throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(Node.class);
        context.generateSchema(new NodeSchemaResolver());
        List<String> schemaContent = Files.readAllLines(schemaFile.toPath());
        assertContains(schemaContent, "<xs:element name=\"node\" type=\"node\"/>");

        List<String> nodeContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"node\">");
        assertContains(nodeContent, "<xs:extension base=\"pipelineDefinitionNode\">");

        nodeContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"pipelineDefinitionNode\">");
        String[] nodeStrings = new String[] {
            "<xs:element name=\"moduleParameter\" type=\"parameterSetName\"/>",
            "<xs:element name=\"inputDataFileType\" type=\"inputTypeReference\"/>",
            "<xs:element name=\"outputDataFileType\" type=\"outputTypeReference\"/>",
            "<xs:element name=\"modelType\" type=\"modelTypeReference\"/>",
            "<xs:attribute name=\"startNewUow\" type=\"xs:boolean\"/>",
            "<xs:attribute name=\"uowGenerator\" type=\"xs:string\"/>",
            "<xs:attribute name=\"moduleName\" type=\"xs:string\" use=\"required\"/>",
            "<xs:attribute name=\"childNodeNames\" type=\"xs:string\"/>" };
        for (String nodeString : nodeStrings) {
            assertContains(nodeContent, nodeString);
        }

        List<String> paramSetContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"parameterSetName\">");
        assertContains(paramSetContent, "<xs:extension base=\"xmlReference\">");

        List<String> xmlReferenceContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"xmlReference\">");
        assertContains(xmlReferenceContent,
            "<xs:attribute name=\"name\" type=\"xs:string\" use=\"required\"/>");

        xmlReferenceContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"inputTypeReference\">");
        assertContains(xmlReferenceContent, "<xs:extension base=\"xmlReference\">");

        xmlReferenceContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"outputTypeReference\">");
        assertContains(xmlReferenceContent, "<xs:extension base=\"xmlReference\">");

        xmlReferenceContent = complexTypeContent(schemaContent,
            "<xs:complexType name=\"modelTypeReference\">");
        assertContains(xmlReferenceContent, "<xs:extension base=\"xmlReference\">");
    }

    @Test
    public void testNodeListConstruction() {

        PipelineDefinitionNode node2 = new PipelineDefinitionNode(new ModuleName("module 2"), null);
        node2.setStartNewUow(true);
        node2.addXmlReference(new ParameterSetName("Remote execution"));
        node2.addXmlReference(new ParameterSetName("Convergence criteria"));
        node2.addXmlReference(new InputTypeReference("flight L1 data"));
        node2.addXmlReference(new OutputTypeReference("flight L2 data"));
        node2.addXmlReference(new ModelTypeReference("georeferencing constants"));

        node.addNextNode(node2);

        PipelineDefinitionNode node3 = new PipelineDefinitionNode(new ModuleName("module 3"), null);
        node3.setStartNewUow(true);
        node3.addXmlReference(new ParameterSetName("Excluded bands"));
        node3.addXmlReference(new InputTypeReference("flight L1 data"));
        node3.addXmlReference(new OutputTypeReference("flight L2 data"));
        node3.addXmlReference(new ModelTypeReference("Temperature references"));

        node.addNextNode(node3);

        assertEquals("module 2, module 3", node.getChildNodeNames());
        assertTrue(node.getNextNodes().contains(node3));
        assertTrue(node.getNextNodes().contains(node2));
    }

    private class NodeSchemaResolver extends SchemaOutputResolver {

        @Override
        public Result createOutput(String namespaceUri, String suggestedFileName)
            throws IOException {
            StreamResult result = new StreamResult(schemaFile);
            result.setSystemId(schemaFile.toURI().toURL().toString());
            return result;
        }

    }

    /**
     * Subclass of {@link PipelineDefinitionNode} that allows an XmlRootElement annotation to be
     * prepended. This allows tests of the {@link PipelineDefinitionNode} class as though it was a
     * valid root element, while not forcing the non-test use-cases to put up with the class being a
     * root element.
     *
     * @author PT
     */
    @XmlRootElement(name = "node")
    @XmlAccessorType(XmlAccessType.NONE)
    private static class Node extends PipelineDefinitionNode {

        public Node() {

        }

        public Node(ModuleName pipelineModuleDefinitionName, String pipelineDefinitionName) {
            super(pipelineModuleDefinitionName, pipelineDefinitionName);
        }
    }

}

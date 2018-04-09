package uk.ac.ebi.subs.xml;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import uk.ac.ebi.subs.data.component.*;
import uk.ac.ebi.subs.data.submittable.*;

import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
public class RunSerialisationTest extends SerialisationTest {
    String RUN_RESOURCE = "/uk/ac/ebi/subs/ena/submittable/run_template.json";

    static String RUN_ACCESSION_XPATH = "/RUN/@accession";
    static String RUN_ALIAS_XPATH = "/RUN/@alias";
    static String RUN_CENTER_NAME_XPATH = "/RUN/@center_name";
    static String RUN_TITLE_XPATH = "/RUN[1]/TITLE[1]/text()";
    static String RUN_EXPERIMENT_ACCESSION = "/RUN[1]/EXPERIMENT_REF[1]/@accession";
    static String RUN_EXPERIMENT_REF_NAME = "/RUN[1]/EXPERIMENT_REF[1]/@ref_name";
    static String RUN_FILES_FILENAME = "/RUN[1]/DATA_BLOCK/FILES/FILE/@filename";
    static String RUN_FILES_CHECKSUM = "/RUN[1]/DATA_BLOCK/FILES/FILE/@checksum";
    static String RUN_FILES_CHECKSUM_METHOD = "/RUN[1]/DATA_BLOCK/FILES/FILE/@checksum_method";
    static String RUN_FILES_FILETYPE = "/RUN[1]/DATA_BLOCK/FILES/FILE/@filetype";

    @Override
    @Before
    public void setUp() throws IOException, JAXBException, URISyntaxException {
        super.setUp();
        marshaller = MappingHelper.createMarshaller(ENAExperiment.class, MappingHelper.SUBMITTABLE_PACKAGE, MappingHelper.RUN_MARSHALLER, MappingHelper.COMPONENT_PACKAGE, MappingHelper.ATTRIBUTE_MAPPING);
        unmarshaller = MappingHelper.createUnmarshaller(ENAExperiment.class, MappingHelper.SUBMITTABLE_PACKAGE, MappingHelper.RUN_MARSHALLER, MappingHelper.COMPONENT_PACKAGE, MappingHelper.ATTRIBUTE_MAPPING);
        marshaller.setProperty(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, false);
    }

    @Override
    protected String getName() {
        return "RUN";
    }

    @Test
    public void testMarshallRunAccession() throws Exception {
        AssayData assayData = createAssayData();
        assayData.setAccession(UUID.randomUUID().toString());
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String accession = executeXPathQueryNodeValue(document,RUN_ACCESSION_XPATH);
        assertThat("run accession", enaRun.getAccession(), equalTo(accession));
    }

    @Test
    public void testMarshallRunAlias() throws Exception {
        AssayData assayData = createAssayData();
        assayData.setAlias(UUID.randomUUID().toString());
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String xmlStudyAlias = executeXPathQueryNodeValue(document,RUN_ALIAS_XPATH);
        assertThat("run alias", enaRun.getAlias(), equalTo(xmlStudyAlias));
    }

    @Test
    public void testMarshallRunCenterName() throws Exception {
        AssayData assayData = createAssayData();
        Team team = new Team();
        team.setName(UUID.randomUUID().toString());
        assayData.setTeam(team);
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_CENTER_NAME_XPATH);
        assertThat("run center_name", team.getName(), equalTo(str));
    }

    @Test
    public void testMarshallRunExperimentRef() throws Exception {
        AssayData assayData = createAssayData();
        AssayRef assayRef = new AssayRef();
        assayRef.setAccession(UUID.randomUUID().toString());
        assayData.setAssayRefs(Arrays.asList(assayRef));
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_EXPERIMENT_ACCESSION);
        assertThat("run experiment ref", assayRef.getAccession(), equalTo(str));
    }

    @Test
    public void testMarshalRunTitle() throws Exception {
        AssayData assayData = createAssayData();
        assayData.setTitle(UUID.randomUUID().toString());
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_TITLE_XPATH);
        assertThat("experiment title to XML", assayData.getTitle(), equalTo(str));
    }

    @Test
    public void testMarshallRunFilename() throws Exception {
        AssayData assayData = createAssayData();
        File file = new File();
        file.setName(UUID.randomUUID().toString());
        assayData.getFiles().add(file);
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_FILES_FILENAME);
        assertThat("run filename", file.getName(), equalTo(str));
    }

    @Test
    public void testMarshallRunFiletype() throws Exception {
        AssayData assayData = createAssayData();
        File file = new File();
        file.setType(UUID.randomUUID().toString());
        assayData.getFiles().add(file);
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_FILES_FILETYPE);
        assertThat("run filename", file.getType(), equalTo(str));
    }

    @Test
    public void testMarshallRunFileChecksum() throws Exception {
        AssayData assayData = createAssayData();
        File file = new File();
        file.setChecksum(UUID.randomUUID().toString());
        assayData.getFiles().add(file);
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_FILES_CHECKSUM);
        assertThat("run filename", file.getChecksum(), equalTo(str));
    }

    @Test
    public void testMarshallRunFileChecksumMethod() throws Exception {
        AssayData assayData = createAssayData();
        File file = new File();
        file.setChecksumMethod(UUID.randomUUID().toString());
        assayData.getFiles().add(file);
        ENARun enaRun = new ENARun(assayData);
        final Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
        marshaller.marshal(enaRun,new DOMResult(document));
        String str = executeXPathQueryNodeValue(document,RUN_FILES_CHECKSUM_METHOD);
        assertThat("run filename", file.getChecksumMethod(), equalTo(str));
    }

    AssayData createAssayData () {
        AssayData assayData = new AssayData();
        return assayData;
    }

    AssayData createAssayData (String alias, String assayAlias) {
        AssayData assayData = new AssayData();
        Team team = new Team();
        team.setName("test-team");
        assayData.setTeam(team);
        assayData.setAlias(alias);
        AssayRef assayRef = new AssayRef();
        assayRef.setAlias(assayAlias);
        assayData.setAssayRefs(Arrays.asList(assayRef));
        assayData.setTitle("Test Title");
        File file = new File();
        file.setType("fastq");
        file.setChecksum("12345678abcdefgh12345678abcdefgh");
        file.setName("Test.fastq");
        file.setChecksumMethod("MD5");
        assayData.getFiles().add(file);
        return assayData;
    }

    @Override
    protected ENASubmittable createENASubmittable() throws IllegalAccessException {
        return new ENARun();
    }

    @Test
    public void testMarshalUnmarshallRun () throws Exception {
        final AssayData assayData = createAssayData(UUID.randomUUID().toString(),"assay-ref-001");
        serialiseDeserialiseTest(assayData,ENARun.class);
    }

}
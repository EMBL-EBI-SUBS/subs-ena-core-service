package uk.ac.ebi.subs.data.submittable;

import com.sun.javafx.css.CssError;
import javafx.application.Platform;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import uk.ac.ebi.ena.sra.xml.LibraryDescriptorType;
import uk.ac.ebi.subs.data.client.*;
import uk.ac.ebi.subs.data.component.*;
import uk.ac.ebi.subs.ena.annotation.ENAAttribute;
import uk.ac.ebi.subs.ena.annotation.ENAPlatform;
import uk.ac.ebi.subs.ena.annotation.ENAValidation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by neilg on 28/03/2017.
 */
@ENAValidation
public class ENAExperiment extends Assay implements ENASubmittable {
    public static final String DESIGN_DESCRIPTION = "design_description";
    public static final String LIBRARY_NAME = "library_name";
    public static final String LIBRARY_STRATEGY = "library_strategy";
    public static final String LIBRARY_SOURCE = "library_source";
    public static final String LIBRARY_SELECTION = "library_selection";

    public static final String PLATFORM_TYPE = "platform_type";
    public static final String INSTRUMENT_MODEL = "instrument_model";

    public static final String LIBRARY_LAYOUT = "library_layout";
    public static final String PAIRED_NOMINAL_LENGTH = "paired_nominal_length";
    public static final String PAIRED_NOMINAL_SDEV = "paired_nominal_sdev";

    public static final String SINGLE = "SINGLE";
    public static final String PAIRED = "PAIRED";

    @ENAPlatform(name = "LS454", instrumentModels = {"454 GS 20", "454 GS FLX", "454 GS FLX", "454 GS FLX Titanium", "454 GS Junior", "unspecified"})
    String ls454 ;

    @ENAPlatform(name = "ILLUMINA", instrumentModels = {"Illumina Genome Analyzer", "Illumina Genome Analyzer II", "Illumina Genome Analyzer IIx",
            "Illumina HiSeq 2500", "Illumina HiSeq 2000", "Illumina HiSeq 1500", "Illumina HiSeq 1000", "Illumina MiSeq", "Illumina HiScanSQ",
            "HiSeq X Ten", "NextSeq 500", "HiSeq X Five", "Illumina HiSeq 3000", "Illumina HiSeq 4000", "NextSeq 550", "unspecified"})
    String illumina ;

    @ENAPlatform(name = "HELICOS", instrumentModels = {"Helicos HeliScope", "unspecified"})
    String helicos;

    @ENAPlatform(name = "ABI_SOLID", instrumentModels = {"AB SOLiD System 2.0", "AB SOLiD System 3.0", "AB SOLiD 3 Plus System",
            "AB SOLiD 4 System", "AB SOLiD 4hq System", "AB SOLiD PI System", "AB 5500 Genetic Analyzer", "AB 5500xl Genetic Analyzer",
            "AB 5500xl-W Genetic Analysis System", "unspecified"})
    String abiSolid = null;

    @ENAPlatform(name = "COMPLETE_GENOMICS", instrumentModels = {"Complete Genomics", "unspecified"})
    String completeGenomics = null;

    @ENAPlatform(name = "BGISEQ", instrumentModels = {"BGISEQ-500"})
    String bgiseq = null;

    @ENAPlatform(name = "OXFORD_NANOPORE", instrumentModels = {"MinION", "GridION", "unspecified"})
    String oxfordNanopore = null;

    @ENAPlatform(name = "PACBIO_SMRT", instrumentModels = {"PacBio RS", "PacBio RS II", "Sequel", "unspecified"})
    String pacbioSMRT = null;

    @ENAPlatform(name = "ION_TORRENT", instrumentModels = {"Ion Torrent PGM", "Ion Torrent Proton", "unspecified"})
    String ionTorrent = null;

    @ENAPlatform(name = "CAPILLARY", instrumentModels = {"AB 3730xL Genetic Analyzer", "AB 3730 Genetic Analyzer", "AB 3500xL Genetic Analyzer",
            "AB 3500 Genetic Analyzer", "AB 3130xL Genetic Analyzer", "AB 3130 Genetic Analyzer", "AB 3130 Genetic Analyzer", "AB 310 Genetic Analyzer",
            "unspecified"})
    String capillary = null;

    @ENAAttribute(name = DESIGN_DESCRIPTION)
    String designDescription;

    @ENAAttribute(name = LIBRARY_NAME)
    String libraryName;

    @ENAAttribute(name = LIBRARY_STRATEGY)
    String libraryStrategy;

    @ENAAttribute(name = LIBRARY_SOURCE)
    String librarySource;

    @ENAAttribute(name = LIBRARY_SELECTION)
    String librarySelection;

    @ENAAttribute(name = LIBRARY_LAYOUT, allowedValues = {SINGLE,PAIRED})
    String libraryLayout = SINGLE;

    @ENAAttribute(name = PAIRED_NOMINAL_LENGTH)
    String nominalLength = null;
    @ENAAttribute(name = PAIRED_NOMINAL_SDEV)
    String nominalSdev = null;

    String singleLibraryLayout;
    PairedLibraryLayout pairedLibraryLayout = null;

    public ENAExperiment(Assay assay) throws IllegalAccessException {
        super();
        BeanUtils.copyProperties(assay, this);
        serialiseAttributes();
        serialiseLibraryLayout();
    }

    public ENAExperiment() {
    }

    /*
    public Map<String,String[]> getPlatformInstrumentMap () {
        Map<String,String[]> platformInstrumentMap = new HashMap<>();
        final Field[] declaredFields = this.getClass().getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(ENAPlatform.class)) {
                final ENAPlatform annotation = field.getAnnotation(ENAPlatform.class);
                platformInstrumentMap.put(annotation.name(),annotation.instrumentModels());
            }
        }
    }
    */

    @Override
    public void serialiseAttributes() throws IllegalAccessException {
        ENASubmittable.super.serialiseAttributes();
        final Optional<Attribute> platformTypeAttribute = getExistingStudyTypeAttribute(PLATFORM_TYPE,false);
        final Optional<Attribute> instrumentModelAttribute = getExistingStudyTypeAttribute(INSTRUMENT_MODEL,false);
        if (!platformTypeAttribute.isPresent())
            throw new IllegalAccessException("Attribute " + PLATFORM_TYPE + " not defined");
        if (!instrumentModelAttribute.isPresent())
            throw new IllegalAccessException("Attribute " + INSTRUMENT_MODEL + " not defined");

        final Field[] declaredFields = this.getClass().getDeclaredFields();
        Field platformField = null;
        List<String> allowedPlatforms = new ArrayList<>();

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(ENAPlatform.class)) {
                final ENAPlatform annotation = field.getAnnotation(ENAPlatform.class);
                allowedPlatforms.add(annotation.name());
                if (annotation.name().equalsIgnoreCase(platformTypeAttribute.get().getValue())) platformField = field;
            }
        }

        if (platformField == null)
            throw new IllegalArgumentException(String.format("%s is not a valid platform.  Platform must be one of %s",
                    platformTypeAttribute.get().getValue(), StringUtils.join(allowedPlatforms, ",")));

        List<String> instrumentModel = Arrays.asList(platformField.getAnnotation(ENAPlatform.class).instrumentModels());

        if (!instrumentModel.contains(instrumentModelAttribute.get().getValue()))
            throw new IllegalArgumentException(String.format("%s is not a valid instrument model for platform %s.  Instrument models must be one of %s",
                instrumentModelAttribute.get().getValue(), platformTypeAttribute.get().getValue(),StringUtils.join(instrumentModel, ",")));

        platformField.set(this,instrumentModelAttribute.get().getValue());
        deleteAttribute(platformTypeAttribute.get());
        deleteAttribute(instrumentModelAttribute.get());
    }

    public void serialiseLibraryLayout() throws IllegalAccessException {
        if (libraryLayout.equals(PAIRED)) {
            this.pairedLibraryLayout = new PairedLibraryLayout(nominalLength, nominalSdev);
            this.singleLibraryLayout = null;
        } else if (libraryLayout.equals(SINGLE)) {
            this.singleLibraryLayout = "";
            this.pairedLibraryLayout = null;
        }
    }

    public void deSerialiseAttributes () throws IllegalAccessException {
        ENASubmittable.super.deSerialiseAttributes();

        final Field[] declaredFields = this.getClass().getDeclaredFields();
        Field platformField = null;

        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(ENAPlatform.class)) {
                if (platformField == null) {
                    platformField = field;
                } else {
                    throw new IllegalArgumentException("Multiple platforms found in class");
                }
            }
        }

        Attribute attribute = new Attribute();

        attribute.setName(platformField.getAnnotation(ENAPlatform.class).name());
        String pt = (String) platformField.get(this);
        attribute.setValue(pt);
        getAttributes().add(attribute);
    }

    public SampleRef getSampleRef () {
        if (getSampleUses().isEmpty())
            return null;
        else
            return getSampleUses().get(0).getSampleRef();
    }

    @Override
    public String getTeamName() {
        Team team = getTeam();
        if (team != null)
            return team.getName();
        else return null;
    }

    @Override
    public void setTeamName(String teamName) {
        Team team = new Team();
        team.setName(teamName);
        setTeam(team);
    }

    public static class Single {}

    public static class PairedLibraryLayout {
        String nominalLength = null;
        String nominalSdev = null;

        public PairedLibraryLayout(String nominalLength, String nominalSdev) {
            this.nominalLength = nominalLength;
            this.nominalSdev = nominalSdev;
        }

        public PairedLibraryLayout () {}
    }
}

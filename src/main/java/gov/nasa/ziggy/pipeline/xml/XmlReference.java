package gov.nasa.ziggy.pipeline.xml;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import gov.nasa.ziggy.data.management.DataFileType;
import gov.nasa.ziggy.pipeline.definition.ModelType;

/**
 * Defines a named reference to a Java object in an XML file. This allows the XML files that define
 * a pipeline to use the name of an input file type, a model type, etc., which will be matched to an
 * appropriate {@link DataFileType}, {@link ModelType}, etc., after the XML is unmarshalled.
 * <p>
 * Because the XML "choice" option requires that each object in the choice have a different data
 * type, it is necessary to define subclasses of {@link XmlReference} that are used for information
 * exchange for each option in the choice.
 *
 * @author PT
 */
@XmlAccessorType(XmlAccessType.NONE)
public class XmlReference {

    @XmlAttribute(required = true)
    protected String name;

    public XmlReference() {
    }

    public XmlReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        XmlReference other = (XmlReference) obj;
        return Objects.equals(name, other.name);
    }

    public static class InputTypeReference extends XmlReference {
        public InputTypeReference() {
        }

        public InputTypeReference(String name) {
            super(name);
        }

    }

    public static class OutputTypeReference extends XmlReference {
        public OutputTypeReference() {
        }

        public OutputTypeReference(String name) {
            super(name);
        }

    }

    public static class ModelTypeReference extends XmlReference {
        public ModelTypeReference() {
        }

        public ModelTypeReference(String name) {
            super(name);
        }

    }
}

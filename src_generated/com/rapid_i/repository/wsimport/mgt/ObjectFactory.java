
package com.rapid_i.repository.wsimport.mgt;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rapid_i.repository.wsimport.mgt package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CheckSetupResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "checkSetupResponse");
    private final static QName _GetGlobalPropertyResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getGlobalPropertyResponse");
    private final static QName _GetGlobalProperty_QNAME = new QName("http://service.web.rapidanalytics.de/", "getGlobalProperty");
    private final static QName _CheckSetup_QNAME = new QName("http://service.web.rapidanalytics.de/", "checkSetup");
    private final static QName _SetGlobalPropertyResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "setGlobalPropertyResponse");
    private final static QName _SetGlobalProperty_QNAME = new QName("http://service.web.rapidanalytics.de/", "setGlobalProperty");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapid_i.repository.wsimport.mgt
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetGlobalPropertyResponse }
     * 
     */
    public GetGlobalPropertyResponse createGetGlobalPropertyResponse() {
        return new GetGlobalPropertyResponse();
    }

    /**
     * Create an instance of {@link GetGlobalProperty }
     * 
     */
    public GetGlobalProperty createGetGlobalProperty() {
        return new GetGlobalProperty();
    }

    /**
     * Create an instance of {@link SetGlobalProperty }
     * 
     */
    public SetGlobalProperty createSetGlobalProperty() {
        return new SetGlobalProperty();
    }

    /**
     * Create an instance of {@link CheckSetupResponse }
     * 
     */
    public CheckSetupResponse createCheckSetupResponse() {
        return new CheckSetupResponse();
    }

    /**
     * Create an instance of {@link SetGlobalPropertyResponse }
     * 
     */
    public SetGlobalPropertyResponse createSetGlobalPropertyResponse() {
        return new SetGlobalPropertyResponse();
    }

    /**
     * Create an instance of {@link CheckSetup }
     * 
     */
    public CheckSetup createCheckSetup() {
        return new CheckSetup();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckSetupResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "checkSetupResponse")
    public JAXBElement<CheckSetupResponse> createCheckSetupResponse(CheckSetupResponse value) {
        return new JAXBElement<CheckSetupResponse>(_CheckSetupResponse_QNAME, CheckSetupResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetGlobalPropertyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getGlobalPropertyResponse")
    public JAXBElement<GetGlobalPropertyResponse> createGetGlobalPropertyResponse(GetGlobalPropertyResponse value) {
        return new JAXBElement<GetGlobalPropertyResponse>(_GetGlobalPropertyResponse_QNAME, GetGlobalPropertyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetGlobalProperty }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getGlobalProperty")
    public JAXBElement<GetGlobalProperty> createGetGlobalProperty(GetGlobalProperty value) {
        return new JAXBElement<GetGlobalProperty>(_GetGlobalProperty_QNAME, GetGlobalProperty.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckSetup }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "checkSetup")
    public JAXBElement<CheckSetup> createCheckSetup(CheckSetup value) {
        return new JAXBElement<CheckSetup>(_CheckSetup_QNAME, CheckSetup.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetGlobalPropertyResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "setGlobalPropertyResponse")
    public JAXBElement<SetGlobalPropertyResponse> createSetGlobalPropertyResponse(SetGlobalPropertyResponse value) {
        return new JAXBElement<SetGlobalPropertyResponse>(_SetGlobalPropertyResponse_QNAME, SetGlobalPropertyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetGlobalProperty }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "setGlobalProperty")
    public JAXBElement<SetGlobalProperty> createSetGlobalProperty(SetGlobalProperty value) {
        return new JAXBElement<SetGlobalProperty>(_SetGlobalProperty_QNAME, SetGlobalProperty.class, null, value);
    }

}

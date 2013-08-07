
package com.rapid_i.repository.wsimport;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rapid_i.repository.wsimport package. 
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

    private final static QName _GetTotalMemory_QNAME = new QName("http://service.web.rapidanalytics.de/", "getTotalMemory");
    private final static QName _GetSystemLoadAverage_QNAME = new QName("http://service.web.rapidanalytics.de/", "getSystemLoadAverage");
    private final static QName _GetMaxMemory_QNAME = new QName("http://service.web.rapidanalytics.de/", "getMaxMemory");
    private final static QName _GetSystemLoadAverageResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getSystemLoadAverageResponse");
    private final static QName _GetVersionNumberResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getVersionNumberResponse");
    private final static QName _GetTotalMemoryResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getTotalMemoryResponse");
    private final static QName _GetMaxMemoryResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getMaxMemoryResponse");
    private final static QName _GetUpSince_QNAME = new QName("http://service.web.rapidanalytics.de/", "getUpSince");
    private final static QName _GetUpSinceResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getUpSinceResponse");
    private final static QName _GetFreeMemoryResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getFreeMemoryResponse");
    private final static QName _GetFreeMemory_QNAME = new QName("http://service.web.rapidanalytics.de/", "getFreeMemory");
    private final static QName _GetVersionNumber_QNAME = new QName("http://service.web.rapidanalytics.de/", "getVersionNumber");
    private final static QName _GetInstalledPlugins_QNAME = new QName("http://service.web.rapidanalytics.de/", "getInstalledPlugins");
    private final static QName _GetInstalledPluginsResponse_QNAME = new QName("http://service.web.rapidanalytics.de/", "getInstalledPluginsResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapid_i.repository.wsimport
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetMaxMemoryResponse }
     * 
     */
    public GetMaxMemoryResponse createGetMaxMemoryResponse() {
        return new GetMaxMemoryResponse();
    }

    /**
     * Create an instance of {@link GetFreeMemory }
     * 
     */
    public GetFreeMemory createGetFreeMemory() {
        return new GetFreeMemory();
    }

    /**
     * Create an instance of {@link GetTotalMemory }
     * 
     */
    public GetTotalMemory createGetTotalMemory() {
        return new GetTotalMemory();
    }

    /**
     * Create an instance of {@link GetUpSince }
     * 
     */
    public GetUpSince createGetUpSince() {
        return new GetUpSince();
    }

    /**
     * Create an instance of {@link GetInstalledPlugins }
     * 
     */
    public GetInstalledPlugins createGetInstalledPlugins() {
        return new GetInstalledPlugins();
    }

    /**
     * Create an instance of {@link GetVersionNumber }
     * 
     */
    public GetVersionNumber createGetVersionNumber() {
        return new GetVersionNumber();
    }

    /**
     * Create an instance of {@link GetSystemLoadAverageResponse }
     * 
     */
    public GetSystemLoadAverageResponse createGetSystemLoadAverageResponse() {
        return new GetSystemLoadAverageResponse();
    }

    /**
     * Create an instance of {@link GetMaxMemory }
     * 
     */
    public GetMaxMemory createGetMaxMemory() {
        return new GetMaxMemory();
    }

    /**
     * Create an instance of {@link GetUpSinceResponse }
     * 
     */
    public GetUpSinceResponse createGetUpSinceResponse() {
        return new GetUpSinceResponse();
    }

    /**
     * Create an instance of {@link GetVersionNumberResponse }
     * 
     */
    public GetVersionNumberResponse createGetVersionNumberResponse() {
        return new GetVersionNumberResponse();
    }

    /**
     * Create an instance of {@link GetSystemLoadAverage }
     * 
     */
    public GetSystemLoadAverage createGetSystemLoadAverage() {
        return new GetSystemLoadAverage();
    }

    /**
     * Create an instance of {@link GetTotalMemoryResponse }
     * 
     */
    public GetTotalMemoryResponse createGetTotalMemoryResponse() {
        return new GetTotalMemoryResponse();
    }

    /**
     * Create an instance of {@link GetFreeMemoryResponse }
     * 
     */
    public GetFreeMemoryResponse createGetFreeMemoryResponse() {
        return new GetFreeMemoryResponse();
    }

    /**
     * Create an instance of {@link PluginInfo }
     * 
     */
    public PluginInfo createPluginInfo() {
        return new PluginInfo();
    }

    /**
     * Create an instance of {@link GetInstalledPluginsResponse }
     * 
     */
    public GetInstalledPluginsResponse createGetInstalledPluginsResponse() {
        return new GetInstalledPluginsResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTotalMemory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getTotalMemory")
    public JAXBElement<GetTotalMemory> createGetTotalMemory(GetTotalMemory value) {
        return new JAXBElement<GetTotalMemory>(_GetTotalMemory_QNAME, GetTotalMemory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSystemLoadAverage }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getSystemLoadAverage")
    public JAXBElement<GetSystemLoadAverage> createGetSystemLoadAverage(GetSystemLoadAverage value) {
        return new JAXBElement<GetSystemLoadAverage>(_GetSystemLoadAverage_QNAME, GetSystemLoadAverage.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMaxMemory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getMaxMemory")
    public JAXBElement<GetMaxMemory> createGetMaxMemory(GetMaxMemory value) {
        return new JAXBElement<GetMaxMemory>(_GetMaxMemory_QNAME, GetMaxMemory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSystemLoadAverageResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getSystemLoadAverageResponse")
    public JAXBElement<GetSystemLoadAverageResponse> createGetSystemLoadAverageResponse(GetSystemLoadAverageResponse value) {
        return new JAXBElement<GetSystemLoadAverageResponse>(_GetSystemLoadAverageResponse_QNAME, GetSystemLoadAverageResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersionNumberResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getVersionNumberResponse")
    public JAXBElement<GetVersionNumberResponse> createGetVersionNumberResponse(GetVersionNumberResponse value) {
        return new JAXBElement<GetVersionNumberResponse>(_GetVersionNumberResponse_QNAME, GetVersionNumberResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTotalMemoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getTotalMemoryResponse")
    public JAXBElement<GetTotalMemoryResponse> createGetTotalMemoryResponse(GetTotalMemoryResponse value) {
        return new JAXBElement<GetTotalMemoryResponse>(_GetTotalMemoryResponse_QNAME, GetTotalMemoryResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMaxMemoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getMaxMemoryResponse")
    public JAXBElement<GetMaxMemoryResponse> createGetMaxMemoryResponse(GetMaxMemoryResponse value) {
        return new JAXBElement<GetMaxMemoryResponse>(_GetMaxMemoryResponse_QNAME, GetMaxMemoryResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUpSince }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getUpSince")
    public JAXBElement<GetUpSince> createGetUpSince(GetUpSince value) {
        return new JAXBElement<GetUpSince>(_GetUpSince_QNAME, GetUpSince.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUpSinceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getUpSinceResponse")
    public JAXBElement<GetUpSinceResponse> createGetUpSinceResponse(GetUpSinceResponse value) {
        return new JAXBElement<GetUpSinceResponse>(_GetUpSinceResponse_QNAME, GetUpSinceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFreeMemoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getFreeMemoryResponse")
    public JAXBElement<GetFreeMemoryResponse> createGetFreeMemoryResponse(GetFreeMemoryResponse value) {
        return new JAXBElement<GetFreeMemoryResponse>(_GetFreeMemoryResponse_QNAME, GetFreeMemoryResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFreeMemory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getFreeMemory")
    public JAXBElement<GetFreeMemory> createGetFreeMemory(GetFreeMemory value) {
        return new JAXBElement<GetFreeMemory>(_GetFreeMemory_QNAME, GetFreeMemory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersionNumber }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getVersionNumber")
    public JAXBElement<GetVersionNumber> createGetVersionNumber(GetVersionNumber value) {
        return new JAXBElement<GetVersionNumber>(_GetVersionNumber_QNAME, GetVersionNumber.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetInstalledPlugins }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getInstalledPlugins")
    public JAXBElement<GetInstalledPlugins> createGetInstalledPlugins(GetInstalledPlugins value) {
        return new JAXBElement<GetInstalledPlugins>(_GetInstalledPlugins_QNAME, GetInstalledPlugins.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetInstalledPluginsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.web.rapidanalytics.de/", name = "getInstalledPluginsResponse")
    public JAXBElement<GetInstalledPluginsResponse> createGetInstalledPluginsResponse(GetInstalledPluginsResponse value) {
        return new JAXBElement<GetInstalledPluginsResponse>(_GetInstalledPluginsResponse_QNAME, GetInstalledPluginsResponse.class, null, value);
    }

}

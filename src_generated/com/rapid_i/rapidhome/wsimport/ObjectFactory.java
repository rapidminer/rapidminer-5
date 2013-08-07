
package com.rapid_i.rapidhome.wsimport;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rapid_i.rapidhome.wsimport package. 
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

    private final static QName _UploadUsageStatisticsResponse_QNAME = new QName("http://ws.rapidhome.rapid_i.com/", "uploadUsageStatisticsResponse");
    private final static QName _UploadUsageStatistics_QNAME = new QName("http://ws.rapidhome.rapid_i.com/", "uploadUsageStatistics");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapid_i.rapidhome.wsimport
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link UploadUsageStatisticsResponse }
     * 
     */
    public UploadUsageStatisticsResponse createUploadUsageStatisticsResponse() {
        return new UploadUsageStatisticsResponse();
    }

    /**
     * Create an instance of {@link UploadUsageStatistics }
     * 
     */
    public UploadUsageStatistics createUploadUsageStatistics() {
        return new UploadUsageStatistics();
    }

    /**
     * Create an instance of {@link StatisticsReport }
     * 
     */
    public StatisticsReport createStatisticsReport() {
        return new StatisticsReport();
    }

    /**
     * Create an instance of {@link StatisticsRecord }
     * 
     */
    public StatisticsRecord createStatisticsRecord() {
        return new StatisticsRecord();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadUsageStatisticsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.rapidhome.rapid_i.com/", name = "uploadUsageStatisticsResponse")
    public JAXBElement<UploadUsageStatisticsResponse> createUploadUsageStatisticsResponse(UploadUsageStatisticsResponse value) {
        return new JAXBElement<UploadUsageStatisticsResponse>(_UploadUsageStatisticsResponse_QNAME, UploadUsageStatisticsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UploadUsageStatistics }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.rapidhome.rapid_i.com/", name = "uploadUsageStatistics")
    public JAXBElement<UploadUsageStatistics> createUploadUsageStatistics(UploadUsageStatistics value) {
        return new JAXBElement<UploadUsageStatistics>(_UploadUsageStatistics_QNAME, UploadUsageStatistics.class, null, value);
    }

}

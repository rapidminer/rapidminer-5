
package com.rapidminer.deployment.client.wsimport;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.rapidminer.deployment.client.wsimport package. 
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

    private final static QName _GetRapidMinerExtensionForOperatorPrefixResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getRapidMinerExtensionForOperatorPrefixResponse");
    private final static QName _GetTopRated_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getTopRated");
    private final static QName _GetTopDownloadsResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getTopDownloadsResponse");
    private final static QName _UpdateServiceException_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "UpdateServiceException");
    private final static QName _GetLatestVersionResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLatestVersionResponse");
    private final static QName _GetMirrors_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getMirrors");
    private final static QName _GetLatestVersion_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLatestVersion");
    private final static QName _GetExtensions_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getExtensions");
    private final static QName _GetMessageOfTheDayResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getMessageOfTheDayResponse");
    private final static QName _SearchForResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "searchForResponse");
    private final static QName _AnyUpdatesSinceResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "anyUpdatesSinceResponse");
    private final static QName _GetRapidMinerExtensionForOperatorPrefix_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getRapidMinerExtensionForOperatorPrefix");
    private final static QName _GetPackageInfoResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getPackageInfoResponse");
    private final static QName _GetLicenseTextHtml_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLicenseTextHtml");
    private final static QName _GetLicenseText_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLicenseText");
    private final static QName _GetMirrorsResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getMirrorsResponse");
    private final static QName _GetTopDownloads_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getTopDownloads");
    private final static QName _GetDownloadURL_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getDownloadURL");
    private final static QName _GetTopRatedResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getTopRatedResponse");
    private final static QName _GetDownloadURLResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getDownloadURLResponse");
    private final static QName _SearchFor_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "searchFor");
    private final static QName _GetAvailableVersions_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getAvailableVersions");
    private final static QName _AnyUpdatesSince_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "anyUpdatesSince");
    private final static QName _GetAvailableVersionsResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getAvailableVersionsResponse");
    private final static QName _GetExtensionsResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getExtensionsResponse");
    private final static QName _GetPackageInfo_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getPackageInfo");
    private final static QName _GetLicenseTextResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLicenseTextResponse");
    private final static QName _GetMessageOfTheDay_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getMessageOfTheDay");
    private final static QName _GetLicenseTextHtmlResponse_QNAME = new QName("http://ws.update.deployment.rapid_i.com/", "getLicenseTextHtmlResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.rapidminer.deployment.client.wsimport
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetMessageOfTheDay }
     * 
     */
    public GetMessageOfTheDay createGetMessageOfTheDay() {
        return new GetMessageOfTheDay();
    }

    /**
     * Create an instance of {@link SearchFor }
     * 
     */
    public SearchFor createSearchFor() {
        return new SearchFor();
    }

    /**
     * Create an instance of {@link GetDownloadURL }
     * 
     */
    public GetDownloadURL createGetDownloadURL() {
        return new GetDownloadURL();
    }

    /**
     * Create an instance of {@link GetTopRatedResponse }
     * 
     */
    public GetTopRatedResponse createGetTopRatedResponse() {
        return new GetTopRatedResponse();
    }

    /**
     * Create an instance of {@link GetMessageOfTheDayResponse }
     * 
     */
    public GetMessageOfTheDayResponse createGetMessageOfTheDayResponse() {
        return new GetMessageOfTheDayResponse();
    }

    /**
     * Create an instance of {@link GetRapidMinerExtensionForOperatorPrefix }
     * 
     */
    public GetRapidMinerExtensionForOperatorPrefix createGetRapidMinerExtensionForOperatorPrefix() {
        return new GetRapidMinerExtensionForOperatorPrefix();
    }

    /**
     * Create an instance of {@link GetTopRated }
     * 
     */
    public GetTopRated createGetTopRated() {
        return new GetTopRated();
    }

    /**
     * Create an instance of {@link GetPackageInfo }
     * 
     */
    public GetPackageInfo createGetPackageInfo() {
        return new GetPackageInfo();
    }

    /**
     * Create an instance of {@link GetDownloadURLResponse }
     * 
     */
    public GetDownloadURLResponse createGetDownloadURLResponse() {
        return new GetDownloadURLResponse();
    }

    /**
     * Create an instance of {@link AnyUpdatesSince }
     * 
     */
    public AnyUpdatesSince createAnyUpdatesSince() {
        return new AnyUpdatesSince();
    }

    /**
     * Create an instance of {@link GetLicenseTextResponse }
     * 
     */
    public GetLicenseTextResponse createGetLicenseTextResponse() {
        return new GetLicenseTextResponse();
    }

    /**
     * Create an instance of {@link GetTopDownloadsResponse }
     * 
     */
    public GetTopDownloadsResponse createGetTopDownloadsResponse() {
        return new GetTopDownloadsResponse();
    }

    /**
     * Create an instance of {@link GetLicenseTextHtmlResponse }
     * 
     */
    public GetLicenseTextHtmlResponse createGetLicenseTextHtmlResponse() {
        return new GetLicenseTextHtmlResponse();
    }

    /**
     * Create an instance of {@link GetMirrorsResponse }
     * 
     */
    public GetMirrorsResponse createGetMirrorsResponse() {
        return new GetMirrorsResponse();
    }

    /**
     * Create an instance of {@link GetLicenseTextHtml }
     * 
     */
    public GetLicenseTextHtml createGetLicenseTextHtml() {
        return new GetLicenseTextHtml();
    }

    /**
     * Create an instance of {@link GetExtensions }
     * 
     */
    public GetExtensions createGetExtensions() {
        return new GetExtensions();
    }

    /**
     * Create an instance of {@link GetLatestVersionResponse }
     * 
     */
    public GetLatestVersionResponse createGetLatestVersionResponse() {
        return new GetLatestVersionResponse();
    }

    /**
     * Create an instance of {@link PackageDescriptor }
     * 
     */
    public PackageDescriptor createPackageDescriptor() {
        return new PackageDescriptor();
    }

    /**
     * Create an instance of {@link GetTopDownloads }
     * 
     */
    public GetTopDownloads createGetTopDownloads() {
        return new GetTopDownloads();
    }

    /**
     * Create an instance of {@link GetLatestVersion }
     * 
     */
    public GetLatestVersion createGetLatestVersion() {
        return new GetLatestVersion();
    }

    /**
     * Create an instance of {@link GetRapidMinerExtensionForOperatorPrefixResponse }
     * 
     */
    public GetRapidMinerExtensionForOperatorPrefixResponse createGetRapidMinerExtensionForOperatorPrefixResponse() {
        return new GetRapidMinerExtensionForOperatorPrefixResponse();
    }

    /**
     * Create an instance of {@link UpdateServiceException }
     * 
     */
    public UpdateServiceException createUpdateServiceException() {
        return new UpdateServiceException();
    }

    /**
     * Create an instance of {@link SearchForResponse }
     * 
     */
    public SearchForResponse createSearchForResponse() {
        return new SearchForResponse();
    }

    /**
     * Create an instance of {@link AnyUpdatesSinceResponse }
     * 
     */
    public AnyUpdatesSinceResponse createAnyUpdatesSinceResponse() {
        return new AnyUpdatesSinceResponse();
    }

    /**
     * Create an instance of {@link GetAvailableVersionsResponse }
     * 
     */
    public GetAvailableVersionsResponse createGetAvailableVersionsResponse() {
        return new GetAvailableVersionsResponse();
    }

    /**
     * Create an instance of {@link GetAvailableVersions }
     * 
     */
    public GetAvailableVersions createGetAvailableVersions() {
        return new GetAvailableVersions();
    }

    /**
     * Create an instance of {@link GetExtensionsResponse }
     * 
     */
    public GetExtensionsResponse createGetExtensionsResponse() {
        return new GetExtensionsResponse();
    }

    /**
     * Create an instance of {@link GetMirrors }
     * 
     */
    public GetMirrors createGetMirrors() {
        return new GetMirrors();
    }

    /**
     * Create an instance of {@link GetLicenseText }
     * 
     */
    public GetLicenseText createGetLicenseText() {
        return new GetLicenseText();
    }

    /**
     * Create an instance of {@link GetPackageInfoResponse }
     * 
     */
    public GetPackageInfoResponse createGetPackageInfoResponse() {
        return new GetPackageInfoResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRapidMinerExtensionForOperatorPrefixResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getRapidMinerExtensionForOperatorPrefixResponse")
    public JAXBElement<GetRapidMinerExtensionForOperatorPrefixResponse> createGetRapidMinerExtensionForOperatorPrefixResponse(GetRapidMinerExtensionForOperatorPrefixResponse value) {
        return new JAXBElement<GetRapidMinerExtensionForOperatorPrefixResponse>(_GetRapidMinerExtensionForOperatorPrefixResponse_QNAME, GetRapidMinerExtensionForOperatorPrefixResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTopRated }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getTopRated")
    public JAXBElement<GetTopRated> createGetTopRated(GetTopRated value) {
        return new JAXBElement<GetTopRated>(_GetTopRated_QNAME, GetTopRated.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTopDownloadsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getTopDownloadsResponse")
    public JAXBElement<GetTopDownloadsResponse> createGetTopDownloadsResponse(GetTopDownloadsResponse value) {
        return new JAXBElement<GetTopDownloadsResponse>(_GetTopDownloadsResponse_QNAME, GetTopDownloadsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateServiceException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "UpdateServiceException")
    public JAXBElement<UpdateServiceException> createUpdateServiceException(UpdateServiceException value) {
        return new JAXBElement<UpdateServiceException>(_UpdateServiceException_QNAME, UpdateServiceException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLatestVersionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLatestVersionResponse")
    public JAXBElement<GetLatestVersionResponse> createGetLatestVersionResponse(GetLatestVersionResponse value) {
        return new JAXBElement<GetLatestVersionResponse>(_GetLatestVersionResponse_QNAME, GetLatestVersionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMirrors }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getMirrors")
    public JAXBElement<GetMirrors> createGetMirrors(GetMirrors value) {
        return new JAXBElement<GetMirrors>(_GetMirrors_QNAME, GetMirrors.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLatestVersion }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLatestVersion")
    public JAXBElement<GetLatestVersion> createGetLatestVersion(GetLatestVersion value) {
        return new JAXBElement<GetLatestVersion>(_GetLatestVersion_QNAME, GetLatestVersion.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetExtensions }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getExtensions")
    public JAXBElement<GetExtensions> createGetExtensions(GetExtensions value) {
        return new JAXBElement<GetExtensions>(_GetExtensions_QNAME, GetExtensions.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMessageOfTheDayResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getMessageOfTheDayResponse")
    public JAXBElement<GetMessageOfTheDayResponse> createGetMessageOfTheDayResponse(GetMessageOfTheDayResponse value) {
        return new JAXBElement<GetMessageOfTheDayResponse>(_GetMessageOfTheDayResponse_QNAME, GetMessageOfTheDayResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchForResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "searchForResponse")
    public JAXBElement<SearchForResponse> createSearchForResponse(SearchForResponse value) {
        return new JAXBElement<SearchForResponse>(_SearchForResponse_QNAME, SearchForResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AnyUpdatesSinceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "anyUpdatesSinceResponse")
    public JAXBElement<AnyUpdatesSinceResponse> createAnyUpdatesSinceResponse(AnyUpdatesSinceResponse value) {
        return new JAXBElement<AnyUpdatesSinceResponse>(_AnyUpdatesSinceResponse_QNAME, AnyUpdatesSinceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetRapidMinerExtensionForOperatorPrefix }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getRapidMinerExtensionForOperatorPrefix")
    public JAXBElement<GetRapidMinerExtensionForOperatorPrefix> createGetRapidMinerExtensionForOperatorPrefix(GetRapidMinerExtensionForOperatorPrefix value) {
        return new JAXBElement<GetRapidMinerExtensionForOperatorPrefix>(_GetRapidMinerExtensionForOperatorPrefix_QNAME, GetRapidMinerExtensionForOperatorPrefix.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPackageInfoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getPackageInfoResponse")
    public JAXBElement<GetPackageInfoResponse> createGetPackageInfoResponse(GetPackageInfoResponse value) {
        return new JAXBElement<GetPackageInfoResponse>(_GetPackageInfoResponse_QNAME, GetPackageInfoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLicenseTextHtml }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLicenseTextHtml")
    public JAXBElement<GetLicenseTextHtml> createGetLicenseTextHtml(GetLicenseTextHtml value) {
        return new JAXBElement<GetLicenseTextHtml>(_GetLicenseTextHtml_QNAME, GetLicenseTextHtml.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLicenseText }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLicenseText")
    public JAXBElement<GetLicenseText> createGetLicenseText(GetLicenseText value) {
        return new JAXBElement<GetLicenseText>(_GetLicenseText_QNAME, GetLicenseText.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMirrorsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getMirrorsResponse")
    public JAXBElement<GetMirrorsResponse> createGetMirrorsResponse(GetMirrorsResponse value) {
        return new JAXBElement<GetMirrorsResponse>(_GetMirrorsResponse_QNAME, GetMirrorsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTopDownloads }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getTopDownloads")
    public JAXBElement<GetTopDownloads> createGetTopDownloads(GetTopDownloads value) {
        return new JAXBElement<GetTopDownloads>(_GetTopDownloads_QNAME, GetTopDownloads.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetDownloadURL }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getDownloadURL")
    public JAXBElement<GetDownloadURL> createGetDownloadURL(GetDownloadURL value) {
        return new JAXBElement<GetDownloadURL>(_GetDownloadURL_QNAME, GetDownloadURL.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTopRatedResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getTopRatedResponse")
    public JAXBElement<GetTopRatedResponse> createGetTopRatedResponse(GetTopRatedResponse value) {
        return new JAXBElement<GetTopRatedResponse>(_GetTopRatedResponse_QNAME, GetTopRatedResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetDownloadURLResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getDownloadURLResponse")
    public JAXBElement<GetDownloadURLResponse> createGetDownloadURLResponse(GetDownloadURLResponse value) {
        return new JAXBElement<GetDownloadURLResponse>(_GetDownloadURLResponse_QNAME, GetDownloadURLResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchFor }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "searchFor")
    public JAXBElement<SearchFor> createSearchFor(SearchFor value) {
        return new JAXBElement<SearchFor>(_SearchFor_QNAME, SearchFor.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAvailableVersions }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getAvailableVersions")
    public JAXBElement<GetAvailableVersions> createGetAvailableVersions(GetAvailableVersions value) {
        return new JAXBElement<GetAvailableVersions>(_GetAvailableVersions_QNAME, GetAvailableVersions.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AnyUpdatesSince }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "anyUpdatesSince")
    public JAXBElement<AnyUpdatesSince> createAnyUpdatesSince(AnyUpdatesSince value) {
        return new JAXBElement<AnyUpdatesSince>(_AnyUpdatesSince_QNAME, AnyUpdatesSince.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAvailableVersionsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getAvailableVersionsResponse")
    public JAXBElement<GetAvailableVersionsResponse> createGetAvailableVersionsResponse(GetAvailableVersionsResponse value) {
        return new JAXBElement<GetAvailableVersionsResponse>(_GetAvailableVersionsResponse_QNAME, GetAvailableVersionsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetExtensionsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getExtensionsResponse")
    public JAXBElement<GetExtensionsResponse> createGetExtensionsResponse(GetExtensionsResponse value) {
        return new JAXBElement<GetExtensionsResponse>(_GetExtensionsResponse_QNAME, GetExtensionsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPackageInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getPackageInfo")
    public JAXBElement<GetPackageInfo> createGetPackageInfo(GetPackageInfo value) {
        return new JAXBElement<GetPackageInfo>(_GetPackageInfo_QNAME, GetPackageInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLicenseTextResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLicenseTextResponse")
    public JAXBElement<GetLicenseTextResponse> createGetLicenseTextResponse(GetLicenseTextResponse value) {
        return new JAXBElement<GetLicenseTextResponse>(_GetLicenseTextResponse_QNAME, GetLicenseTextResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMessageOfTheDay }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getMessageOfTheDay")
    public JAXBElement<GetMessageOfTheDay> createGetMessageOfTheDay(GetMessageOfTheDay value) {
        return new JAXBElement<GetMessageOfTheDay>(_GetMessageOfTheDay_QNAME, GetMessageOfTheDay.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLicenseTextHtmlResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.update.deployment.rapid_i.com/", name = "getLicenseTextHtmlResponse")
    public JAXBElement<GetLicenseTextHtmlResponse> createGetLicenseTextHtmlResponse(GetLicenseTextHtmlResponse value) {
        return new JAXBElement<GetLicenseTextHtmlResponse>(_GetLicenseTextHtmlResponse_QNAME, GetLicenseTextHtmlResponse.class, null, value);
    }

}

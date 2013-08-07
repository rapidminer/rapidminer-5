
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for executionResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="executionResponse">
 *   &lt;complexContent>
 *     &lt;extension base="{http://service.web.rapidanalytics.de/}response">
 *       &lt;sequence>
 *         &lt;element name="firstExecution" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="jobId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "executionResponse", propOrder = {
    "firstExecution",
    "jobId"
})
public class ExecutionResponse
    extends Response
{

    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar firstExecution;
    protected int jobId;

    /**
     * Gets the value of the firstExecution property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getFirstExecution() {
        return firstExecution;
    }

    /**
     * Sets the value of the firstExecution property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setFirstExecution(XMLGregorianCalendar value) {
        this.firstExecution = value;
    }

    /**
     * Gets the value of the jobId property.
     * 
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * Sets the value of the jobId property.
     * 
     */
    public void setJobId(int value) {
        this.jobId = value;
    }

}


package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getProcessContents complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getProcessContents">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="entryLocation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="revisionNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getProcessContents", propOrder = {
    "entryLocation",
    "revisionNumber"
})
public class GetProcessContents {

    protected String entryLocation;
    protected int revisionNumber;

    /**
     * Gets the value of the entryLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEntryLocation() {
        return entryLocation;
    }

    /**
     * Sets the value of the entryLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEntryLocation(String value) {
        this.entryLocation = value;
    }

    /**
     * Gets the value of the revisionNumber property.
     * 
     */
    public int getRevisionNumber() {
        return revisionNumber;
    }

    /**
     * Sets the value of the revisionNumber property.
     * 
     */
    public void setRevisionNumber(int value) {
        this.revisionNumber = value;
    }

}

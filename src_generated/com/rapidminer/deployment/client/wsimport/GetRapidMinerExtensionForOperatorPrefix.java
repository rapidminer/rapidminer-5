
package com.rapidminer.deployment.client.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getRapidMinerExtensionForOperatorPrefix complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getRapidMinerExtensionForOperatorPrefix">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="forPrefix" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getRapidMinerExtensionForOperatorPrefix", propOrder = {
    "forPrefix"
})
public class GetRapidMinerExtensionForOperatorPrefix {

    protected String forPrefix;

    /**
     * Gets the value of the forPrefix property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForPrefix() {
        return forPrefix;
    }

    /**
     * Sets the value of the forPrefix property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForPrefix(String value) {
        this.forPrefix = value;
    }

}

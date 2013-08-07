
package com.rapid_i.repository.wsimport;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for setAccessRights complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="setAccessRights">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="entryLocation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="accessRights" type="{http://service.web.rapidanalytics.de/}accessRights" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setAccessRights", propOrder = {
    "entryLocation",
    "accessRights"
})
public class SetAccessRights {

    protected String entryLocation;
    protected List<AccessRights> accessRights;

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
     * Gets the value of the accessRights property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessRights property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessRights().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessRights }
     * 
     * 
     */
    public List<AccessRights> getAccessRights() {
        if (accessRights == null) {
            accessRights = new ArrayList<AccessRights>();
        }
        return this.accessRights;
    }

}

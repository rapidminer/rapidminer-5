
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getRunningProcessesInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getRunningProcessesInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="scheduledProcessId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getRunningProcessesInfo", propOrder = {
    "scheduledProcessId"
})
public class GetRunningProcessesInfo {

    protected int scheduledProcessId;

    /**
     * Gets the value of the scheduledProcessId property.
     * 
     */
    public int getScheduledProcessId() {
        return scheduledProcessId;
    }

    /**
     * Sets the value of the scheduledProcessId property.
     * 
     */
    public void setScheduledProcessId(int value) {
        this.scheduledProcessId = value;
    }

}

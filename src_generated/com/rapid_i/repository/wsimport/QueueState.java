
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for queueState complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="queueState">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="backlog" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="numberOfRunningProcesses" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "queueState", propOrder = {
    "backlog",
    "numberOfRunningProcesses"
})
public class QueueState {

    protected int backlog;
    protected int numberOfRunningProcesses;

    /**
     * Gets the value of the backlog property.
     * 
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the value of the backlog property.
     * 
     */
    public void setBacklog(int value) {
        this.backlog = value;
    }

    /**
     * Gets the value of the numberOfRunningProcesses property.
     * 
     */
    public int getNumberOfRunningProcesses() {
        return numberOfRunningProcesses;
    }

    /**
     * Sets the value of the numberOfRunningProcesses property.
     * 
     */
    public void setNumberOfRunningProcesses(int value) {
        this.numberOfRunningProcesses = value;
    }

}

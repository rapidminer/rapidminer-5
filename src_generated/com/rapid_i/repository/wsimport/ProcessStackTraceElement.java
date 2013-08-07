
package com.rapid_i.repository.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for processStackTraceElement complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="processStackTraceElement">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="applyCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="executionTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="operatorName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processStackTraceElement", propOrder = {
    "applyCount",
    "executionTime",
    "operatorName"
})
public class ProcessStackTraceElement {

    protected int applyCount;
    protected long executionTime;
    protected String operatorName;

    /**
     * Gets the value of the applyCount property.
     * 
     */
    public int getApplyCount() {
        return applyCount;
    }

    /**
     * Sets the value of the applyCount property.
     * 
     */
    public void setApplyCount(int value) {
        this.applyCount = value;
    }

    /**
     * Gets the value of the executionTime property.
     * 
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Sets the value of the executionTime property.
     * 
     */
    public void setExecutionTime(long value) {
        this.executionTime = value;
    }

    /**
     * Gets the value of the operatorName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Sets the value of the operatorName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperatorName(String value) {
        this.operatorName = value;
    }

}

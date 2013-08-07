
package com.rapid_i.rapidhome.wsimport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for statisticsRecord complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="statisticsRecord">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="execution" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="failure" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="operatorException" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="operatorName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="runtimeError" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="stop" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="userError" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "statisticsRecord", propOrder = {
    "execution",
    "failure",
    "id",
    "operatorException",
    "operatorName",
    "runtimeError",
    "stop",
    "userError"
})
public class StatisticsRecord {

    protected int execution;
    protected int failure;
    protected int id;
    protected int operatorException;
    protected String operatorName;
    protected int runtimeError;
    protected int stop;
    protected int userError;

    /**
     * Gets the value of the execution property.
     * 
     */
    public int getExecution() {
        return execution;
    }

    /**
     * Sets the value of the execution property.
     * 
     */
    public void setExecution(int value) {
        this.execution = value;
    }

    /**
     * Gets the value of the failure property.
     * 
     */
    public int getFailure() {
        return failure;
    }

    /**
     * Sets the value of the failure property.
     * 
     */
    public void setFailure(int value) {
        this.failure = value;
    }

    /**
     * Gets the value of the id property.
     * 
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     */
    public void setId(int value) {
        this.id = value;
    }

    /**
     * Gets the value of the operatorException property.
     * 
     */
    public int getOperatorException() {
        return operatorException;
    }

    /**
     * Sets the value of the operatorException property.
     * 
     */
    public void setOperatorException(int value) {
        this.operatorException = value;
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

    /**
     * Gets the value of the runtimeError property.
     * 
     */
    public int getRuntimeError() {
        return runtimeError;
    }

    /**
     * Sets the value of the runtimeError property.
     * 
     */
    public void setRuntimeError(int value) {
        this.runtimeError = value;
    }

    /**
     * Gets the value of the stop property.
     * 
     */
    public int getStop() {
        return stop;
    }

    /**
     * Sets the value of the stop property.
     * 
     */
    public void setStop(int value) {
        this.stop = value;
    }

    /**
     * Gets the value of the userError property.
     * 
     */
    public int getUserError() {
        return userError;
    }

    /**
     * Sets the value of the userError property.
     * 
     */
    public void setUserError(int value) {
        this.userError = value;
    }

}

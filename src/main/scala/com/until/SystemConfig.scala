package com.until

import java.io.{BufferedInputStream, File, FileInputStream}
import java.util
import java.util.Properties

import org.slf4j.{Logger, LoggerFactory}
import sun.security.util.PropertyExpander

/**
  * @Author: king
  * @Date: 2019-01-10
  * @Desc: 配置文件获取工具类
  */

object SystemConfig {
  private var mConfig: Properties = null
  private val log: Logger = LoggerFactory.getLogger(SystemConfig.getClass)

  mConfig = new Properties()
  try {
    try {
      mConfig.load(new BufferedInputStream(new FileInputStream(System.getProperty("user.dir") + File.separator + "application.properties")))
    } catch {
      case _: Exception => println(_)
        try {
          mConfig.load(new BufferedInputStream(
            new FileInputStream(
              System.getProperty("user.dir") + File.separator + "conf" + File.separator + "application.properties")))
        } catch {
          case _: Exception => println(_)
            try {

              mConfig.load(SystemConfig.getClass.getClassLoader().getResourceAsStream("application.properties"))
            } catch {
              case _: Exception => println(_)
            }
        }
    }
    log.info("successfully loaded default properties.")

    // Now expand system properties for properties in the
    // config.expandedProperties list,
    // replacing them by their expanded values.
    val expandedPropertiesDef: String = mConfig.get("config.expandedProperties").toString
    if (expandedPropertiesDef != null) {
      val expandedProperties: Array[String] = expandedPropertiesDef.split(",")
      for (i <- 0 to expandedProperties.length) {
        val propName: String = expandedProperties(i).trim()
        val initialValue: String = mConfig.get(propName).toString
        if (initialValue != null) {
          val expandedValue: String = PropertyExpander.expand(initialValue)
          mConfig.put(propName, expandedValue)
          if (log.isDebugEnabled()) {
            log.info("Expanded value of " + propName + " from '" + initialValue + "' to '" + expandedValue + "'")
          }
        }
      }
    }

    // some debugging for those that want it
    if (log.isDebugEnabled()) {
      log.debug("SystemConfig looks like this ...")
      var key: String = null
      val keys = mConfig.keys()
      while (keys.hasMoreElements()) {
        key = keys.nextElement().toString
        log.debug(key + "=" + mConfig.getProperty(key))
      }
    }

  } catch {
    case _: Exception => println(_)
  }


  // no, you may not instantiate this class :p
  //private SystemConfig() {}

  /**
    * Retrieve a property value
    *
    * @param key Name of the property
    * @return String Value of property requested, null if not found
    */
  def getProperty(key: String): String = {
    mConfig.getProperty(key)
  }

  def getProperty(key: String, defaultValue: String): String = {
    log.debug("Fetching property [" + key + "=" + mConfig.getProperty(key) + "]")
    val value: String = SystemConfig.getProperty(key)
    if (value == null) return defaultValue
    value
  }

  /**
    * Retrieve a property as a boolean ... defaults to false if not present.
    */
  def getBooleanProperty(name: String): Boolean = {
    getBooleanProperty(name, defaultValue = false)
  }

  /**
    * Retrieve a property as a boolean ... with specified default if not present.
    */
  def getBooleanProperty(name: String, defaultValue: Boolean): Boolean = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)
    if (value == null) return defaultValue
    boolean2Boolean(x = new Boolean {
      value
    }).booleanValue()
  }

  /**
    * Retrieve a property as a int ... defaults to 0 if not present.
    *
    * @param name
    * @return
    */
  def getIntProperty(name: String): Int = {
    getIntProperty(name, 0)
  }

  /**
    * 返回指定默认�?
    *
    * @param name
    * @param defaultValue
    * @return
    */
  def getIntProperty(name: String, defaultValue: Int): Int = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)

    if (value == null) return defaultValue

    try {
      Integer.parseInt(value)
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }

  /**
    * 返回指定默认值的int数组
    *
    * @param name
    * @param defaultValue
    * @return int[]
    * @author wondtech liangming
    * @date 2008-07-25
    */
  def getIntPropertyArray(name: String, defaultValue: Array[Int]): Array[Int] = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)

    if (value == null) return defaultValue

    try {
      val propertyArray: Array[String] = value.split(",") // 将字符用逗开分离
      val result: Array[Int] = new Array[Int](propertyArray.length)
      for (i <- 0 to propertyArray.length) {
        //
        result(i) = Integer.parseInt(propertyArray(i))
      }
      result
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }

  /**
    * 返回指定默认值的boolean数组
    *
    * @param name
    * @param defaultValue
    * @return boolean[]
    * @author wondtech liangming
    * @date 2008-07-25
    */
  def getBooleanPropertyArray(name: String, defaultValue: Array[Boolean]): Array[Boolean] = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)
    if (value == null) return defaultValue
    try {
      val propertyArray: Array[String] = value.split(",") // 将字符用逗开分离
      val result: Array[Boolean] = new Array[Boolean](propertyArray.length)
      for (i <- 0 to propertyArray.length) {
        result(i) = boolean2Boolean(x = new Boolean {
          propertyArray(i)
        }).booleanValue()
      }
      result
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }

  /**
    * 返回指定默认值的str数组
    *
    * @param name
    * @param defaultValue
    * @return String[]
    * @author wondtech liangming
    * @date 2008-07-25
    */
  def getPropertyArray(name: String, defaultValue: Array[String]): Array[String] = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)
    if (value == null) return defaultValue
    try {
      val propertyArray: Array[String] = value.split(",") // 将字符用逗开分离
      propertyArray
    } catch {
      case _: NumberFormatException => defaultValue
    }
  }

  /**
    * 返回指定默认值的str数组
    *
    * @param name
    * @return String[]
    * @author wondtech liangming
    * @date 2008-07-25
    */
  def getPropertyArray(name: String): Array[String] = {
    // get the value first, then convert
    val value: String = SystemConfig.getProperty(name)
    if (value == null) return null
    try {
      val propertyArray: Array[String] = value.split(",") // 将字符用逗开分离
      propertyArray
    } catch {
      case _: NumberFormatException => null
    }
  }

  /**
    * Retrieve all property keys
    *
    * @return Enumeration A list of all keys
    */
  def keys() {
    mConfig.keys()
  }


  def getPropertyMap(name: String): util.Map[String, String] = {
    val maps: Array[String] = getPropertyArray(name)
    val map: util.Map[String, String] = new util.TreeMap()
    try {
      for (str <- maps) {
        val array: Array[String] = str.split(":")
        if (array.length > 1) {
          map.put(array(0), array(1))
        }
      }
    } catch {
      case _: Exception =>
        log.error("获取PropertyMap信息错误! key is :" + name)
        println(_)
    }
    map
  }

  def main(args: Array[String]): Unit = {

  }

}

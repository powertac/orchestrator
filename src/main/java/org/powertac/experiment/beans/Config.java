package org.powertac.experiment.beans;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.powertac.experiment.constants.Constants;
import org.powertac.experiment.services.HibernateUtil;
import org.powertac.experiment.services.Utils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "config")
public class Config
{
  private static Logger log = Utils.getLogger();

  private int configId;
  private String configKey;
  private String configValue;

  public static String getIndexContent ()
  {
    String content;
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      org.hibernate.Query query = session.createQuery(Constants.HQL.GET_CONFIG);
      query.setString("configKey", "index_content");
      Config config = (Config) query.uniqueResult();

      if (config == null) {
        config = new Config();
        config.setConfigKey("index_content");
        config.setConfigValue("");
        session.save(config);
      }

      content = config.getConfigValue();
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error, getting index content");
      return null;
    }
    finally {
      session.close();
    }

    return content;
  }

  public static boolean setIndexContent (String newContent)
  {
    Session session = HibernateUtil.getSession();
    Transaction transaction = session.beginTransaction();
    try {
      org.hibernate.Query query = session.createQuery(Constants.HQL.GET_CONFIG);
      query.setString("configKey", "index_content");
      Config config = (Config) query.uniqueResult();
      config.setConfigValue(newContent);
      session.update(config);
      transaction.commit();
    }
    catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
      log.error("Error, setting index content");
      return false;
    }
    finally {
      session.close();
    }

    return true;
  }

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "configId", unique = true, nullable = false)
  public int getConfigId ()
  {
    return configId;
  }

  public void setConfigId (int configId)
  {
    this.configId = configId;
  }

  @Column(name = "configKey", nullable = false)
  public String getConfigKey ()
  {
    return configKey;
  }

  public void setConfigKey (String configKey)
  {
    this.configKey = configKey;
  }

  @Column(name = "configValue", nullable = false)
  @Type(type = "text")
  public String getConfigValue ()
  {
    return configValue;
  }

  public void setConfigValue (String configValue)
  {
    this.configValue = configValue;
  }
}
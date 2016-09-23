package uk.ac.ebi.pride.toolsuite.pgconverter.utils;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * This class provides general utitilies for the PGConverter tool, such as:
 * arguments, supported file types, REDIS messaging, and handling exiting the application.
 *
 * @author Tobias Ternent
 */
public class Utility {
  private static final Logger log = LoggerFactory.getLogger(Utility.class);

  public static final String ARG_VALIDATION = "v";
  public static final String ARG_CONVERSION = "c";
  public static final String ARG_MESSAGE = "m";
  public static final String ARG_MZID = "mzid";
  public static final String ARG_PEAK = "peak";
  public static final String ARG_PEAKS = "peaks";
  public static final String ARG_PRIDEXML = "pridexml";
  public static final String ARG_MZTAB = "mztab";
  public static final String ARG_PROBED = "probed";
  public static final String ARG_BIGBED = "bigbed";
  public static final String ARG_OUTPUTFILE = "outputfile";
  public static final String ARG_INPUTFILE = "inputfile";
  public static final String ARG_OUTPUTTFORMAT = "outputformat";
  public static final String ARG_CHROMSIZES = "chromsizes";
  public static final String ARG_ASQLFILE = "asqlfile";
  public static final String ARG_ASQLNAME = "asqlname";
  public static final String ARG_BIGBEDCONVERTER = "bigbedconverter";
  public static final String ARG_REPORTFILE = "reportfile";
  public static final String ARG_REDIS = "redis";
  public static final String ARG_REDIS_SERVER = "redisserver";
  public static final String ARG_REDIS_PORT = "redisport";
  public static final String ARG_REDIS_PASSWORD = "redispassword";
  public static final String ARG_REDIS_CHANNEL = "redischannel";
  public static final String ARG_REDIS_MESSAGE = "redismessage";
  public static final String ARG_SKIP_SERIALIZATION = "skipserialization";
  public static final String MS_INSTRUMENT_MODEL_AC = "MS:1000031";
  public static final String MS_SOFTWARE_AC = "MS:1000531";
  public static final String MS_CONTACT_EMAIL_AC = "MS:1000589";
  public static final String MS_SOFTWARE_NAME = "software";
  public static final String MS_INSTRUMENT_MODEL_NAME = "instrument model";

  /**
   * The supported file types.
   */
  public enum FileType {MZID("mzid"), MZTAB("mztab"), PRIDEXML("xml"), ASQL("as"), PROBED("pro.bed"), BIGBED("bb"), UNKNOWN("");
    private String format;

    FileType(String format) {
        this.format = format;
    }

    public String toString() {
        return format;
    }
  }

  /**
   * Handles exiting from the tool, and potentially messages REDIS if set.
   * @param cmd
   */
  public static void exit(CommandLine cmd) {
    if (cmd.hasOption(ARG_REDIS)) {
      notifyRedisChannel(cmd.getOptionValue(ARG_REDIS_SERVER), Integer.parseInt(cmd.getOptionValue(ARG_REDIS_PORT)),
                         cmd.getOptionValue(ARG_REDIS_PASSWORD), cmd.getOptionValue(ARG_REDIS_CHANNEL), cmd.getOptionValue(ARG_REDIS_MESSAGE));
      }
    log.info("Exiting application.");
     System.exit(0);
  }

  /**
   * Publishes a success or failure message to the specified REDIS channel according to the credentials used.
   * @param jedisServer the REDIS server name.
   * @param jedisPort the REDIS server port.
   * @param jedisPassword the REDIS password
   * @param assayChannel the REDIS channel to post a message to.
   * @param message the message content.
   */
  public static void notifyRedisChannel(String jedisServer, int jedisPort, String jedisPassword, String assayChannel, String message) {
    try {
      log.info("Connecting to REDIS channel:" + assayChannel);
      JedisPool pool = new JedisPool(new JedisPoolConfig(), jedisServer, jedisPort, 0, jedisPassword);
      Jedis jedis = pool.getResource();
      log.info("Publishing message to REDIS: " + message);
      jedis.publish(assayChannel, message);
      log.info("Published message to REDIS, closing connection");
      jedis.quit();
    } catch (Exception e) {
      log.error("Exception while publishing message to REDIS channel.", e);
    }
  }
}

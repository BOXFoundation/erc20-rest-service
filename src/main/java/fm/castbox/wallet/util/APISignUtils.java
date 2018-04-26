package fm.castbox.wallet.util;

import java.beans.PropertyDescriptor;
import java.io.EOFException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import fm.castbox.wallet.exception.InvalidParamException;
import fm.castbox.wallet.properties.APIProperties;
import fm.castbox.wallet.util.algorithm.ECDSAAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class APISignUtils {

  private static APIProperties apiProperties;

  /**
   *
   * Transform the readable fields (excludes certain) of an object to a sorted QueryString format String, which will be
   * used later on the signature validation of some api requests.
   *
   * @param       object,  the object whose readable fields will be transformed to a string of QueryString format.
   *
   * @param       excludeFields, the fields need to be exclude from the transition, if more than one field provided,
   *                            them could be connected with a comma separator, like 'field1,field2'
   *
   * @return      strReadyToSign, a sorted QueryString format result.
   *
   * @exception   IllegalAccessException, Exception
   *
   *
   * Examples:
   *
   *      @DATA class A { int b1; int c2, int a3; int e4; };  foo = new A(5,6,7,8,9);
   *
   *      prepareStrToSign(foo, null)  returns 'a3=7&b1=5&c2=6&e4=9'
   *      prepareStrToSign(foo, 'e4')  returns 'a3=7&b1=5&c2=6'
   *
   */
  public static String prepareStrToSign(Object object, String excludeFields) throws Exception {
    Map<String, Object> fieldsMap = objectToSortedMap(object, excludeFields);
    StringBuffer result = new StringBuffer();
    Iterator<String> iter = fieldsMap.keySet().iterator();
    while (iter.hasNext()) {
      String fieldName = iter.next();
      Object fieldValue = fieldsMap.get(fieldName);
      if (null == fieldValue) {
        continue;
      }
      if (fieldValue instanceof String && ((String)fieldValue).isEmpty() ) {
        continue;
      }
      if (!isBaseDataType(fieldValue.getClass())) {
        if (fieldValue instanceof JSONArray) {
          fieldValue = fieldValue.toString();
        } else {
          fieldValue = JSON.toJSONString(objectToSortedMap(fieldValue, null));
        }
      }
      result.append(fieldName).append("=").append(fieldValue.toString()).append("&");
    }

    String strReadyToSign = result.toString().substring(0, result.length() > 0 ? result.length() - 1 : 0); // trim off last '&'
    return strReadyToSign;
  }

  public static boolean verifySign(String unsignedString, String sign){
    try {
      String keyFromConfig = Optional.ofNullable(apiProperties).orElse(new APIProperties()).getAllowedPubkey();
      return verifySign(keyFromConfig, unsignedString, sign);
    } catch (Exception e) {
      log.info("Verify Sign Fail: ", e);
      return false;
    }
  }

  public static boolean verifySign(String pubKey, String unsignedString, String sign) throws Exception {
    if (StringUtils.isEmpty(pubKey) || StringUtils.isEmpty(unsignedString) || StringUtils.isEmpty(sign)) {
      throw new RuntimeException("Param" + (StringUtils.isEmpty(pubKey) ? " 'pubKey'" : "")
                                          + (StringUtils.isEmpty(unsignedString) ? " 'unsignedString'" : "")
                                          + (StringUtils.isEmpty(sign) ? " 'sign'" : "")
                                          + " could not be empty");
    }
    try {
      return ECDSAAlgorithm.verify(unsignedString, sign, pubKey);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof EOFException) {
        return false; // caused by invalid sign format
      }
      throw e;
    }
  }

  public static void verifySignedObject(Object objectWithSignField) throws Exception {
    String toSignStr = APISignUtils.prepareStrToSign(objectWithSignField, "sign");
    String objectSign = (String) PropertyUtils.getProperty(objectWithSignField, "sign");
    boolean isSignValid = APISignUtils.verifySign(toSignStr, objectSign);
    if (!isSignValid) {
      log.info("toSignStr is " + toSignStr);
      // TODO: uncomment below after debugging
//      throw new InvalidParamException("Object Sign", "srcStr is " + toSignStr);
    }
  }

  public static String signForString(String priKey, String unsignedString) throws Exception {
    return ECDSAAlgorithm.sign(priKey, unsignedString.getBytes());
  }

  @Autowired
  public void setApiProperties(APIProperties apiProperties) {
    APISignUtils.apiProperties = apiProperties;
  }

  private static SortedMap<String, Object> objectToSortedMap(Object object, String excludeFields) throws Exception {
    excludeFields = Optional.ofNullable(excludeFields).orElse("");
    List<String> exFields = new ArrayList<String>();
    exFields.addAll(Arrays.asList(excludeFields.split(",")));
    exFields.add("class");

    SortedMap<String, Object> fieldsMap = new TreeMap<String, Object>();
    PropertyDescriptor[] discriptors = PropertyUtils.getPropertyDescriptors(object);
    for(int i = 0; i < discriptors.length; i++) {
      String fieldName = discriptors[i].getName();
      boolean flag = discriptors[i].getReadMethod() != null && exFields.indexOf(fieldName) < 0;
      if (flag) {
        // Note: problems occur if object's class is a 'nested static class', so try to avoid that
        Object value = PropertyUtils.getProperty(object, fieldName);
        if (null != value) {
          fieldsMap.put(fieldName, value);
        }
      }
    }

    return fieldsMap;
  }

  private static boolean isBaseDataType(Class clazz)  throws Exception {
    return (clazz.equals(String.class) || clazz.equals(Integer.class)||
            clazz.equals(Byte.class) || clazz.equals(Long.class) ||
            clazz.equals(Double.class) || clazz.equals(Float.class) ||
            clazz.equals(Character.class) || clazz.equals(Short.class) ||
            clazz.equals(BigDecimal.class) || clazz.equals(BigInteger.class) ||
            clazz.equals(Boolean.class) || clazz.equals(Date.class) || clazz.isPrimitive());
  }
}

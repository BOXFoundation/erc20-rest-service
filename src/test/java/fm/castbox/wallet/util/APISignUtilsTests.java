package fm.castbox.wallet.util;

import lombok.Data;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class APISignUtilsTests {

    private static final String PUBKEY = "BAK2uyd5XgVlj3JB0jA00dyPm4j7EOj4SLtN1LN9RCWc4uuemwjr22M2OQUOMqP4dSSL4h9iOu0jtmQDlk00bkI=";

    @TestConfiguration
    @Data
    public class RequestDTO {
      private String strParam1;
      private int intParam2;
      private float floatParam3;
      private boolean boolParam4;
      private double doubleParma5;
      private EmbededRDTO embedParam;
      private String sign;

      @Data
      public class EmbededRDTO {
        private String esParam1;
        private int esParam2;
      }
    }

    @Test
    public void testPrepareStrToSign() throws Exception {
      RequestDTO rDto = buildTestRDTO();
      assertNull(rDto.getSign());

      String unsign1 = APISignUtils.prepareStrToSign(rDto, null);
      assertEquals(unsign1, "boolParam4=false&doubleParma5=2.333&embedParam={\"esParam1\":\"esp1\",\"esParam2\":3}" +
                                 "&floatParam3=2.33&intParam2=100&strParam1=sp1");

      String unsign2 = APISignUtils.prepareStrToSign(rDto, "intParam2");
      assertEquals(unsign2, "boolParam4=false&doubleParma5=2.333&embedParam={\"esParam1\":\"esp1\",\"esParam2\":3}" +
                                  "&floatParam3=2.33&strParam1=sp1");

      String unsign3 = APISignUtils.prepareStrToSign(rDto, "intParam2,embedParam");
      assertEquals(unsign3, "boolParam4=false&doubleParma5=2.333&floatParam3=2.33&strParam1=sp1");
    }

    @Test
    public void testVerifySign() throws Exception {
      RequestDTO rDto = buildTestRDTO();
      assertNull(rDto.getSign());

      String unsign1 = "boolParam4=false&doubleParma5=2.333&embedParam={\"esParam1\":\"esp1\",\"esParam2\":3}" +
              "&floatParam3=2.33&intParam2=100&strParam1=sp1";

      assertTrue(APISignUtils.verifySign(PUBKEY, unsign1,
              "MEQCIGOKzvEAXZl9VN9TgXTAnOaNYTt7CpElrtV5pHTQgjQKAiB0gKJmbKVfj5L3AcmGgbXQ/tlhFhPPpY63/S+rXEaHSQ=="));

//      assertTrue(APISignUtils.verifySign(unsign1,
//              "MEQCIGOKzvEAXZl9VN9TgXTAnOaNYTt7CpElrtV5pHTQgjQKAiB0gKJmbKVfj5L3AcmGgbXQ/tlhFhPPpY63/S+rXEaHSQ=="));
    }

    private RequestDTO buildTestRDTO() {
      RequestDTO rDto = new RequestDTO();
      rDto.strParam1 = "sp1";
      rDto.intParam2 = 100;
      rDto.floatParam3 = 2.33F;
      rDto.boolParam4 = false;
      rDto.doubleParma5 = 2.333;
      RequestDTO.EmbededRDTO embeded = rDto.new EmbededRDTO();
      embeded.esParam1 = "esp1";
      embeded.esParam2 = 3;
      rDto.embedParam = embeded;
      return  rDto;
    }

}

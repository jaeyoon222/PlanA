package com.study.StudyCafe.service;

import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class SmsService {
    private final String apiKey = "NCSDI6CB6HSZ7ISM";
    private final String apiSecret = "RMIPJZVVWODFCOVWYIKRTFB514BYG6OS";
    private final String senderPhone = "01084602161"; // 등록된 발신번호

    public void sendSMS(String to, String message) {
        //메세지 클래스 객체를 생성, 인증을 위한 키와 시크릿 넘기기
        Message coolsms = new Message(apiKey, apiSecret);

        HashMap<String, String> params = new HashMap<>();
        params.put("to", to);
        params.put("from", senderPhone);
        params.put("type", "SMS");
        params.put("text", message);
        params.put("app_version", "gptonline v1.0");

        try {
            //설정 된 파라미터를 이용해 coolsms 서버에 문자 전송 요청을 보냅니다.
            //결과는 json 형식으로 반환
            JSONObject result = coolsms.send(params);
            System.out.println("문자 전송 결과: " + result.toJSONString());
        } catch (CoolsmsException e) {
            System.err.println("문자 전송 실패: " + e.getMessage());
            System.err.println("에러 코드: " + e.getCode());
        }
    }
}

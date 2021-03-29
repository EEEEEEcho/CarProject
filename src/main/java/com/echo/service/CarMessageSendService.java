package com.echo.service;

import com.alibaba.fastjson.JSON;
import com.echo.encrypt.gm.sm4.SM4;
import com.echo.pojo.Car;
import com.echo.pojo.EncryptMessage;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class CarMessageSendService {
    public void messageCommunication(String message, String url, Car car) {
        System.out.println("message:" + message);
        System.out.println("Car:" + car);
        byte[] sessionKey = decode(car.getSessionKey());
        String result = encryptMessageToString(message.getBytes(StandardCharsets.UTF_8), sessionKey);
        EncryptMessage encryptMessage = null;
        try {
            encryptMessage = doPost(car.getCarVin(), result, url);
            System.out.println("encryptMessage : " + encryptMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (encryptMessage != null) {
            String encryptMessageStr = encryptMessage.getMessage();
            byte[] bytes = decryptMessageToByte(encryptMessageStr, sessionKey);
            String finalResult = new String(bytes);
            System.out.println(finalResult);
        }
    }

    public EncryptMessage doPost(String carVin, String message, String url) throws IOException {
        System.out.println("url:" + url);
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("carVin", carVin));
        params.add(new BasicNameValuePair("data", message));
        post.setEntity(new UrlEncodedFormEntity(params));
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println("result:" + result);
            return JSON.parseObject(result, EncryptMessage.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encode(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public byte[] decode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }

    public byte[] encryptMessageToByte(byte[] bytes, byte[] key) {
        try {
            return SM4.ecbCrypt(true, key, bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] encryptMessageToByte(String str, byte[] key) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(str);
            return SM4.ecbCrypt(true, key, bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decryptMessageToByte(byte[] bytes, byte[] key) {
        try {
            return SM4.ecbCrypt(false, key, bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decryptMessageToByte(String str, byte[] key) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(str);
            return SM4.ecbCrypt(false, key, bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encryptMessageToString(String str, byte[] key) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(str);
            byte[] ecbCrypt = SM4.ecbCrypt(true, key, bytes, 0, bytes.length);
            return Base64.getUrlEncoder().encodeToString(ecbCrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encryptMessageToString(byte[] bytes, byte[] key) {
        try {
            byte[] ecbCrypt = SM4.ecbCrypt(true, key, bytes, 0, bytes.length);
            return Base64.getUrlEncoder().encodeToString(ecbCrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decryptMessageToString(byte[] bytes, byte[] key) {
        try {
            byte[] ecbCrypt = SM4.ecbCrypt(false, key, bytes, 0, bytes.length);
            return Base64.getUrlEncoder().encodeToString(ecbCrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String decryptMessageToString(String str, byte[] key) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(str);
            byte[] ecbCrypt = SM4.ecbCrypt(false, key, bytes, 0, bytes.length);
            return Base64.getUrlEncoder().encodeToString(ecbCrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

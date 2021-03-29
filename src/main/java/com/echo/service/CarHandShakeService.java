package com.echo.service;

import com.alibaba.fastjson.JSON;
import com.echo.encrypt.gm.sm4.SM4;
import com.echo.encrypt.gm.sm9.*;

import com.echo.pojo.Car;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

public class CarHandShakeService {
    private final SM9Curve sm9Curve = new SM9Curve();
    private final SM9 sm9 = new SM9(sm9Curve);
    private final byte[] birthKey = new byte[16];


    public Car doGetRequest(String message, String url) {
        //        创建httpclient客户端
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        //创建get请求
        url += message;
        System.out.println("url:" + url);
        HttpGet httpGet = new HttpGet(url);
        //响应模型
        CloseableHttpResponse response = null;
        try {
            //发送get请求
            response = httpClient.execute(httpGet);
            //响应模型中获取响应实体
            HttpEntity entity = response.getEntity();
            System.out.println("响应状态:" + response.getStatusLine());
            if (entity != null) {
                System.out.println("响应内容长度:" + entity.getContentLength());
                String responseText = EntityUtils.toString(entity);
                System.out.println("响应内容:" + responseText);
                return JSON.parseObject(responseText, Car.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Car finishHandShake(Car car, String url) {
        //1.先解密自己的密钥信息
        String carPrivateEncKey = decryptMessageToString(car.getPrivateEncKey(), birthKey);
        String carPrivateSignKey = decryptMessageToString(car.getPrivateSignKey(), birthKey);
        String carPrivateExecKey = decryptMessageToString(car.getPrivateExecKey(), birthKey);
        //2.todo 存储这些key
        car.setPrivateEncKey(carPrivateEncKey);
        car.setPrivateSignKey(carPrivateSignKey);
        car.setPrivateExecKey(carPrivateExecKey);
        //3.还原ServerTmpKey和MasterPublicKey
        byte[] masterPublicKeyBytes = decode(car.getHandShakeInfo().getEncryptMasterPublicKey());
        try {
            masterPublicKeyBytes = SM4.ecbCrypt(false, birthKey, masterPublicKeyBytes, 0, masterPublicKeyBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] serverTempKeyBytes = decode(car.getHandShakeInfo().getServerTempKey());
        try {
            serverTempKeyBytes = SM4.ecbCrypt(false, birthKey, serverTempKeyBytes, 0, serverTempKeyBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        MasterPublicKey masterPublicKey = MasterPublicKey.fromByteArray(sm9Curve, masterPublicKeyBytes);
        G1KeyPair serverTempKey = G1KeyPair.fromByteArray(sm9Curve, serverTempKeyBytes);
        //4.生成clientTempKey
        System.out.println(Arrays.toString(masterPublicKeyBytes));
        System.out.println(car.getHandShakeInfo().getServerVin());
        G1KeyPair clientTempKey = sm9.keyExchangeInit(masterPublicKey, car.getHandShakeInfo().getServerVin());
        //5.生成clientAgreementKey
        PrivateKey privateExecKey = PrivateKey.fromByteArray(sm9Curve, decode(car.getPrivateExecKey()));
        ResultKeyExchange clientAgreementKey = null;
        try {
            clientAgreementKey = sm9.keyExchange(masterPublicKey, false, car.getCarVin(), car.getHandShakeInfo().getServerVin(), privateExecKey, clientTempKey, serverTempKey.getPublicKey(), 16);
            car.getHandShakeInfo().setSB(encode(clientAgreementKey.getSB1()));
            car.getHandShakeInfo().setSA(encode(clientAgreementKey.getSA2()));
            car.setSessionKey(encode(clientAgreementKey.getSK()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //6.封装信息
        String clientTempKeyStr = encryptMessageToString(clientTempKey.toByteArray(), birthKey);
        car.getHandShakeInfo().setClientTempKey(clientTempKeyStr);
        String sb1Str = null;
        if (clientAgreementKey != null) {
            sb1Str = encryptMessageToString(clientAgreementKey.getSB1(), birthKey);
            car.getHandShakeInfo().setSB(sb1Str);
        }
        //7.发送
        String params = car.getCarVin() + "/" + clientTempKeyStr + "/" + sb1Str;
        Car sa = doGetRequest(params, url);
        System.out.println("sa:" + sa);
        System.out.println("birthKey:" + Arrays.toString(birthKey));
        //8.完成握手
        byte[] serverSA = decryptMessageToByte(sa.getHandShakeInfo().getSA(), birthKey);
        System.out.println("serverSA" + Arrays.toString(serverSA));
        byte[] clientSA = decode(car.getHandShakeInfo().getSA());
        System.out.println("clientSA" + Arrays.toString(clientSA));
        if (SM9Utils.byteEqual(serverSA, clientSA)) {
            return car;
        }
        return null;
        //return doGetRequest(params,url);
    }

//    public Car finishHandShake(Car car){
//        byte[] SA = decryptMessageToByte(car.getHandShakeInfo().getSA(), birthKey);
//        if()
//    }


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
//    private MasterPublicKey masterPublicKey;
//    private PrivateKey privateKey;
//    private G1KeyPair clientTempKey;
//    private G1KeyPair serverTempKey;
//    private ResultKeyExchange clientAgreementKey;
//    private Car car;
//    public Car handShake(String carVin,EncryptMessage encryptMessage){
//        car = new Car();
//        car.setVinCode(carVin);
//        car.setBirthKey(new byte[16]);
////        EncryptMessage encryptMessage = carClient.handShake(carVin);
//        byte[] data = encryptMessage.getData();
//        if(data == null || data.length == 0){
//            car.setBytes(new byte[16]);
//            return car;
//        }
//        byte[] decryptBytes = null;
//        try {
//            decryptBytes = SM4.ecbCrypt(false, car.getBirthKey(), data, 0, data.length);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if(decryptBytes != null){
//            byte[] encPrivateKey = new byte[129];
//            System.arraycopy(decryptBytes,0,encPrivateKey,0,129);
//            //System.out.println(Arrays.toString(encPrivateKey));
//            byte[] signPrivateKey = new byte[65];
//            System.arraycopy(decryptBytes,129,signPrivateKey,0,65);
//            //System.out.println(Arrays.toString(signPrivateKey));
//            byte[] execPrivateKey = new byte[129];
//            System.arraycopy(decryptBytes,194,execPrivateKey,0,129);
//            //System.out.println(Arrays.toString(execPrivateKey));
//            byte[] serverTempKey = new byte[96];
//            System.arraycopy(decryptBytes,323,serverTempKey,0,96);
//            //System.out.println(Arrays.toString(serverTempKey));
//            byte[] encMasterPublicKey = new byte[65];
//            System.arraycopy(decryptBytes,419,encMasterPublicKey,0,65);
//            //System.out.println(Arrays.toString(encMasterPublicKey));
//            car.setPrivateEncKey(encPrivateKey);
//            car.setPrivateSignKey(signPrivateKey);
//            car.setPrivateExecKey(execPrivateKey);
//            car.setServerTempKey(serverTempKey);
//            car.setPublicMasterKey(encMasterPublicKey);
//        }
//        return car;
//    }
//
//    public byte[] handShakePartTwo(){
//        byte[] clientTempKey = genClientTempKey().toByteArray();
//        byte[] sb1 = getSB1();
//        System.out.println("ClientTempKey:" + Arrays.toString(clientTempKey));
//        System.out.println("SB1:" + Arrays.toString(sb1));
//        System.out.println(sm9.getCurve().toString());
//        byte[] result = new byte[clientTempKey.length + sb1.length];
//        System.arraycopy(clientTempKey,0,result,0,clientTempKey.length);
//        System.out.println(Arrays.toString(clientTempKey) + " : " + clientTempKey.length);
//        System.arraycopy(sb1,0,result,clientTempKey.length,sb1.length);
//        System.out.println(Arrays.toString(sb1) + " : " + sb1.length);
//        System.out.println(Arrays.toString(result));
//        try {
//            result = SM4.ecbCrypt(true,car.getBirthKey(),result,0,result.length);
//            return result;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public G1KeyPair genClientTempKey(){
//        masterPublicKey = MasterPublicKey.fromByteArray(sm9Curve, car.getPublicMasterKey());
//        privateKey = PrivateKey.fromByteArray(sm9Curve, car.getPrivateEncKey());
//        sm9 = new SM9(sm9Curve);
//        clientTempKey = sm9.keyExchangeInit(masterPublicKey, "ADERXWRY78VC78W");
//        serverTempKey = G1KeyPair.fromByteArray(sm9Curve, car.getServerTempKey());
//        return clientTempKey;
//    }
//
//    public byte[] getSB1(){
//        int keyByteLength = 16;
//        try {
//            clientAgreementKey = sm9.keyExchange(masterPublicKey,false,car.getVinCode(),"ADERXWRY78VC78W",PrivateKey.fromByteArray(sm9Curve,car.getPrivateExecKey()),clientTempKey,serverTempKey.getPublicKey(),keyByteLength);
//
//            return clientAgreementKey.getSB1();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return new byte[0];
//    }
//
//    public MasterPublicKey getMasterPublicKey() {
//        return masterPublicKey;
//    }
//
//    public void setMasterPublicKey(MasterPublicKey masterPublicKey) {
//        this.masterPublicKey = masterPublicKey;
//    }
//
//    public SM9 getSm9() {
//        return sm9;
//    }
//
//    public void setSm9(SM9 sm9) {
//        this.sm9 = sm9;
//    }
//
//    public ResultKeyExchange getClientAgreementKey() {
//        return clientAgreementKey;
//    }
//
//    public void setClientAgreementKey(ResultKeyExchange clientAgreementKey) {
//        this.clientAgreementKey = clientAgreementKey;
//    }
//
//    public Car getCar() {
//        return car;
//    }
//
//    public void setCar(Car car) {
//        this.car = car;
//    }
//
//    public PrivateKey getPrivateKey() {
//        return privateKey;
//    }
//
//    public void setPrivateKey(PrivateKey privateKey) {
//        this.privateKey = privateKey;
//    }
//
//    public G1KeyPair getClientTempKey() {
//        return clientTempKey;
//    }
//
//    public void setClientTempKey(G1KeyPair clientTempKey) {
//        this.clientTempKey = clientTempKey;
//    }
//
//    public G1KeyPair getServerTempKey() {
//        return serverTempKey;
//    }
//
//    public void setServerTempKey(G1KeyPair serverTempKey) {
//        this.serverTempKey = serverTempKey;
//    }
}

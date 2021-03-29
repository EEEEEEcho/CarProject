package com.echo.mapper;

import com.echo.encrypt.gm.sm4.SM4;

import java.util.Arrays;
import java.util.Base64;

public class Test {
    public static void main(String[] args) throws Exception {
//        String s = "epmKwbMn23Drfcz1F3sQ-iwjb1WL4SnxqlS0ZnfYzpJhezJ5H9he78uFNAe6BW7FBEUq6lxqP8RQmM-Mrz-URlRDiEKy54Uo-mmrNtcaCrUomOcMsgS1O5vdNFT60_H8-I3Lrxo-rChO3oMdFvw9roDrBIJXRw67AZZ2NHhqEJM=";
//        byte[] decode = Base64.getUrlDecoder().decode(s);
//        System.out.println(Arrays.toString(decode));
//        decode = SM4.ecbCrypt(true,new byte[16],decode,0,decode.length);
//        System.out.println(Arrays.toString(decode));
//        decode = SM4.ecbCrypt(false,new byte[16],decode,0,decode.length);
//        System.out.println(Arrays.toString(decode));
        byte[] bytes = new byte[16];
        String s = Base64.getUrlEncoder().encodeToString(bytes);
        System.out.println(s);
    }
}

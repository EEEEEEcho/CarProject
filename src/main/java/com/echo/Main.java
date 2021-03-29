package com.echo;

import com.echo.controller.CarController;
import com.echo.pojo.Car;

public class Main {
    public static void main(String[] args) {
        CarController carController = new CarController();
        String carVin = "BNHED7EGFF8A7146";
        String handShakeStartUrl = "http://localhost:10010/api/consumer/car/handshake/start/";
        //String handShakeStartUrl = "http://localhost:8081/car/handshake/start/";
        //String handShakeStartUrl = "http://localhost:9091/car/handshake/start/";
        System.out.println(handShakeStartUrl);
        Car car = carController.startHandShake(carVin, handShakeStartUrl);
        System.out.println("第一次握手后的：" + car);
        String doHandShakeUrl = "http://localhost:10010/api/consumer/car/handshake/finish/";
        //String doHandShakeUrl = "http://localhost:8081/car/handshake/finish/";
        //String doHandShakeUrl = "http://localhost:9091/car/handshake/finish/";
        car = carController.finishHandShake(car, doHandShakeUrl);
        if (car != null) {
            System.out.println("可算完事了");
            System.out.println(car);
        }
        //String message = "TellMeAStory";
        String message = "That Is Your Life";
        //String communicateUrl = "http://localhost:8081/message/";
        //String communicateUrl = "http://localhost:9091/message/";
        String communicateUrl = "http://localhost:10010/api/consumer/message/";
        carController.messageCommunication(message, communicateUrl, car);
    }
}

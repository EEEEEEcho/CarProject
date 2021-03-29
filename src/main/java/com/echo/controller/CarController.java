package com.echo.controller;

import com.echo.pojo.Car;
import com.echo.service.CarHandShakeService;
import com.echo.service.CarMessageSendService;

public class CarController {
    private final CarHandShakeService carHandShakeService = new CarHandShakeService();
    private final CarMessageSendService carMessageSendService = new CarMessageSendService();

    public Car startHandShake(String carVin, String url) {
        return carHandShakeService.doGetRequest(carVin, url);
    }

    public Car finishHandShake(Car car, String url) {
        return carHandShakeService.finishHandShake(car, url);
    }

    public void messageCommunication(String message, String url, Car car) {
        carMessageSendService.messageCommunication(message, url, car);
    }
//    public Car finshHandShake(Car car){
//        return carHandShakeService.finishHandShake(car);
//    }
//    public EncryptMessage startHandShakePartTwo(String clientTempKeyAndSBStr,String url){
//        return doGetRequest(clientTempKeyAndSBStr,url);
//    }
}

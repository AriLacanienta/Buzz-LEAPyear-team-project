package com.buzzleapyear.trading_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.buzzleapyear.trading_api.service.TestService;

@RestController
@RequestMapping("api/v1") 
public class TestController {

  private final TestService testService;

  public TestController(TestService testService) {
    this.testService = testService;
  }

  @GetMapping("/test")
  public String index() {
    return testService.testPrint();
  }

  @GetMapping("/getClientTradeHistory/{client_id}")
  public String getClientTradeHistory(){
    return "not yet implemented";
  }

}
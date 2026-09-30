package com.buzzleapyear.trading_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buzzleapyear.trading_api.service.TestService;

@RestController
@RequestMapping("/api/v1/test") 
public class TestController {

  private final TestService testService;

  public TestController(TestService testService) {
    this.testService = testService;
  }

  @GetMapping
  public String index() {
    return testService.testPrint();
  }

}
package com.controller;

import com.domain.Demo;
import com.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class DemoController {

    private static final Logger LOG = LoggerFactory.getLogger(DemoController.class);


    @Resource
    private DemoService demoService;



    /**
     * GET, POST, PUT, DELETE
     *
     * /user?id=1
     * /user/1
     * @return
     */

    @GetMapping("/demo/list")
    public List<Demo> list() {
        return demoService.list();
    }


}



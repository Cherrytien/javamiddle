package com.controller;

import com.controller.BaseController;
import com.enums.OrderStatusEnum;
import com.enums.PayMethod;
import com.domain.OrderStatus;
import com.domain.bo.ShopcartBO;
import com.domain.bo.SubmitOrderBO;
import com.domain.vo.MerchantOrdersVO;
import com.domain.vo.OrderVO;
import com.service.OrderService;
import com.util.CookieUtils;
import com.util.IMOOCJSONResult;
import com.util.JsonUtils;
import com.util.RedisOperator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(value = "订单相关", tags = {"订单相关的api接口"})
@RequestMapping("orders")
@RestController
public class OrdersController extends BaseController {

    final static Logger logger = LoggerFactory.getLogger(OrdersController.class);

    @Autowired
    private OrderService orderService;

    @Resource
    private RestTemplate restTemplate;

    @Autowired
    private RedisOperator redisOperator;


    // 自己添加
    @Configuration
    public class Config {
        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder){
            return builder.build();
        }
    }


    @ApiOperation(value = "用户下单", notes = "用户下单", httpMethod = "POST")
    @PostMapping("/create")
    public IMOOCJSONResult create(
            @RequestBody SubmitOrderBO submitOrderBO,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (submitOrderBO.getPayMethod() != PayMethod.WEIXIN.type
                && submitOrderBO.getPayMethod() != PayMethod.ALIPAY.type ) {
            return IMOOCJSONResult.errorMsg("支付方式不支持！");
        }

//        System.out.println(submitOrderBO.toString());

        String shopcartJson = redisOperator.get(FOODIE_SHOPCART + ":" + submitOrderBO.getUserId());
        if (StringUtils.isBlank(shopcartJson)) {
//            return IMOOCJSONResult.errorMsg("购物数据不正确");
            System.out.println("购物数据不正确");
            ShopcartBO shopcartBO = new ShopcartBO();
            String itemSpecIds = submitOrderBO.getItemSpecIds();
            String itemSpecIdArr[] = itemSpecIds.split(",");

            for (String itemSpecId : itemSpecIdArr) {
                shopcartBO.setSpecId(itemSpecId);
                shopcartBO.setBuyCounts(1);
                shopcartJson = shopcartJson + JsonUtils.objectToJson(shopcartBO);
            }


            shopcartJson = "["+JsonUtils.objectToJson(shopcartBO)+"]";
//            System.out.println(shopcartJson);
        }

        List<ShopcartBO> shopcartList = JsonUtils.jsonToList(shopcartJson, ShopcartBO.class);

        // 1. 创建订单
        OrderVO orderVO = orderService.createOrder(shopcartList, submitOrderBO);
        String orderId = orderVO.getOrderId();

        // 2. 创建订单以后，移除购物车中已结算（已提交）的商品
        /**
         * 1001
         * 2002 -> 用户购买
         * 3003 -> 用户购买
         * 4004
         */
        // 清理覆盖现有的redis汇总的购物数据
        shopcartList.removeAll(orderVO.getToBeRemovedShopcatdList());
        redisOperator.set(FOODIE_SHOPCART + ":" + submitOrderBO.getUserId(), JsonUtils.objectToJson(shopcartList));
        // 整合redis之后，完善购物车中的已结算商品清除，并且同步到前端的cookie
        CookieUtils.setCookie(request, response, FOODIE_SHOPCART, JsonUtils.objectToJson(shopcartList), true);

        // 3. 向支付中心发送当前订单，用于保存支付中心的订单数据
//        MerchantOrdersVO merchantOrdersVO = orderVO.getMerchantOrdersVO();
//        merchantOrdersVO.setReturnUrl(payReturnUrl);

        // 为了方便测试购买，所以所有的支付金额都统一改为1分钱
//        merchantOrdersVO.setAmount(1);

//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.add("imoocUserId","imooc");
//        headers.add("password","imooc");

//        HttpEntity<MerchantOrdersVO> entity =
//                new HttpEntity<>(merchantOrdersVO, headers);

//        ResponseEntity<IMOOCJSONResult> responseEntity =
//                restTemplate.postForEntity(paymentUrl,
//                        entity,
//                        IMOOCJSONResult.class);
//        IMOOCJSONResult paymentResult = responseEntity.getBody();
//        if (paymentResult.getStatus() != 200) {
//            logger.error("发送错误：{}", paymentResult.getMsg());
//            return IMOOCJSONResult.errorMsg("支付中心订单创建失败，请联系管理员！");
//        }

        return IMOOCJSONResult.ok(orderId);
    }

    @PostMapping("notifyMerchantOrderPaid")
    public Integer notifyMerchantOrderPaid(String merchantOrderId) {
        orderService.updateOrderStatus(merchantOrderId, OrderStatusEnum.WAIT_DELIVER.type);
        return HttpStatus.OK.value();
    }

    @PostMapping("getPaidOrderInfo")
    public IMOOCJSONResult getPaidOrderInfo(String orderId) {

        OrderStatus orderStatus = orderService.queryOrderStatusInfo(orderId);
        return IMOOCJSONResult.ok(orderStatus);
    }
}

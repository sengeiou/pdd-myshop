package com.ht.weichat.service.impl;

import com.ht.weichat.service.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.http.PopAccessTokenClient;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.http.api.request.PddOrderListGetRequest;
import com.pdd.pop.sdk.http.api.response.PddOrderListGetResponse;
import com.pdd.pop.sdk.http.token.AccessTokenResponse;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class SalesServiceImpl implements SalesService {
    public static final String clientId = "864a62d925204fa29d5a2d2b22d8c67e";
    public static final String clientSecret = "f821108d75c0dc7951e94cdd27103a28d19c7839";
    public static final String redirectUri = "https://mms.pinduoduo.com";
    public static final int pageSize = 100;
    public static final String daytimeStart = "00:00:00";
    public static final String daytimeEnd = "23:59:59";
//    public static String accessToken = "14bc0edea9da4da2bc47ebb7f0aeb6c8f9052bb0";
    public static String accessToken="4a24278bbfd348e29b95d1be617a8060e6ef3f12";

    @Override
    public  String getCodeUrl() {
        String url = "http://mms.pinduoduo.com/open.html";
        //client_id
        url += "?client_id=" + clientId;
        //授权类型为CODE
        url += "&response_type=code";
        //授权回调地址
        url += "&redirect_uri=" + redirectUri;
        return url;
    }

    @Override
    public String getAccessTokenFromCode(String code) {

        try {
            PopAccessTokenClient client = new PopAccessTokenClient(
                    clientId,
                    clientSecret);
            AccessTokenResponse accessTokenResponse = client.generate(code);
            accessToken = accessTokenResponse.getAccessToken();
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String unsend() {
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        String date=new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-")
                .append(calendar.get(Calendar.MONTH)+1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        SaleResult saleResult = new SaleResult(date, "未发货销量数据");
        PopClient client = new PopHttpClient(clientId, clientSecret);
        long startTime=today.getTime() / 1000L-86400;
        long endTime=today.getTime() / 1000L;
        PddOrderListGetResponse firstResponse = getOrderListGetResponse(client, 1,startTime,endTime,1, accessToken);
        if (firstResponse == null) {
            return "获取数据失败";
        }
        if (firstResponse.getOrderListGetResponse() == null){
            if(firstResponse.getErrorResponse()!=null ){
                return firstResponse.getErrorResponse().getErrorMsg();
            }
            return "获取订单失败";
        }

        List<PddOrderListGetResponse.OrderListGetResponseOrderListItem> firstOrderList = firstResponse.getOrderListGetResponse().getOrderList();
        int totalCount = firstResponse.getOrderListGetResponse().getTotalCount();
        // System.out.println("正在统计第一页订单:" + firstOrderList.size() + "条...");
        saleResult.total_order_count=totalCount;
        calOrderList(firstOrderList,saleResult);
        if (totalCount > firstOrderList.size()) {
            int page = totalCount % pageSize > 0 ? totalCount / pageSize + 1 : totalCount / pageSize;
            //System.out.println("剩余数据:" + (page - 1) + "页...");
            for (int i = 2; i <= page; i++) {
                PddOrderListGetResponse response = getOrderListGetResponse(client, date,i, accessToken);
                List<PddOrderListGetResponse.OrderListGetResponseOrderListItem> orderList = response.getOrderListGetResponse().getOrderList();
                //System.out.println("正在统计第" + i + "页订单:" + orderList.size() + "条...");
                calOrderList(orderList,saleResult);
            }
        }

        Collections.sort(saleResult.daySale, new GoodComparator());
        for (GoodItem goodItem : saleResult.daySale) {
            Collections.sort(goodItem.sku_list, new SkuComparator());
        }
        // System.out.println("统计完成，输出结果：");
        //System.out.println(JsonUtil.transferToJson(saleResult));
        return JsonUtil.transferToJson(saleResult);
    }

    @Override
    public String yesterday(){
//        String date = "2020-01-12";
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        String date=new StringBuilder().append(calendar.get(Calendar.YEAR)).append("-")
                .append(calendar.get(Calendar.MONTH)+1).append("-").append(calendar.get(Calendar.DAY_OF_MONTH)).toString();
        SaleResult saleResult = new SaleResult(date, "昨日销量数据");

        PopClient client = new PopHttpClient(clientId, clientSecret);

        PddOrderListGetResponse firstResponse = getOrderListGetResponse(client, date,1, accessToken);
        if (firstResponse == null) {
            return "获取数据失败";
        }

        if (firstResponse.getOrderListGetResponse() == null){
            if(firstResponse.getErrorResponse()!=null ){
                return firstResponse.getErrorResponse().getErrorMsg();
            }
        return "获取订单失败";
        }

        List<PddOrderListGetResponse.OrderListGetResponseOrderListItem> firstOrderList = firstResponse.getOrderListGetResponse().getOrderList();
        int totalCount = firstResponse.getOrderListGetResponse().getTotalCount();
        //System.out.println("当日订单：" + totalCount + "条");
        if (firstOrderList == null) {
            return "没有获取到订单数据";
        }
       // System.out.println("正在统计第一页订单:" + firstOrderList.size() + "条...");
        saleResult.total_order_count=totalCount;
        calOrderList(firstOrderList,saleResult);
        if (totalCount > firstOrderList.size()) {
            int page = totalCount % pageSize > 0 ? totalCount / pageSize + 1 : totalCount / pageSize;
            //System.out.println("剩余数据:" + (page - 1) + "页...");
            for (int i = 2; i <= page; i++) {
                PddOrderListGetResponse response = getOrderListGetResponse(client, date,i, accessToken);
                List<PddOrderListGetResponse.OrderListGetResponseOrderListItem> orderList = response.getOrderListGetResponse().getOrderList();
                //System.out.println("正在统计第" + i + "页订单:" + orderList.size() + "条...");
                calOrderList(orderList,saleResult);
            }
        }

        Collections.sort(saleResult.daySale, new GoodComparator());
        for (GoodItem goodItem : saleResult.daySale) {
            Collections.sort(goodItem.sku_list, new SkuComparator());
        }
       // System.out.println("统计完成，输出结果：");
        //System.out.println(JsonUtil.transferToJson(saleResult));
        return JsonUtil.transferToJson(saleResult);

    }

    private  PddOrderListGetResponse getOrderListGetResponse(PopClient client,String date, int page, String accessToken) {
       // System.out.println("正在获取第" + page + "页订单...");
        try {
            long startTime=dateToStamp(date + " " + daytimeStart) / 1000L;
            long endTime=dateToStamp(date + " " + daytimeEnd) / 1000L;
            return getOrderListGetResponse(client,5,startTime,endTime,page,accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private  PddOrderListGetResponse getOrderListGetResponse(PopClient client,int orderStatus,long startTime,long endTime, int page, String accessToken) {
        // System.out.println("正在获取第" + page + "页订单...");
        try {
            PddOrderListGetRequest request = new PddOrderListGetRequest();
            request.setOrderStatus(orderStatus);
            request.setRefundStatus(5);
            request.setStartConfirmAt(startTime);
            request.setEndConfirmAt(endTime);
            request.setPage(page);
            request.setPageSize(pageSize);
            PddOrderListGetResponse response = client.syncInvoke(request, accessToken);
            // System.out.println(JsonUtil.transferToJson(response));
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public  void calOrderList(List<PddOrderListGetResponse.OrderListGetResponseOrderListItem> orderList,SaleResult saleResult) {
        for (PddOrderListGetResponse.OrderListGetResponseOrderListItem orderListItem : orderList) {
            List<PddOrderListGetResponse.OrderListGetResponseOrderListItemItemListItem> itemList = orderListItem.getItemList();
            for (PddOrderListGetResponse.OrderListGetResponseOrderListItemItemListItem item : itemList) {
                int findGoodIndex = findGoodItemInList(item.getGoodsId(), saleResult.daySale);
                if (findGoodIndex == -1) {
                    GoodItem goodItem = new GoodItem();
                    goodItem.goods_id = item.getGoodsId();
                    goodItem.goods_name = item.getGoodsName();
                    goodItem.pay_amount += orderListItem.getPayAmount();
                    goodItem.sale_count += item.getGoodsCount();
                    SkuItem skuItem = new SkuItem();
                    skuItem.sku_id = item.getSkuId();
                    skuItem.spec = item.getGoodsSpec();
                    skuItem.sale_count += item.getGoodsCount();
                    goodItem.sku_list.add(skuItem);
                    saleResult.daySale.add(goodItem);
                } else {
                    GoodItem goodItem = saleResult.daySale.get(findGoodIndex);
                    goodItem.pay_amount += orderListItem.getPayAmount();
                    goodItem.sale_count += item.getGoodsCount();
                    int findSkuIndex = findSkuItemInList(item.getSkuId(), goodItem.sku_list);
                    if (findSkuIndex == -1) {
                        SkuItem skuItem = new SkuItem();
                        skuItem.sku_id = item.getSkuId();
                        skuItem.spec = item.getGoodsSpec();
                        skuItem.sale_count += item.getGoodsCount();
                        goodItem.sku_list.add(skuItem);
                    } else {
                        SkuItem skuItem = goodItem.sku_list.get(findSkuIndex);
                        skuItem.sale_count += item.getGoodsCount();
                    }
                }
                saleResult.pay_amount += orderListItem.getPayAmount();
            }

        }
    }

    public  int findGoodItemInList(String goodId, List<GoodItem> goodItemList) {
        for (int i = 0; i < goodItemList.size(); i++) {
            GoodItem goodItem = goodItemList.get(i);
            if (goodId.equals(goodItem.goods_id)) {
                return i;
            }
        }
        return -1;
    }

    public  int findSkuItemInList(String skuId, List<SkuItem> skuItemList) {
        for (int i = 0; i < skuItemList.size(); i++) {
            SkuItem skuItem = skuItemList.get(i);
            if (skuId.equals(skuItem.sku_id)) {
                return i;
            }
        }
        return -1;
    }

    public  long dateToStamp(String time) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(time);
        return date.getTime();
    }

    public Date yesterday(Date today) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
        return calendar.getTime();
    }

}
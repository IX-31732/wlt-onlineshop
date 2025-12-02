package com.onlineshop.onlineshop.service;

import com.onlineshop.onlineshop.repository.OrderGoodsRepository;
import com.onlineshop.onlineshop.repository.UserOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalesService {

    @Autowired
    private UserOrderRepository userOrderRepository;
    @Autowired
    private OrderGoodsRepository orderGoodsRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private GoodsService goodsService;

    //获取销售统计报表
    public SalesReport getSalesReport(Date startDate, Date endDate) {
        SalesReport report = new SalesReport();
        //订单数量统计
        Long orderCount = userOrderRepository.countByOrderDateBetween(startDate, endDate);
        report.setOrderCount(orderCount);
        //销售总额
        Double totalSales = userOrderRepository.getTotalSalesByDateRange(startDate, endDate);
        report.setTotalSales(totalSales != null ? totalSales : 0.0);
        //商品销售统计
        List<Object[]> goodsSales = orderGoodsRepository.getGoodsSalesByDateRange(startDate, endDate);
        report.setGoodsSales(goodsSales);
        //热销商品
        List<Object[]> bestSellers = orderGoodsRepository.findBestSellingGoods();
        report.setBestSellingGoods(bestSellers);
        return report;
    }

    //获取商家销售统计报表
    public MerchantSalesReport getMerchantSalesReport(Long merchantId, Date startDate, Date endDate) {
        MerchantSalesReport report = new MerchantSalesReport();
        //获取商家相关订单
        List<Object[]> merchantSales = orderGoodsRepository.getMerchantSalesByDateRange(merchantId, startDate, endDate);
        //计算商家订单数量和销售额
        Long orderCount = merchantSales.stream().map(arr -> (Long) arr[0]).distinct().count();
        Double totalSales = merchantSales.stream().mapToDouble(arr -> (Double) arr[3]).sum();
        report.setOrderCount(orderCount);
        report.setTotalSales(totalSales);
        report.setGoodsSales(merchantSales);
        return report;
    }

    //获取热销商品
    public List<Object[]> getBestSellingGoods() {
        return orderGoodsRepository.findBestSellingGoods();
    }

    //获取商家的热销商品
    public List<Object[]> getMerchantBestSellingGoods(Long merchantId) {
        return orderGoodsRepository.findMerchantBestSellingGoods(merchantId);
    }

    //获取商品销售统计
    public List<Object[]> getGoodsSalesByDateRange(Date startDate, Date endDate) {
        return orderGoodsRepository.getGoodsSalesByDateRange(startDate, endDate);
    }

    //获取商家在指定时间段的销售额
    public Double getMerchantSalesByDateRange(Long merchantId, Date startDate, Date endDate) {
        List<Object[]> salesData = orderGoodsRepository.getMerchantSalesByDateRange(merchantId, startDate, endDate);
        return salesData.stream().mapToDouble(arr -> (Double) arr[3]).sum();
    }

    //获取商家商品统计
    public GoodsService.MerchantGoodsStats getMerchantGoodsStats(Long merchantId) {
        return goodsService.getMerchantGoodsStats(merchantId);
    }

    //销售统计报表类
    public static class SalesReport {
        private Long orderCount;
        private Double totalSales;
        private List<Object[]> goodsSales;
        private List<Object[]> bestSellingGoods;
        //getter和setter函数
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public Double getTotalSales() { return totalSales; }
        public void setTotalSales(Double totalSales) { this.totalSales = totalSales; }
        public List<Object[]> getGoodsSales() { return goodsSales; }
        public void setGoodsSales(List<Object[]> goodsSales) { this.goodsSales = goodsSales; }
        public List<Object[]> getBestSellingGoods() { return bestSellingGoods; }
        public void setBestSellingGoods(List<Object[]> bestSellingGoods) { this.bestSellingGoods = bestSellingGoods; }
    }

    //商家销售统计报表类
    public static class MerchantSalesReport {
        private Long orderCount;
        private Double totalSales;
        private List<Object[]> goodsSales;
        //getter和setter函数
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public Double getTotalSales() { return totalSales; }
        public void setTotalSales(Double totalSales) { this.totalSales = totalSales; }
        public List<Object[]> getGoodsSales() { return goodsSales; }
        public void setGoodsSales(List<Object[]> goodsSales) { this.goodsSales = goodsSales; }
    }
}
package com.onlineshop.onlineshop.service;

import com.onlineshop.onlineshop.entity.UserOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
    //发送发货确认邮件
    public void sendShippingConfirmation(String toEmail, UserOrder order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); //设置发件人
            message.setTo(toEmail);
            message.setSubject("订单发货通知 - 订单号: " + order.getOid());
            message.setText("尊敬的顾客，您的订单已发货。订单号: " + order.getOid() +
                    "\n总金额: " + order.getTotalAmount() +
                    "\n物流信息：请登录系统查看详细信息" +
                    "\n感谢您的购买！");
            mailSender.send(message);
            System.out.println("发货确认邮件已发送至: " + toEmail);
        } catch (Exception e) {
            System.err.println("发送邮件失败: " + e.getMessage());
        }
    }

    //发送订单确认邮件
    public void sendOrderConfirmation(String toEmail, UserOrder order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); //设置发件人
            message.setTo(toEmail);
            message.setSubject("订单确认 - 订单号: " + order.getOid());
            message.setText("您的订单已成功创建。订单号: " + order.getOid() +
                    "\n总金额: " + order.getTotalAmount() +
                    "\n订单状态: " + order.getStatus() +
                    "\n请及时完成付款。");
            mailSender.send(message);
            System.out.println("订单确认邮件已发送至: " + toEmail);
        } catch (Exception e) {
            System.err.println("发送邮件失败: " + e.getMessage());
        }
    }
}
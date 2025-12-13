package com.crm.utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailUtils {
    @Resource
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @PostConstruct
    public void init() {
        log.info("当前配置的发件人邮箱：{}", fromEmail);
    }

    /**
     * 发送合同审核通过通知
     */
    // 发送邮件方法中，也打印fromEmail
    public void sendContractApprovedEmail(String toEmail, String contractName, String comment, String contractNumber) {
        try {
            log.info("发件人邮箱：{}，收件人邮箱：{}", fromEmail, toEmail); // 新增日志
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 关键：必须是配置的username，且和SMTP授权的邮箱一致
            message.setTo(toEmail);
            message.setSubject("【合同审核通知】您的合同已通过审核");
            message.setText(String.format(
                    "尊敬的用户：\n\n您创建的合同《%s》已通过审核！\n审核意见：%s\n合同编号：%s",
                    contractName, comment, contractNumber
            ));
            javaMailSender.send(message);
            log.info("审核通过邮件发送成功");
        } catch (Exception e) {
            log.error("发送审核通过邮件失败", e);
        }
    }

    // 拒绝邮件方法同理，加日志
    public void sendContractRejectedEmail(String toEmail, String contractName, String comment, String contractNumber) {
        try {
            log.info("发件人邮箱：{}，收件人邮箱：{}", fromEmail, toEmail);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【合同审核通知】您的合同未通过审核");
            message.setText(String.format(
                    "尊敬的用户：\n\n您创建的合同《%s》未通过审核！\n审核意见：%s\n合同编号：%s",
                    contractName, comment, contractNumber
            ));
            javaMailSender.send(message);
            log.info("审核拒绝邮件发送成功");
        } catch (Exception e) {
            log.error("发送审核拒绝邮件失败", e);
        }
    }
}

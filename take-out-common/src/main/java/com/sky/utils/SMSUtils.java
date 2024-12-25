package com.sky.utils;

import com.aliyun.tea.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信发送工具类
 */
@Slf4j
public class SMSUtils {

	/**
	 * 使用AK&SK初始化账号Client
	 * @return Client
	 * @throws Exception
	 */
	public static com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
		// 工程代码泄露可能会导致 AccessKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考。
		// 建议使用更安全的 STS 方式，更多鉴权访问方式请参见：https://help.aliyun.com/document_detail/378657.html。
		com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
				// 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID。
				.setAccessKeyId(accessKeyId)
				// 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET。
				.setAccessKeySecret(accessKeySecret);
		// Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
		config.endpoint = "dysmsapi.aliyuncs.com";
		return new com.aliyun.dysmsapi20170525.Client(config);
	}

	public static void sendMessage(String phoneNumbers, String param, String accessKeyId, String accessKeySecret) throws Exception {
		com.aliyun.dysmsapi20170525.Client client = SMSUtils.createClient(accessKeyId, accessKeySecret);
		com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
				.setSignName("Reggie")
				.setTemplateCode("SMS_465402623")
				.setPhoneNumbers(phoneNumbers)
				.setTemplateParam("{\"code\":\""+param+"\"}");
		com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
		try {
			// 复制代码运行请自行打印 API 的返回值
			client.sendSmsWithOptions(sendSmsRequest, runtime);
		} catch (TeaException error) {
			// 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
			// 错误 message
			System.out.println(error.getMessage());
			// 诊断地址
			System.out.println(error.getData().get("Recommend"));
			com.aliyun.teautil.Common.assertAsString(error.message);
		} catch (Exception _error) {
			TeaException error = new TeaException(_error.getMessage(), _error);
			// 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
			// 错误 message
			System.out.println(error.getMessage());
			// 诊断地址
			System.out.println(error.getData().get("Recommend"));
			com.aliyun.teautil.Common.assertAsString(error.message);
		}
	}
}





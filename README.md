# strategy-bot

币安合约网格套利机器人  

[定制化需求或者开发可加v：lucky_zht]

测试地址：https://52quant.net/


前端代码：https://github.com/ZRory/strategy-bot-web

需要环境：java8, mysql，redis


使用步骤： （必须能链接到BINANCE API才能使用本机器人，局域网代理见yml配置）


1.在MySQL执行resources中的初始化脚本 初始化数据库

2.配置数据库链接信息到application-local.yml/application-prod.yml配置文件中

3.用户注册和找回密码相关使用的短信服务类：SmsService 需要你们自己申请服务提供商并接入 或者通过日志打印来注册

![image](https://user-images.githubusercontent.com/31235873/168237254-fd7f8fbb-0dc9-4d84-9330-f1fedcd9bac6.png)

![image](https://user-images.githubusercontent.com/31235873/168237306-07d5c71f-92d5-4542-b06b-09573a4bce46.png)

![image](https://user-images.githubusercontent.com/31235873/168237358-d7618eef-0f80-482d-8d94-d73a96623c01.png)

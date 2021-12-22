package com.thinkdifferent.convertoffice.config;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @auther zz
 * @Date 2019/4/26 15:08
 */
@Configuration
public class RabbitMQConfig
{
//    public static Log log = LogFactory.getLog(RabbitMQConfig.class.getName());

    @Value(value = "${spring.rabbitmq.host}")
    private String host;

    @Value(value = "${spring.rabbitmq.port}")
    private int port;

    @Value(value = "${spring.rabbitmq.username}")
    private String username;

    @Value(value = "${spring.rabbitmq.password}")
    private String password;

    /**
     * Broker:它提供一种传输服务,它的角色就是维护一条从生产者到消费者的路线，保证数据能按照指定的方式进行传输,
     * Exchange：消息交换机,它指定消息按什么规则,路由到哪个队列。
     * Queue:消息的载体,每个消息都会被投到一个或多个队列。
     * Binding:绑定，它的作用就是把exchange和queue按照路由规则绑定起来.
     * Routing Key:路由关键字,exchange根据这个关键字进行消息投递。
     * vhost:虚拟主机,一个broker里可以有多个vhost，用作不同用户的权限分离。
     * Producer:消息生产者,就是投递消息的程序.
     * Consumer:消息消费者,就是接受消息的程序.
     * Channel:消息通道,在客户端的每个连接里,可建立多个channel.
     */

    // 配置“交换机”
    // 接收队列交换机
    public static final String EXECHANGE_RECEIVE = "exchange_receive";//交换机

    // 配置“队列名称”
    // 接收队列名称
    public static final String QUEUE_RECEIVE = "queue_receive";//请求队列名称

    // 配置“路由关键字”
    // 接收队列路由关键字
    public static final String ROUTING_RECEIVE = "routing_receive";//路由关键字

    
    @Bean
    public ConnectionFactory connectionFactory()
    {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost("/");
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        return connectionFactory;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    //必须是prototype类型
    public RabbitTemplate rabbitTemplate()
    {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        return template;
    }

    /**
     * 针对消费者配置
     * 1. 设置交换机类型
     * 2. 将队列绑定到交换机
     * FanoutExchange: 将消息分发到所有的绑定队列，无routingkey的概念
     * HeadersExchange ：通过添加属性key-value匹配
     * DirectExchange:按照routingkey分发到指定队列
     * TopicExchange:多关键字匹配
     * 只需要设置交换机和队列都持久化 就能实现队列和消息持久化
     * RabbitMQ的交换器类型一共有四种（direct、fanout、topic以及headers），每一种类型实现了不同的路由算法，
     * 其中direct类型交换器非常简单，当声明一个队列时，它会自动绑定到direct类型交换器（默认条件下，是一个空白字符串名称的交换器），
     * 并以队列名称作为路由键；当消息发送到RabbitMQ后所拥有的路由键与绑定使用的路由键匹配，消息就被投递到对应的队列。
     * headers交换器和direct交换器完全一致，但性能会差很多，headers交换器允许匹配AMQP消息的是header而非路由键，因此它并不实用。
     * fanout交换器可以将收到的消息投递给所有附件在此交换器上的队列。topic交换器可以使得来自不同源头的消息能够到达同一个队列
     */
    
    // 接收  交换机
    @Bean
    public DirectExchange defaultExchange_receive()
    {
        //默认是交换机持久化
        return new DirectExchange(EXECHANGE_RECEIVE);
    }

    
    // 接收  队列
    @Bean
    public Queue queue_recieve()
    {
        return new Queue(QUEUE_RECEIVE, true); //队列持久
    }


    /**
     * 将消息和交换机进行绑定
     * @return
     */
    // 接收队列和接收交换机绑定
    @Bean
    public Binding binding_queue_reecive()
    {

        return BindingBuilder.bind(queue_recieve()).to(defaultExchange_receive()).with(ROUTING_RECEIVE);
    }


}

package com.xxl.job.core.handler.annotation;

import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.enums.ExecutorRouteStrategyEnum;

import java.lang.annotation.*;

/**
 * annotation for job handler
 * @author 2016-5-17 21:06:49
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JobRegister {

    String jobCron();// 任务执行CRON表达式，必填

    String jobDesc();// 任务描述，必填

    String author() default "auto-xxl"; // 负责人

    //GlueTypeEnum glueType() default GlueTypeEnum.BEAN; // GLUE类型

    ExecutorRouteStrategyEnum executorRouteStrategy() default ExecutorRouteStrategyEnum.FIRST; // 执行器路由策略，必填 默认：FIRST

    String executorParam() default "";  // 执行器，任务参数

    ExecutorBlockStrategyEnum executorBlockStrategy() default ExecutorBlockStrategyEnum.SERIAL_EXECUTION; // 阻塞处理策略，必填 默认：SERIAL_EXECUTION

    int executorTimeout() default 0; // 任务执行超时时间，单位秒

    int executorFailRetryCount() default 0; // 失败重试次数

    //String childJobId(); // 子任务ID，多个逗号分隔
}

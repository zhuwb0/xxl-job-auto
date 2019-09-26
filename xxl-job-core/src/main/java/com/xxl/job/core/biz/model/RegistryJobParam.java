package com.xxl.job.core.biz.model;

import java.io.Serializable;

/**
 * Created by xuxueli on 16/7/22.
 */
public class RegistryJobParam implements Serializable{
    private static final long serialVersionUID = 42L;

    private String jobCron;		// 任务执行CRON表达式，必填
    private String jobDesc;

    private String author;		// 负责人 auto-xxl

    private String glueType;		// GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum

    private String executorRouteStrategy;	// 执行器路由策略，必填 默认：FIRST

    private String executorHandler;		    // 执行器，任务Handler名称，必填 @JobHandler的value

    private String executorParam;		    // 执行器，任务参数

    private String executorBlockStrategy;	// 阻塞处理策略，必填 默认：SERIAL_EXECUTION

    private int executorTimeout;     		// 任务执行超时时间，单位秒

    private int executorFailRetryCount;		// 失败重试次数

    private String childJobId;		// 子任务ID，多个逗号分隔

    public String getJobCron() {
        return jobCron;
    }

    public void setJobCron(String jobCron) {
        this.jobCron = jobCron;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getExecutorRouteStrategy() {
        return executorRouteStrategy;
    }

    public void setExecutorRouteStrategy(String executorRouteStrategy) {
        this.executorRouteStrategy = executorRouteStrategy;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParam() {
        return executorParam;
    }

    public void setExecutorParam(String executorParam) {
        this.executorParam = executorParam;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public int getExecutorFailRetryCount() {
        return executorFailRetryCount;
    }

    public void setExecutorFailRetryCount(int executorFailRetryCount) {
        this.executorFailRetryCount = executorFailRetryCount;
    }

    public String getChildJobId() {
        return childJobId;
    }

    public void setChildJobId(String childJobId) {
        this.childJobId = childJobId;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    @Override
    public String toString() {
        return "RegistryJobParam{" +
                "jobCron='" + jobCron + '\'' +
                ", jobDesc='" + jobDesc + '\'' +
                ", author='" + author + '\'' +
                ", glueType='" + glueType + '\'' +
                ", executorRouteStrategy='" + executorRouteStrategy + '\'' +
                ", executorHandler='" + executorHandler + '\'' +
                ", executorParam='" + executorParam + '\'' +
                ", executorBlockStrategy='" + executorBlockStrategy + '\'' +
                ", executorTimeout=" + executorTimeout +
                ", executorFailRetryCount=" + executorFailRetryCount +
                ", childJobId='" + childJobId + '\'' +
                '}';
    }
}

package com.xxl.job.core.executor.impl;

import com.xxl.job.core.biz.model.RegistryJobParam;
import com.xxl.job.core.cron.CronExpression;
import com.xxl.job.core.enums.ExecutorRouteStrategyEnum;
import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.handler.annotation.JobRegister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Map;

/**
 * xxl-job executor (for spring)
 *
 * @author xuxueli 2018-11-01 09:24:52
 */
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware {


    @Override
    public void start() throws Exception {

        // init JobHandler Repository
        initJobHandlerRepository(applicationContext);

        // refresh GlueFactory
        GlueFactory.refreshInstance(1);


        // super start
        super.start();
    }

    private void initJobHandlerRepository(ApplicationContext applicationContext){
        if (applicationContext == null) {
            return;
        }

        // init job handler action
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(JobHandler.class);

        if (serviceBeanMap!=null && serviceBeanMap.size()>0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                if (serviceBean instanceof IJobHandler){
                    String name = serviceBean.getClass().getAnnotation(JobHandler.class).value();
                    IJobHandler handler = (IJobHandler) serviceBean;
                    if (loadJobHandler(name) != null) {
                        throw new RuntimeException("xxl-job jobhandler naming conflicts.");
                    }
                    registJobHandler(name, handler);
                    initJobRegParam(serviceBean, name);
                }
            }
        }
    }

    private void initJobRegParam(Object serviceBean, String name) {
        if (!serviceBean.getClass().isAnnotationPresent(JobRegister.class)) {
            return;
        }
        JobRegister jobRegister = serviceBean.getClass().getAnnotation(JobRegister.class);
        if (!CronExpression.isValidExpression(jobRegister.jobCron())) {
            throw new XxlJobException("JobHandler:["+name+"], jobinfo_field_cron_unvalid："+jobRegister.jobCron());
        }
        if (jobRegister.jobDesc()==null || jobRegister.jobDesc().trim().length()==0) {
            throw new XxlJobException("JobHandler:["+name+"], jobinfo_field_jobdesc");
        }
        if (jobRegister.author()==null || jobRegister.author().trim().length()==0) {
            throw new XxlJobException("JobHandler:["+name+"], jobinfo_field_author");
        }
        if (jobParamList == null) {
            jobParamList = new ArrayList<>();
        }
        RegistryJobParam jobParam = new RegistryJobParam();
        jobParam.setAuthor(jobRegister.author());
        jobParam.setJobCron(jobRegister.jobCron());
        jobParam.setJobDesc(jobRegister.jobDesc());
        jobParam.setExecutorBlockStrategy(jobRegister.executorBlockStrategy().name());
        jobParam.setExecutorRouteStrategy(jobRegister.executorRouteStrategy().name());
        jobParam.setExecutorFailRetryCount(jobRegister.executorFailRetryCount());
        jobParam.setExecutorHandler(name);
        jobParam.setExecutorParam(jobRegister.executorParam());
        jobParam.setExecutorTimeout(jobRegister.executorTimeout());
        jobParam.setGlueType(GlueTypeEnum.BEAN.name());
        jobParamList.add(jobParam);
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}

package com.xxl.job.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryJobParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author xuxueli 2017-07-27 21:54:20
 */
@Service
public class AdminBizImpl implements AdminBiz {
    private static Logger logger = LoggerFactory.getLogger(AdminBizImpl.class);

    @Resource
    public XxlJobLogDao xxlJobLogDao;
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobService xxlJobService;


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        for (HandleCallbackParam handleCallbackParam: callbackParamList) {
            ReturnT<String> callbackResult = callback(handleCallbackParam);
            logger.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                    (callbackResult.getCode()==IJobHandler.SUCCESS.getCode()?"success":"fail"), handleCallbackParam, callbackResult);
        }

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        // valid log item
        XxlJobLog log = xxlJobLogDao.load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log repeate callback.");     // avoid repeat callback, trigger child job etc
        }

        // trigger success, to trigger child job
        String callbackMsg = null;
        if (IJobHandler.SUCCESS.getCode() == handleCallbackParam.getExecuteResult().getCode()) {
            XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(log.getJobId());
            if (xxlJobInfo!=null && xxlJobInfo.getChildJobId()!=null && xxlJobInfo.getChildJobId().trim().length()>0) {
                callbackMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";

                String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
                    if (childJobId > 0) {

                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        callbackMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode()==ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg());
                    } else {
                        callbackMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i]);
                    }
                }

            }
        }

        // handle msg
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg()!=null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getExecuteResult().getMsg() != null) {
            handleMsg.append(handleCallbackParam.getExecuteResult().getMsg());
        }
        if (callbackMsg != null) {
            handleMsg.append(callbackMsg);
        }

        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getExecuteResult().getCode());
        log.setHandleMsg(handleMsg.toString());
        xxlJobLogDao.updateHandleInfo(log);

        return ReturnT.SUCCESS;
    }

    private boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public synchronized ReturnT<String> registry(RegistryParam registryParam) {
        int ret = xxlJobRegistryDao.registryUpdate(registryParam.getRegistGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
        if (ret < 1) {
            xxlJobRegistryDao.registrySave(registryParam.getRegistGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
            /**
             * 注册或更新执行器信息
             * 1、判断执行器是否存在
             * 2、新增或更新执行器
             */
            XxlJobGroup xxlJobGroup = xxlJobGroupDao.loadByAppName(registryParam.getRegistryKey());
            if (xxlJobGroup == null) {
                xxlJobGroup = buildXxlJobGroup(registryParam.getRegistryKey(), registryParam.getRegistryValue());
                xxlJobGroupDao.save(xxlJobGroup);
            } else {
                List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppName());
                String addressListStr = null;
                if (registryList!=null && !registryList.isEmpty()) {
                    Collections.sort(registryList);
                    addressListStr = "";
                    for (String item:registryList) {
                        addressListStr += item + ",";
                    }
                    addressListStr = addressListStr.substring(0, addressListStr.length()-1);
                }
                xxlJobGroup.setAddressList(addressListStr);
                xxlJobGroupDao.update(xxlJobGroup);
            }
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 启动自动注册job
     * 判断执行器是否存在
     * 1、校验是否存在该job(执行器+JobHandler唯一)
     * 2、存在判断是否为以注解为准还是以数据库中为准
     *  2.1、以注解为准每次强制更新
     *  2.2、以数据库中为准则结束
     * @param registryParam
     * @return
     */
    @Override
    public synchronized ReturnT<String> registryJob(RegistryParam registryParam) {
        XxlJobGroup xxlJobGroup = xxlJobGroupDao.loadByAppName(registryParam.getRegistryKey());
        if (xxlJobGroup == null) {
            //未找到执行器，注册job失败
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<RegistryJobParam> jobParamList = mapper.readValue(registryParam.getRegistryValue(), new TypeReference<List<RegistryJobParam>>() {});
            jobParamList.forEach(jobParam -> {
                XxlJobInfo jobInfo = xxlJobInfoDao.loadByExecutorHandlerAndGroup(xxlJobGroup.getId(), jobParam.getExecutorHandler());
                if (jobInfo == null) {
                    //insert
                    jobInfo = buildXxlJobInfo(jobParam, xxlJobGroup.getId());
                    xxlJobInfoDao.save(jobInfo);
                }
                //FIXME 更新暂不做
            });
        } catch (Exception e) {
            logger.error("registryJob appName:{} error:{}", registryParam.getRegistryKey(), e.getMessage());
            return ReturnT.FAIL;
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        xxlJobRegistryDao.registryDelete(registryParam.getRegistGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
        return ReturnT.SUCCESS;
    }

    private XxlJobInfo buildXxlJobInfo(RegistryJobParam jobParam, int jobGroup){
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setAuthor(jobParam.getAuthor());
        jobInfo.setJobGroup(jobGroup);
        jobInfo.setJobDesc(jobParam.getJobDesc());
        jobInfo.setJobCron(jobParam.getJobCron());
        jobInfo.setExecutorHandler(jobParam.getExecutorHandler());
        jobInfo.setExecutorParam(jobParam.getExecutorParam());
        jobInfo.setExecutorRouteStrategy(jobParam.getExecutorRouteStrategy());
        jobInfo.setExecutorBlockStrategy(jobParam.getExecutorBlockStrategy());
        jobInfo.setExecutorTimeout(jobParam.getExecutorTimeout());
        jobInfo.setExecutorFailRetryCount(jobParam.getExecutorFailRetryCount());
        jobInfo.setGlueType(jobParam.getGlueType());
        jobInfo.setChildJobId(jobParam.getChildJobId());
        return jobInfo;
    }

    private XxlJobGroup buildXxlJobGroup(String appName, String address){
        XxlJobGroup group = new XxlJobGroup();
        group.setAddressList(address);
        group.setAppName(appName);
        group.setTitle(appName);
        group.setAddressType(0);
        group.setOrder(1);
        return group;
    }

    private List<String> findRegistryByAppName(String appNameParam){
        HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
        List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT);
        if (list != null) {
            for (XxlJobRegistry item: list) {
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appName = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appName);
                    if (registryList == null) {
                        registryList = new ArrayList<String>();
                    }

                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appName, registryList);
                }
            }
        }
        return appAddressMap.get(appNameParam);
    }
}

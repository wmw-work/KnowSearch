package com.didichuxing.datachannel.arius.admin.biz.template.srv.base.impl;

import static com.didichuxing.datachannel.arius.admin.common.constant.AdminConstant.MILLIS_PER_DAY;
import static com.didichuxing.datachannel.arius.admin.common.constant.AriusConfigConstant.ARIUS_COMMON_GROUP;
import static com.didichuxing.datachannel.arius.admin.common.constant.AriusConfigConstant.CLUSTERS_INDEX_EXPIRE_DELETE_AHEAD;
import static com.didichuxing.datachannel.arius.admin.common.constant.AriusConfigConstant.INDEX_OPERATE_AHEAD_SECONDS;
import static com.didichuxing.datachannel.arius.admin.common.util.IndexNameFactory.genIndexNameClear;

import com.didichuxing.datachannel.arius.admin.biz.template.TemplatePhyManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.TemplateSrvManager;
import com.didichuxing.datachannel.arius.admin.biz.template.srv.base.BaseTemplateSrv;
import com.didichuxing.datachannel.arius.admin.common.bean.common.BaseResult;
import com.didichuxing.datachannel.arius.admin.common.bean.common.OperateRecord;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.template.IndexTemplateDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterPhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ClusterTemplateSrv;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplate;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplateLogicWithClusterAndMasterTemplate;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhy;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.template.IndexTemplatePhyWithLogic;
import com.didichuxing.datachannel.arius.admin.common.constant.ESClusterVersionEnum;
import com.didichuxing.datachannel.arius.admin.common.constant.operaterecord.OperateTypeEnum;
import com.didichuxing.datachannel.arius.admin.common.constant.operaterecord.TriggerWayEnum;
import com.didichuxing.datachannel.arius.admin.common.exception.AdminOperateException;
import com.didichuxing.datachannel.arius.admin.common.tuple.TupleTwo;
import com.didichuxing.datachannel.arius.admin.common.tuple.Tuples;
import com.didichuxing.datachannel.arius.admin.common.util.AriusDateUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.ESVersionUtil;
import com.didichuxing.datachannel.arius.admin.common.util.IndexNameFactory;
import com.didichuxing.datachannel.arius.admin.common.util.ListUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ProjectUtils;
import com.didichuxing.datachannel.arius.admin.common.util.TemplateUtils;
import com.didichuxing.datachannel.arius.admin.core.service.cluster.physic.ClusterPhyService;
import com.didichuxing.datachannel.arius.admin.core.service.common.AriusConfigInfoService;
import com.didichuxing.datachannel.arius.admin.core.service.common.OperateRecordService;
import com.didichuxing.datachannel.arius.admin.core.service.template.logic.IndexTemplateService;
import com.didichuxing.datachannel.arius.admin.core.service.template.physic.IndexTemplatePhyService;
import com.didiglobal.logi.log.ILog;
import com.didiglobal.logi.log.LogFactory;
import com.didiglobal.logi.security.service.ProjectService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author chengxiang
 * @date 2022/5/11
 */
public abstract class BaseTemplateSrvImpl implements BaseTemplateSrv {
    protected static final ILog LOGGER = LogFactory.getLog(BaseTemplateSrvImpl.class);

    @Autowired
    protected IndexTemplateService    indexTemplateService;

    @Autowired
    protected IndexTemplatePhyService indexTemplatePhyService;

    @Autowired
    protected TemplatePhyManager      templatePhyManager;

    @Autowired
    protected TemplateSrvManager      templateSrvManager;

    @Autowired
    protected ClusterPhyService         clusterPhyService;
    @Autowired
    protected OperateRecordService operateRecordService;
    @Autowired
    protected ProjectService         projectService;
    @Autowired
    protected AriusConfigInfoService ariusConfigInfoService;
   
    
    @Override
    public boolean isTemplateSrvOpen(Integer templateId) {
        return templateSrvManager.isTemplateSrvOpen(templateId, templateSrv().getCode());
    }

    @Override
    public String templateSrvName() {
        return templateSrv().getServiceName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> openSrv(List<Integer> templateIdList, String operator, Integer projectId) throws AdminOperateException {
        // 0.校验服务是否可以开启
        for (Integer templateId : templateIdList) {
            Result<Void> checkAvailableResult = checkSrvIsValid(templateId);
            if (checkAvailableResult.failed()) { return checkAvailableResult;}
        }

        // 1.更新DB服务开启状态
        Result<Void> updateResult = updateSrvStatus(templateIdList, Boolean.TRUE,operator,projectId);
        if (updateResult.failed()) { return updateResult;}

        return Result.buildSucc();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> closeSrv(List<Integer> templateIdList, String operator, Integer projectId) throws AdminOperateException {
        // 0.更新DB服务关闭状态
        Result<Void> updateResult = updateSrvStatus(templateIdList, Boolean.FALSE, operator, projectId);
        if (updateResult.failed()) { return updateResult;}

        return Result.buildSucc();
    }

    protected Result<Void> checkSrvIsValid(Integer logicTemplateId) {
        ESClusterVersionEnum requireESClusterVersion = templateSrv().getEsClusterVersion();

        IndexTemplateLogicWithClusterAndMasterTemplate template = indexTemplateService.getLogicTemplateWithClusterAndMasterTemplate(logicTemplateId);
        if (null == template || null == template.getMasterTemplate()) {
            LOGGER.warn("class=ColdManagerImpl||method=isTemplateSrvAvailable||templateId={}||errMsg=masterPhyTemplate is null",
                    logicTemplateId);
            return Result.buildFail();
        }

        String masterCluster = template.getMasterTemplate().getCluster();
        ClusterPhy clusterPhy = clusterPhyService.getClusterByName(masterCluster);
        if (null == clusterPhy) {
            LOGGER.warn("class=ColdManagerImpl||method=isTemplateSrvAvailable||templateId={}||errMsg=clusterPhy of template is null",
                    logicTemplateId);
            return Result.buildFail();
        }

        String esVersion = clusterPhy.getEsVersion();

        if (ESVersionUtil.isHigher(requireESClusterVersion.getVersion(), esVersion)) {
            return Result.buildFail(String.format("不支持该模板服务, 模板[%s]归属集群目前版本为:%s, 模板服务需要的最低版本为:%s",
                    logicTemplateId,esVersion, requireESClusterVersion.getVersion()));
        }
        return Result.buildSucc();
    }

    /******************************************private************************************************/
    /**
     * 更新DB服务状态
     *
     * @param status,        true:开启, false:关闭
     * @param templateIdList
     * @param operator
     * @param projectId
     * @return
     */
    private Result<Void> updateSrvStatus(List<Integer> templateIdList, Boolean status, String operator,
                                         Integer projectId) throws AdminOperateException {
        String srvCode = templateSrv().getCode().toString();
        List<TupleTwo</*old*/IndexTemplate,/*change*/IndexTemplate>> tupleTwos= Lists.newArrayList();
        for (Integer templateId : templateIdList) {
            IndexTemplate indexTemplate = indexTemplateService.getLogicTemplateById(templateId);
            

            if (null == indexTemplate) { continue;}
            TupleTwo</*old*/IndexTemplate,/*change*/IndexTemplate> tupleTwo = Tuples.of(indexTemplate,null);
            if (status) {
                addSrvCode(indexTemplate, srvCode);
            }
            else { removeSrvCode(indexTemplate, srvCode);}
    
            tupleTwo= tupleTwo.update2(indexTemplate);
            tupleTwos.add(tupleTwo);
        }
        //确认操作项目的合法性
        if (tupleTwos.stream().map(TupleTwo::v1)
                .map(oldIndexTemplate -> ProjectUtils.checkProjectCorrectly(IndexTemplate::getProjectId,
                        oldIndexTemplate, projectId)).allMatch(BaseResult::failed)) {
            return Result.buildFail("当前项目不属于超级项目或者持有该操作的项目");
        }
        for (TupleTwo<IndexTemplate, IndexTemplate> tupleTwo : tupleTwos) {
            final Result<Void> result = indexTemplateService.editTemplateInfoTODB(
                    ConvertUtil.obj2Obj(tupleTwo.v2, IndexTemplateDTO.class));
            if (result.success()) {
                operateRecordService.save(
                        new OperateRecord.Builder().operationTypeEnum(OperateTypeEnum.TEMPLATE_SERVICE)
                                .triggerWayEnum(TriggerWayEnum.MANUAL_TRIGGER).bizId(tupleTwo.v2.getId())
                                .project(projectService.getProjectBriefByProjectId(projectId))
                                .content(Boolean.TRUE.equals(status) ? "开启模板服务" : "关闭模板服务").userOperation(operator)
                                .build());
            }
        }
        
        
        return Result.buildSucc();
    }

    /**
     * 添加开启服务到对应模板实体中
     * @param indexTemplate
     * @param addSrvCode
     * @return true:有修改, false:无修改；根据返回值判断是否需要刷新到DB
     */
    private void addSrvCode(IndexTemplate indexTemplate, String addSrvCode) {
        String srvCodeStr = indexTemplate.getOpenSrv();
        List<String> srvCodeList = ListUtils.string2StrList(srvCodeStr);
        if (srvCodeList.isEmpty()) {
            indexTemplate.setOpenSrv(addSrvCode);
        } else {
            if (!srvCodeList.contains(addSrvCode)) {
                indexTemplate.setOpenSrv(srvCodeStr + "," + addSrvCode);
            }
        }
    }

    private void removeSrvCode(IndexTemplate indexTemplate, String removeSrvCode) {
        String srvCodeStr = indexTemplate.getOpenSrv();
        List<String> srvCodeList = ListUtils.string2StrList(srvCodeStr);
        if (srvCodeList.contains(removeSrvCode)) {
            srvCodeList.remove(removeSrvCode);
            indexTemplate.setOpenSrv(ListUtils.strList2String(srvCodeList));
        }
    }
    
    
    
     ///////////////////////////////////srv
     @Override
    public boolean isTemplateSrvOpen(String phyClusterName) {
        boolean enable =isPhyClusterOpenTemplateSrv(phyClusterName, templateSrv().getCode());

        LOGGER.info("class=BaseTemplateSrv||method=enableTemplateSrv||clusterName={}||enable={}||templateSrv={}",
            phyClusterName, enable, templateServiceName());

        return enable;
    }

    @Override
    public boolean isTemplateSrvOpen(List<IndexTemplatePhy> indexTemplatePhies) {
        for (IndexTemplatePhy indexTemplatePhy : indexTemplatePhies) {
            if (!isTemplateSrvOpen(indexTemplatePhy.getCluster())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Result<Boolean> checkOpenTemplateSrvByCluster(String phyCluster){
        return Result.buildSucc(Boolean.TRUE);
    }

    @Override
    public Result<Boolean> checkOpenTemplateSrvWhenClusterJoin(String httpAddresses, String password) {
        return Result.buildSucc(Boolean.TRUE);
    }

    @Override
    public String templateServiceName() {
        return templateSrv().getServiceName();
    }
    ////////private
    private boolean isPhyClusterOpenTemplateSrv(String phyCluster, int srvId) {
        try {
            Result<List<ClusterTemplateSrv>> result =clusterPhyService. getPhyClusterTemplateSrv(phyCluster);
            if (null == result || result.failed()) {
                return false;
            }
        
            List<ClusterTemplateSrv> clusterTemplateSrvs = result.getData();
            for (ClusterTemplateSrv templateSrv : clusterTemplateSrvs) {
                if (srvId == templateSrv.getServiceId()) {
                    return true;
                }
            }
        
            return false;
        } catch (Exception e) {
            LOGGER.warn("class=TemplateSrvManager||method=isPhyClusterOpenTemplateSrv||phyCluster={}||srvId={}",
                    phyCluster, srvId, e);
        
            return true;
        }
    }
    
    
     protected Set<String> getIndexByBeforeDay(IndexTemplatePhyWithLogic physicalWithLogic, int days) {
        try {
            IndexTemplate logicTemplate = physicalWithLogic.getLogicTemplate();

            if (!physicalWithLogic.getExpression().endsWith("*")) {
                return Sets.newHashSet();
            }

            if (!TemplateUtils.isSaveByDay(logicTemplate.getDateFormat())
                    && !TemplateUtils.isSaveByMonth(logicTemplate.getDateFormat())) {
                return Sets.newHashSet();
            }

            List<String> indices = indexTemplatePhyService.getMatchIndexNames(physicalWithLogic.getId());
            if (CollectionUtils.isEmpty(indices)) {
                LOGGER.info("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||msg=no match indices", logicTemplate.getName());
                return Sets.newHashSet();
            }

            return getFinalIndexSet(physicalWithLogic, days, logicTemplate, indices);
        } catch (Exception e) {
            LOGGER.warn("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||templateName={}||errMsg={}", physicalWithLogic.getName(),
                    e.getMessage(), e);
        }

        return Sets.newHashSet();
    }
    private Set<String> getFinalIndexSet(IndexTemplatePhyWithLogic physicalWithLogic, int days, IndexTemplate logicTemplate, List<String> indices) {
        Set<String> finalIndexSet = Sets.newHashSet();
        for (String indexName : indices) {
            if (StringUtils.isBlank(indexName)) {
                continue;
            }

            Date indexTime = IndexNameFactory.genIndexTimeByIndexName(
                    genIndexNameClear(indexName, logicTemplate.getExpression()), logicTemplate.getExpression(),
                    logicTemplate.getDateFormat());

            if (indexTime == null) {
                LOGGER.warn(
                        "class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||indexName={}||msg=template parse index time fail",
                        logicTemplate.getName(), indexName);
                continue;
            }

            if (TemplateUtils.isSaveByMonth(logicTemplate.getDateFormat())) {
                // 需要将索引时间定为当月的最后一天 确保最后一天的数据能被保留到保存时长
                indexTime = AriusDateUtils.getLastDayOfTheMonth(indexTime);
            }

            if (needOperateAhead(physicalWithLogic)) {
                int aheadSeconds = ariusConfigInfoService.intSetting(ARIUS_COMMON_GROUP,
                        INDEX_OPERATE_AHEAD_SECONDS, 2 * 60 * 60);
                indexTime = AriusDateUtils.getBeforeSeconds(indexTime, aheadSeconds);
            }

            long timeIntervalDay = (System.currentTimeMillis() - indexTime.getTime()) / MILLIS_PER_DAY;
            if (timeIntervalDay < days) {
                LOGGER.info(
                        "class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||template={}||indexName={}||timeIntervalDay={}||msg=index not match",
                        logicTemplate.getName(), indexName, timeIntervalDay);
                continue;
            }

            LOGGER.info("class=TemplatePhyManagerImpl||method=getIndexByBeforeDay||indexName={}||indexTime={}||timeIntervalDay={}", indexName,
                    indexTime, timeIntervalDay);

            finalIndexSet.add(indexName);
        }
        return finalIndexSet;
    }
    private boolean needOperateAhead(IndexTemplatePhyWithLogic physicalWithLogic) {
        Set<String> clusterSet = ariusConfigInfoService.stringSettingSplit2Set(ARIUS_COMMON_GROUP,
                CLUSTERS_INDEX_EXPIRE_DELETE_AHEAD, "", ",");
        return clusterSet.contains(physicalWithLogic.getCluster());
    }

}
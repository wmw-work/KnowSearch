package com.didichuxing.datachannel.arius.admin.rest.controller.v3.op.cluster.phy;

import static com.didichuxing.datachannel.arius.admin.common.constant.ApiVersion.V3;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.didichuxing.datachannel.arius.admin.biz.cluster.ClusterPhyManager;
import com.didichuxing.datachannel.arius.admin.common.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.cluster.ClusterJoinDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.cluster.ClusterPhyConditionDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.cluster.ClusterPhyDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.entity.cluster.ecm.ClusterRoleInfo;
import com.didichuxing.datachannel.arius.admin.common.bean.vo.cluster.ClusterPhyVO;
import com.didichuxing.datachannel.arius.admin.common.bean.vo.cluster.ESClusterRoleVO;
import com.didichuxing.datachannel.arius.admin.common.constant.result.ResultType;
import com.didichuxing.datachannel.arius.admin.common.util.AriusObjUtils;
import com.didichuxing.datachannel.arius.admin.common.util.ConvertUtil;
import com.didichuxing.datachannel.arius.admin.common.util.HttpRequestUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

/**
 * 物理集群接口
 *
 * @author ohushenglin_v
 * @date 2022-05-20
 */
@RestController("esPhyClusterControllerV3")
@RequestMapping({ V3 + "/cluster/phy" })
@Api(tags = "ES物理集群集群接口(REST)")
public class ESPhyClusterController {

    @Autowired
    private ClusterPhyManager clusterPhyManager;

    @Value("${zeus.server}")
    private String              zeusServerUrl;

    /**
     * 根据物理集群ID获取全部角色
     */
    @GetMapping("/{clusterId}/roles")
    @ResponseBody
    @ApiOperation(value = "根据物理集群ID获取全部角色列表", notes = "")
    public Result<List<ESClusterRoleVO>> roleList(@PathVariable Integer clusterId) {
        List<ClusterRoleInfo> clusterRoleInfos = clusterPhyManager.listClusterRolesByClusterId(clusterId);

        if (AriusObjUtils.isNull(clusterRoleInfos)) {
            return Result.buildFail(ResultType.NOT_EXIST.getMessage());
        }
        return Result.buildSucc(ConvertUtil.list2List(clusterRoleInfos, ESClusterRoleVO.class));
    }

    @PostMapping("/join")
    @ResponseBody
    @ApiOperation(value = "接入集群", notes = "支持多类型集群加入")
    public Result<ClusterPhyVO> joinCluster(HttpServletRequest request, @RequestBody ClusterJoinDTO param) {
        return clusterPhyManager.joinCluster(param, HttpRequestUtils.getOperator(request));
    }

    @GetMapping("/{clusterLogicType}/{clusterLogicId}/list")
    @ResponseBody
    @ApiOperation(value = "获取逻辑集群可关联region的物理集群名称列表")
    public Result<List<String>> listCanBeAssociatedRegionOfClustersPhys(@PathVariable Integer clusterLogicType,
                                                                        @PathVariable Long clusterLogicId) {
        return clusterPhyManager.listCanBeAssociatedRegionOfClustersPhys(clusterLogicType, clusterLogicId);
    }

    @GetMapping("/{clusterLogicType}/names")
    @ResponseBody
    @ApiOperation(value = "获取逻辑集群可进行关联的物理集群名称")
    public Result<List<String>> listCanBeAssociatedClustersPhys(@PathVariable Integer clusterLogicType) {
        return clusterPhyManager.listCanBeAssociatedClustersPhys(clusterLogicType);
    }

    @GetMapping("/names")
    @ResponseBody
    @ApiOperation(value = "获取项目下的物理集群名称，可根据类型筛选")
    public Result<List<String>> listClusterPhyNameByResourceType(@RequestParam(value = "clusterResourceType", required = false) Integer clusterResourceType,
                                                                 HttpServletRequest request) {
        return clusterPhyManager.listClusterPhyNameByResourceType(clusterResourceType,
            HttpRequestUtils.getAppId(request));
    }

    @GetMapping("/{templateId}/sameversion/clusternames")
    @ResponseBody
    @ApiOperation(value = "根据模板所在集群，获取与该集群相同版本号的集群名称列表")
    public Result<List<String>> getTemplateSameVersionClusterNamesByTemplateId(HttpServletRequest request, @PathVariable Integer templateId) {
        return clusterPhyManager.getTemplateSameVersionClusterNamesByTemplateId(HttpRequestUtils.getAppId(request), templateId);
    }

    @PostMapping("/page")
    @ResponseBody
    @ApiOperation(value = "按条件分页获取物理集群列表")
    public PaginationResult<ClusterPhyVO> pageGetClusterPhys(HttpServletRequest request,
                                                                         @RequestBody ClusterPhyConditionDTO condition) {
        return clusterPhyManager.pageGetClusterPhys(condition, HttpRequestUtils.getAppId(request));
    }

    @GetMapping("/{clusterPhyId}/overview")
    @ResponseBody
    @ApiOperation(value = "获取物理集群概览信息接口")
    @ApiImplicitParam(type = "Integer", name = "clusterPhyId", value = "物理集群ID", required = true)
    public Result<ClusterPhyVO> overview(@PathVariable("clusterPhyId") Integer clusterId, HttpServletRequest request) {
        return Result.buildSucc(clusterPhyManager.getClusterPhyOverview(clusterId, HttpRequestUtils.getAppId(request)));
    }

    @GetMapping("/{clusterLogicType}/{clusterName}/version/list")
    @ResponseBody
    @ApiOperation(value = "根据逻辑集群类型和物理集群名称获取相同版本的可关联的物理名称列表")
    @Deprecated
    public Result<List<String>> getPhyClusterNameWithSameEsVersion(@PathVariable("clusterLogicType") Integer clusterLogicType, @PathVariable(name = "clusterName", required = false) String clusterName) {
        return clusterPhyManager.getPhyClusterNameWithSameEsVersion(clusterLogicType, clusterName);
    }

    @GetMapping("/{clusterLogicId}/bind/version/list")
    @ResponseBody
    @ApiOperation(value = "新建的逻辑集群绑定region的时候进行物理集群版本的校验")
    @Deprecated
    public Result<List<String>> getPhyClusterNameWithSameEsVersionAfterBuildLogic(@PathVariable("clusterLogicId") Long clusterLogicId) {
        return clusterPhyManager.getPhyClusterNameWithSameEsVersionAfterBuildLogic(clusterLogicId);
    }

    @GetMapping("/zeus-url")
    @ResponseBody
    @ApiOperation(value = "获取zeus管控平台跳转接口")
    public Result<String> zeusUrl() {
        return Result.buildSucc(zeusServerUrl);
    }

    @PutMapping("/gateway")
    @ResponseBody
    @ApiOperation(value = "更新物理集群的gateway" )
    public Result<ClusterPhyVO> updateClusterGateway(HttpServletRequest request, @RequestBody ClusterPhyDTO param) {
        return clusterPhyManager.updateClusterGateway(param, HttpRequestUtils.getOperator(request));
    }

    @PutMapping("")
    @ResponseBody
    @ApiOperation(value = "编辑集群接口" )
    public Result<Boolean> edit(HttpServletRequest request, @RequestBody ClusterPhyDTO param) {
        return clusterPhyManager.editCluster(param, HttpRequestUtils.getOperator(request));
    }

    @DeleteMapping("{clusterPhyId}")
    @ResponseBody
    @ApiOperation(value = "删除物理集群" )
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable("clusterPhyId") Integer clusterId) {
        return clusterPhyManager.deleteCluster(clusterId, HttpRequestUtils.getOperator(request));
    }

}

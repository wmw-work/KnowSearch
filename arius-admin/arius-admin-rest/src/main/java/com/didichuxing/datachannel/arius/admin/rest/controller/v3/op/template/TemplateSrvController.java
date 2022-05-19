package com.didichuxing.datachannel.arius.admin.rest.controller.v3.op.template;

import com.didichuxing.datachannel.arius.admin.biz.template.new_srv.TemplateSrvManager;
import com.didichuxing.datachannel.arius.admin.common.bean.common.PaginationResult;
import com.didichuxing.datachannel.arius.admin.common.bean.common.Result;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.template.srv.BaseTemplateSrvOpenDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.dto.template.srv.TemplateSrvQueryDTO;
import com.didichuxing.datachannel.arius.admin.common.bean.vo.template.srv.TemplateWithSrvVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.didichuxing.datachannel.arius.admin.common.constant.ApiVersion.V3_OP;

/**
 * @author chengxiang
 * @date 2022/5/18
 */
@RestController
@RequestMapping(V3_OP + "/template/srv")
@Api(tags = "模板服务接口")
public class TemplateSrvController {

    @Autowired
    private TemplateSrvManager templateSrvManager;

    @PostMapping("/page")
    @ResponseBody
    @ApiOperation(value = "分页模糊查询模板服务")
    public PaginationResult<TemplateWithSrvVO> pageGetTemplateWithSrv(@RequestBody TemplateSrvQueryDTO condition) {
        return templateSrvManager.pageGetTemplateWithSrv(condition);
    }

    @PutMapping("/{srvCode}/{templateIdList}")
    @ResponseBody
    @ApiOperation(value = "开启模板服务")
    public Result<Void> openTemplateSrv(@PathVariable("srvCode") Integer srvCode,
                                        @PathVariable("templateIdList") List<Integer> templateIdList,
                                        @RequestBody BaseTemplateSrvOpenDTO openParam) {
        return templateSrvManager.openSrv(srvCode, templateIdList, openParam);
    }

    @DeleteMapping("/{srvCode}/{templateIdList}")
    @ResponseBody
    @ApiOperation(value = "关闭模板服务")
    public Result<Void> closeTemplateSrv(@PathVariable("srvCode") Integer srvCode,
                                         @PathVariable("templateIdList") List<Integer> templateIdList) {
        return templateSrvManager.closeSrv(srvCode, templateIdList);
    }

    /**
    @PostMapping("/checkAvailable/{srvCode}/{templateIdList}")
    @ResponseBody
    @ApiOperation(value = "检查模板服务是否可用")
    public Result<Void> checkTemplateSrvAvailable(@PathVariable("srvCode") Integer srvCode,
                                                  @PathVariable("templateIdList") List<Integer> templateIdList) {
        return templateSrvManager.checkSrvAvailable(srvCode, templateIdList);
    }
     */
}

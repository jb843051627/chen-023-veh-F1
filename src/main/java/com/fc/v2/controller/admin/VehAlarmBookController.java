package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehAlarmRow;
import com.fc.v2.model.custom.AlarmBookPayload;
import com.fc.v2.model.custom.AlarmBookReceipt;
import com.fc.v2.service.ITVehAlarmRowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 月度报警处置核收 Controller——册子只认这一道口子：从这道门进，别处递来的不算数。
 * 一册一次、一次性进门、整册导入；机构自录与各市转递两条来路走同一道门。
 *
 * @author fuce
 * @date 2026-09-28
 */
@Api(value = "月度报警处置核收")
@Controller
@RequestMapping("/VehAlarmBookController")
public class VehAlarmBookController extends BaseController {

    @Autowired
    private ITVehAlarmRowService vehAlarmRowService;

    @Log(title = "报警处置册整册进门", action = "receive")
    @ApiOperation(value = "整册进门（唯一入口）", notes = "一册一次、整册导入；二遍递交只照面不改库")
    @PostMapping("/receive")
    @RequiresPermissions("veh:vehAlarmBook:receive")
    @ResponseBody
    public AjaxResult receive(@RequestBody AlarmBookPayload payload) {
        if (payload == null || payload.getBookNo() == null || payload.getBookNo().trim().isEmpty()) {
            return AjaxResult.error("册码不许空：册子只认这一道口子，没册码的不进门");
        }
        AlarmBookReceipt receipt = vehAlarmRowService.receiveBook(payload);
        return AjaxResult.success(200, receipt.isDuplicate() ? "同册码二遍递交：已与库存照面，一行未加、一行未减" : "整册进门办结", receipt);
    }

    @Log(title = "报警处置册撤册", action = "void")
    @ApiOperation(value = "撤册", notes = "撤完同册码方可重递；旧行一行不删")
    @PostMapping("/void")
    @RequiresPermissions("veh:vehAlarmBook:void")
    @ResponseBody
    public AjaxResult voidBook(@RequestParam String bookNo,
                               @RequestParam(required = false) String reason) {
        boolean ok = vehAlarmRowService.voidBook(bookNo, reason);
        return ok ? AjaxResult.success("已撤册，同册码可以重递")
                : AjaxResult.error("册码不在有效册中或已撤，无须再撤");
    }

    @ApiOperation(value = "回执", notes = "原有/落库/送回三数，回库现点现取")
    @GetMapping("/receipt")
    @RequiresPermissions("veh:vehAlarmBook:view")
    @ResponseBody
    public AjaxResult receipt(@RequestParam String bookNo) {
        AlarmBookReceipt receipt = vehAlarmRowService.getReceipt(bookNo);
        if (receipt == null) {
            return AjaxResult.error("册码不在册，取不到回执");
        }
        return AjaxResult.successData(200, receipt);
    }

    @ApiOperation(value = "事后对账", notes = "行上结果与回执数须出自同一回点算，差行当场报出")
    @GetMapping("/audit")
    @RequiresPermissions("veh:vehAlarmBook:view")
    @ResponseBody
    public AjaxResult audit(@RequestParam String bookNo) {
        AlarmBookReceipt.Audit audit = vehAlarmRowService.audit(bookNo);
        return AjaxResult.successData(audit.isBalanced() ? 200 : 500, audit);
    }

    @Log(title = "报警处置行勾核结果", action = "decide")
    @ApiOperation(value = "行结果三选一", notes = "未勾→收下/退回，已落结果的行不许覆")
    @PostMapping("/decide")
    @RequiresPermissions("veh:vehAlarmBook:decide")
    @ResponseBody
    public AjaxResult decide(@RequestParam Long id,
                             @RequestParam int status,
                             @RequestParam(required = false) String reason) {
        if (status != 1 && status != 2) {
            return AjaxResult.error("行上的结果只许三选一：未勾、收下、退回");
        }
        int n = vehAlarmRowService.decideRow(id, status, reason);
        return n > 0 ? AjaxResult.success("结果已落行")
                : AjaxResult.error("该行已落结果，不许覆；要改先撤册");
    }

    @ApiOperation(value = "现行名单", notes = "进门行按系统排定行次升序")
    @GetMapping("/rows/current")
    @RequiresPermissions("veh:vehAlarmBook:view")
    @ResponseBody
    public AjaxResult currentRows(@RequestParam String bookNo) {
        List<TVehAlarmRow> rows = vehAlarmRowService.listCurrentRows(bookNo);
        return AjaxResult.successData(200, rows);
    }

    @ApiOperation(value = "往期层", notes = "只朝旧册开，一条不许少")
    @GetMapping("/rows/archived")
    @RequiresPermissions("veh:vehAlarmBook:view")
    @ResponseBody
    public AjaxResult archivedRows(@RequestParam String bookNo) {
        List<TVehAlarmRow> rows = vehAlarmRowService.listArchivedRows(bookNo);
        return AjaxResult.successData(200, rows);
    }

    @Log(title = "未勾行挪往期", action = "archive")
    @ApiOperation(value = "挪往期", notes = "连着两个报送年度仍未勾的现行行挪往期")
    @PostMapping("/archive")
    @RequiresPermissions("veh:vehAlarmBook:archive")
    @ResponseBody
    public AjaxResult archive(@RequestParam int currentReportYear) {
        int n = vehAlarmRowService.archiveStaleRows(currentReportYear);
        return AjaxResult.success("已把" + n + "行未勾旧行挪到往期，现行名单不再摆出");
    }
}

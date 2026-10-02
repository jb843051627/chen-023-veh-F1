package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TVehAlarmRow;
import com.fc.v2.model.custom.AlarmBookPayload;
import com.fc.v2.model.custom.AlarmBookReceipt;

/**
 * 报警处置核收 Service（batch-process 形状：整册进门、一道口子，无单条零递）。
 *
 * 门规：
 * 1. 一册一次、一次性进门、整册导入；机构自录与市级转递两条来路走同一道门。
 * 2. 进门头一步数行，压过每册行顶的整册原样退回、库里一行不留。
 * 3. 一行两个数（采集条数、未处置条数）一块儿递，缺任一个不许代补；
 *    认车只认车辆营运代号，对不上/念不全一律退回该行；未处置 &gt; 采集的该行原路退回、
 *    缘故写在行上，同册别行不受牵动。
 * 4. 行次进门时由系统按落库先后排定，排定不再挪。
 * 5. 同册码第二遍递交只与库里已有行照面：一行不加、一行不减；要改先撤册再重递。
 * 6. 回执只填原有/落库/送回三数，回库现点现取且接得平；空册写“空册”不发核讫。
 * 7. 连着两个报送年度结果仍空着（未勾）的行挪往期，往期照旧翻得着、一条不许少。
 *
 * @author fuce
 * @date 2026-09-28
 */
public interface ITVehAlarmRowService {

    /** 按主键回查单行 */
    TVehAlarmRow selectTVehAlarmRowById(Long id);

    /**
     * 一册整进门：唯一入口。返回三数回执。
     * 超顶：整册退回（无行落库）；空册：回执“空册”不核讫；
     * 二遍：仅照面，duplicate=true，库不动。
     */
    AlarmBookReceipt receiveBook(AlarmBookPayload payload);

    /**
     * 撤册：有效册码腾空（active_key 置空、void_flag=1），旧行一行不删。
     * 撤完同册码方可重递，重递交的是新册头，头一遍落的结果覆不掉。
     * @return 是否撤成（册码不在册/已撤返回 false）
     */
    boolean voidBook(String bookNo, String reason);

    /** 回执：三数回库现点现取，不拿屏上当下那份凑数。册码不在册返回 null。 */
    AlarmBookReceipt getReceipt(String bookNo);

    /**
     * 事后对账：任挑一行回头看，行上结果须与办结快照、回执三数出自同一回点算；
     * 差开的行号在 diffRows 里当场报出。
     */
    AlarmBookReceipt.Audit audit(String bookNo);

    /**
     * 勾核行结果：未勾 → 收下/退回，只许三选一，已落下结果的行不许覆。
     * @param status 1收下 2退回
     */
    int decideRow(Long id, int status, String reason);

    /**
     * 列出某册头下的行：archived=0 现行名单；archived=1 往期层（一条不许少）；
     * null 两层都给，按行次升序。
     */
    List<TVehAlarmRow> listRows(Long bookId, Integer archived);

    /** 现行名单（archived=0），按行次升序 */
    List<TVehAlarmRow> listCurrentRows(String bookNo);

    /** 往期层（archived=1），按行次升序，只朝旧册开 */
    List<TVehAlarmRow> listArchivedRows(String bookNo);

    /**
     * 挪往期：结果空着（未勾）且进门年度与当前报送年度连着跨两个年度的行，
     * 现行名单撤下、往期层照旧可翻。新进门的行走不着这条路。
     * @return 挪动的行数
     */
    int archiveStaleRows(int currentReportYear);
}

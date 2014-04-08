/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.eip.cayenne.om.portlet.EipTTodo;
import com.aimluck.eip.cayenne.om.portlet.VEipTScheduleList;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.schedule.util.ScheduleUtils;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.portal.ALPortalApplicationService;
import com.aimluck.eip.todo.util.ToDoUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * スケジュール1日表示の検索結果を管理するクラスです。
 * 
 */
public class ScheduleOnedaySelectData extends AjaxScheduleMonthlySelectData {

  /** <code>logger</code> logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ScheduleOnedaySelectData.class.getName());

  /** <code>viewDate</code> 表示する日付 */
  private ALDateTimeField viewDate;

  /** <code>prevDate</code> 前の日付 */
  private ALDateTimeField prevDate;

  /** <code>nextDate</code> 次の日付 */
  private ALDateTimeField nextDate;

  /** <code>prevWeek</code> 前の週 */
  private ALDateTimeField prevWeek;

  /** <code>nextWeek</code> 次の週 */
  private ALDateTimeField nextWeek;

  /** <code>prevWeek</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextWeek</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>today</code> 今日 */
  private ALDateTimeField today;

  /** <code>resultData</code> 検索結果 */
  private ScheduleResultData[] resultData;

  // /** <code>spanResultData</code> 検索結果 */
  // private ScheduleResultData spanResultData;

  /** <code>termList</code> 期間スケジュールリスト */
  private ArrayList<ScheduleResultData> termList;

  /** <code>startHour</code> 表示開始時間 */
  protected int startHour;

  /** <code>endHour</code> 表示終了時間 */
  protected int endHour;

  /** <code>count</code> カウンター */
  private int count;

  /** <code>viewtype</code> 表示タイプ */
  protected String viewtype;

  /** <code>is_duplicate</code> 重複スケジュールがあるかどうか */
  protected boolean is_duplicate;

  /** <code>dlist</code> 重複スケジュール */
  private List<ScheduleResultData> dlist;

  /** <code>tmpIndex</code> テンポラリ */
  int tmpIndex;

  /** <code>tmpViewDate2</code> テンポラリ */
  protected String tmpViewDate2;

  /** <code>rowspanMap</code> rowpspan */
  private Map<Integer, Integer> rowspanMap;

  /** <code>rowIndex</code> rowIndex */
  private int rowIndex;

  /** <code>viewJob</code> ToDo 表示設定 */
  protected int viewToDo;

  /** <code>todoList</code> ToDo リスト */
  private List<ScheduleToDoResultData> todoList;

  /** ポートレット ID */
  private String portletId;

  /** <code>hasAuthoritySelfInsert</code> アクセス権限 */
  private boolean hasAuthoritySelfInsert = false;

  private boolean hasAuthorityNote = false;

  private String userName;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    userName = ALEipUtils.getALEipUser(rundata).getAliasName().toString();
    // 展開されるパラメータは以下の通りです。
    // ・viewDate 形式：yyyy-MM-dd

    // POST/GET から yyyy-MM-dd の形式で受け渡される。
    viewDate = new ALDateTimeField("yyyy-MM-dd");
    viewDate.setNotNull(true);
    nextDate = new ALDateTimeField("yyyy-MM-dd");
    prevDate = new ALDateTimeField("yyyy-MM-dd");
    nextWeek = new ALDateTimeField("yyyy-MM-dd");
    prevWeek = new ALDateTimeField("yyyy-MM-dd");
    nextMonth = new ALDateTimeField("yyyy-MM-dd");
    prevMonth = new ALDateTimeField("yyyy-MM-dd");
    today = new ALDateTimeField("yyyy-MM-dd");
    Calendar to = Calendar.getInstance();
    to.set(Calendar.HOUR_OF_DAY, 0);
    to.set(Calendar.MINUTE, 0);
    today.setValue(to.getTime());
    // 表示開始時間の設定
    String startHourInit;
    try {
      startHourInit =
        ALEipUtils
          .getPortlet(rundata, context)
          .getPortletConfig()
          .getInitParameter("p1a-rows");
    } catch (NullPointerException e) {
      startHourInit = null;
    }
    startHour = startHourInit != null ? Integer.parseInt(startHourInit) : 0;
    startHour = startHour > 24 ? 0 : startHour;
    // 表示終了時間の設定
    String endHourInit;
    try {
      endHourInit =
        ALEipUtils
          .getPortlet(rundata, context)
          .getPortletConfig()
          .getInitParameter("p1b-rows");
    } catch (NullPointerException e) {
      endHourInit = null;
    }
    endHour = endHourInit != null ? Integer.parseInt(endHourInit) : 13;
    endHour = endHour > 24 ? 13 : endHour;
    // ToDo 表示設定
    String todoInit;
    try {
      todoInit =
        ALEipUtils
          .getPortlet(rundata, context)
          .getPortletConfig()
          .getInitParameter("p5a-view");
    } catch (NullPointerException e) {
      todoInit = null;
    }
    viewToDo =
      (!ALPortalApplicationService.isActive(ToDoUtils.TODO_PORTLET_NAME) || todoInit == null)
        ? 0
        : Integer.parseInt(todoInit);
    tmpIndex = 0;
    count = 0;
    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // 表示する日付
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("view_start")) {
        ALEipUtils.setTemp(rundata, context, "view_start", rundata
          .getParameters()
          .getString("view_start"));
      }
    }
    // viewDate に値を設定する。
    String tmpViewDate = ALEipUtils.getTemp(rundata, context, "view_start");
    if (tmpViewDate2 != null) {
      tmpViewDate = tmpViewDate2;
    }
    if (tmpViewDate == null || tmpViewDate.equals("")) {
      // セッションに情報がない場合は今日の日付を設定する。
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewDate.setValue(cal.getTime());
    } else {
      viewDate.setValue(tmpViewDate);
      if (!viewDate.validate(new ArrayList<String>())) {
        ALEipUtils.removeTemp(rundata, context, "view_start");

        throw new ALPageNotFoundException();

      }
    }
    // MonthlyCalendarに表示する月を登録
    this.setMonthlyCalendarViewMonth(viewDate.getYear(), viewDate.getMonth());

    Calendar viewDateCal = Calendar.getInstance();
    viewDateCal.setTime(viewDate.getValue());
    viewDateCal.add(Calendar.DATE, 7);
    nextWeek.setValue(viewDateCal.getTime());
    viewDateCal.setTime(viewDate.getValue());
    viewDateCal.add(Calendar.DATE, -7);
    prevWeek.setValue(viewDateCal.getTime());
    viewDateCal.setTime(viewDate.getValue());
    viewDateCal.add(Calendar.MONTH, 1);
    nextMonth.setValue(viewDateCal.getTime());
    viewDateCal.setTime(viewDate.getValue());
    viewDateCal.add(Calendar.MONTH, -1);
    prevMonth.setValue(viewDateCal.getTime());

    resultData = new ScheduleResultData[(endHour - startHour) * 12 * 2];
    dlist = new ArrayList<ScheduleResultData>();
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewDate.getValue());
    cal2.add(Calendar.DATE, 1);
    nextDate.setValue(cal2.getTime());
    cal2.add(Calendar.DATE, -2);
    prevDate.setValue(cal2.getTime());

    rowspanMap = new HashMap<Integer, Integer>();
    for (int i = startHour; i <= endHour; i++) {
      rowspanMap.put(Integer.valueOf(i), Integer.valueOf(12));
    }

    ALEipUtils.setTemp(rundata, context, "tmpStart", viewDate.toString()
      + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", viewDate.toString()
      + "-00-00");

    termList = new ArrayList<ScheduleResultData>();

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
    viewtype = "oneday";

    int userId = ALEipUtils.getUserId(rundata);

    // アクセス権限
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();

    hasAuthoritySelfInsert =
      aclhandler.hasAuthority(
        userId,
        ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF,
        ALAccessControlConstants.VALUE_ACL_INSERT);

    hasAuthorityNote = ALPortalApplicationService.isActive("Note");

  }

  /**
   * 
   * @param rundata
   * @param context
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected ResultList<VEipTScheduleList> selectList(RunData rundata,
      Context context) throws ALPageNotFoundException, ALDBErrorException {
    try {
      List<VEipTScheduleList> resultBaseList =
        getSelectQuery(rundata, context).fetchList();
      List<VEipTScheduleList> resultList =
        ScheduleUtils.sortByDummySchedule(resultBaseList);

      List<VEipTScheduleList> list = new ArrayList<VEipTScheduleList>();
      List<VEipTScheduleList> delList = new ArrayList<VEipTScheduleList>();
      int delSize = 0;
      int resultSize = resultList.size();
      int size = 0;
      boolean canAdd = true;
      for (int i = 0; i < resultSize; i++) {
        VEipTScheduleList record = resultList.get(i);
        delList.clear();
        canAdd = true;
        size = list.size();
        for (int j = 0; j < size; j++) {
          VEipTScheduleList record2 = list.get(j);
          if (!record.getRepeatPattern().equals("N")
            && "D".equals(record2.getStatus())
            && record.getScheduleId().intValue() == record2
              .getParentId()
              .intValue()) {
            canAdd = false;
            break;
          }
          if (!record2.getRepeatPattern().equals("N")
            && "D".equals(record.getStatus())
            && record2.getScheduleId().intValue() == record
              .getParentId()
              .intValue()) {
            // [繰り返しスケジュール] 親の ID を検索
            if (!delList.contains(record2)) {
              delList.add(record2);
            }
            canAdd = true;
          }
        }
        delSize = delList.size();
        for (int k = 0; k < delSize; k++) {
          list.remove(delList.get(k));
        }

        if (canAdd) {
          list.add(record);
        }
      }

      // ダミーを削除する．
      delList.clear();
      size = list.size();
      for (int i = 0; i < size; i++) {
        VEipTScheduleList record = list.get(i);
        if ("D".equals(record.getStatus())) {
          delList.add(record);
        }
      }
      delSize = delList.size();
      for (int i = 0; i < delSize; i++) {
        list.remove(delList.get(i));
      }

      // ソート
      Collections.sort(list, new Comparator<VEipTScheduleList>() {

        @Override
        public int compare(VEipTScheduleList a, VEipTScheduleList b) {
          Calendar cal = Calendar.getInstance();
          Calendar cal2 = Calendar.getInstance();
          cal.setTime(a.getStartDate());
          cal.set(0, 0, 0);
          cal2.setTime(b.getStartDate());
          cal2.set(0, 0, 0);
          if ((cal.getTime()).compareTo(cal2.getTime()) != 0) {
            return (cal.getTime()).compareTo(cal2.getTime());
          } else {
            cal.setTime(a.getEndDate());
            cal.set(0, 0, 0);
            cal2.setTime(b.getEndDate());
            cal2.set(0, 0, 0);

            return (cal.getTime()).compareTo(cal2.getTime());
          }
        }
      });

      if (viewToDo == 1) {
        // ToDo の読み込み
        loadToDo(rundata, context);
      }

      return new ResultList<VEipTScheduleList>(list);
    } catch (Exception e) {
      logger.error("[ScheduleOnedaySelectData]", e);
      throw new ALDBErrorException();
    }
  }

  /**
   * 検索条件を設定した SelectQuery を返します。
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Deprecated
  protected SelectQuery<VEipTScheduleList> getSelectQuery(RunData rundata,
      Context context) {
    SelectQuery<VEipTScheduleList> query =
      Database.query(VEipTScheduleList.class);

    Expression exp1 =
      ExpressionFactory.matchExp(VEipTScheduleList.USER_ID_PROPERTY, Integer
        .valueOf(ALEipUtils.getUserId(rundata)));
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.matchExp(
        VEipTScheduleList.TYPE_PROPERTY,
        ScheduleUtils.SCHEDULEMAP_TYPE_USER);
    query.andQualifier(exp2);

    // 終了日時
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(
        VEipTScheduleList.END_DATE_PROPERTY,
        viewDate.getValue());

    // 日付を1日ずつずらす
    Calendar cal = Calendar.getInstance();
    cal.setTime(viewDate.getValue());
    cal.add(Calendar.DATE, 1);
    ALDateTimeField field = new ALDateTimeField();
    field.setValue(cal.getTime());
    // 開始日時
    // LESS_EQUALからLESS_THANへ修正、期間スケジュールFIXのため(Haruo Kaneko)
    Expression exp12 =
      ExpressionFactory.lessExp(VEipTScheduleList.START_DATE_PROPERTY, field
        .getValue());
    // 通常スケジュール
    Expression exp13 =
      ExpressionFactory.noMatchExp(
        VEipTScheduleList.REPEAT_PATTERN_PROPERTY,
        "N");
    // 期間スケジュール
    Expression exp14 =
      ExpressionFactory.noMatchExp(
        VEipTScheduleList.REPEAT_PATTERN_PROPERTY,
        "S");
    query.andQualifier((exp11.andExp(exp12)).orExp(exp13.andExp(exp14)));
    // 開始日時でソート
    List<Ordering> orders = new ArrayList<Ordering>();
    orders.add(new Ordering(VEipTScheduleList.START_DATE_PROPERTY, true));
    orders.add(new Ordering(VEipTScheduleList.END_DATE_PROPERTY, true));
    query.getQuery().addOrderings(orders);

    return query;
  }

  /**
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  protected VEipTScheduleList selectDetail(RunData rundata, Context context) {
    // このメソッドは利用されません。
    return null;
  }

  /**
   * 
   * @param record
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected Object getResultData(VEipTScheduleList record)
      throws ALPageNotFoundException, ALDBErrorException {
    ScheduleResultData rd = new ScheduleResultData();
    ScheduleResultData rd2 = new ScheduleResultData();
    rd.initField();
    rd2.setFormat("yyyy-MM-dd-HH-mm");
    rd2.initField();
    try {
      if ("R".equals(record.getStatus())) {
        return rd;
      }
      if (!ScheduleUtils.isView(viewDate, record.getRepeatPattern(), record
        .getStartDate(), record.getEndDate())) {
        return rd;
      }
      // ID
      rd.setScheduleId(record.getScheduleId().intValue());
      // 親スケジュール ID
      rd.setParentId(record.getParentId().intValue());
      // タイトル
      rd.setName(record.getName());
      // 開始時間
      rd.setStartDate(record.getStartDate());
      // 終了時間
      rd.setEndDate(record.getEndDate());
      // 仮スケジュールかどうか
      rd.setTmpreserve("T".equals(record.getStatus()));
      // 公開するかどうか
      rd.setPublic("O".equals(record.getPublicFlag()));
      // 表示するかどうか
      rd.setHidden("P".equals(record.getPublicFlag()));
      // ダミーか
      // rd.setDummy("D".equals(record.getStatus()));
      // 繰り返しパターン
      rd.setPattern(record.getRepeatPattern());

      // 期間スケジュールの場合
      if (rd.getPattern().equals("S")) {
        termList.add(rd);
        return rd;
      }

      // 繰り返しスケジュールの場合
      if (!rd.getPattern().equals("N")) {

        if (!ScheduleUtils.isView(viewDate, rd.getPattern(), rd
          .getStartDate()
          .getValue(), rd.getEndDate().getValue())) {
          return rd;
        }
        rd.setRepeat(true);
      }

      // Oneday
      boolean dup = false;
      int sta = startHour * 12;
      int eta = endHour * 12;
      int st =
        Integer.parseInt(rd.getStartDate().getHour())
          * 12
          + Integer.parseInt(rd.getStartDate().getMinute())
          / 5;
      int ed =
        Integer.parseInt(rd.getEndDate().getHour())
          * 12
          + Integer.parseInt(rd.getEndDate().getMinute())
          / 5;
      if (!(rd.getStartDate().getDay().equals(rd.getEndDate().getDay()))
        && rd.getEndDate().getHour().equals("0")) {
        ed = 12 * 24;
      }
      if ((ed - sta > 0 && eta - st > 0) || (ed - sta == 0 && st == ed)) {
        if (sta > st) {
          st = sta;
        }
        if (eta < ed) {
          ed = eta;
        }
        sta -= rowIndex;
        // eta -= rowIndex;
        int tmpRowIndex = rowIndex;
        if (ed - st == 0) {
          rd.setRowspan(1);
          Integer rowspan =
            rowspanMap.get(Integer.valueOf(rd.getStartDate().getHour()));
          if (rowspan.intValue() > 12) {
            (resultData[tmpIndex]).setDuplicate(true);
            rd.setDuplicate(true);
          }
          rowspanMap.put(Integer.valueOf(rd.getStartDate().getHour()), Integer
            .valueOf(rowspan.intValue() + 1));
          rowIndex++;
          ed++;
        } else {
          rd.setRowspan(ed - st);
        }

        if (st - sta - count > 0) {
          rd2.setRowspan(st - sta - count);
          Calendar cal = Calendar.getInstance();
          cal.setTime(viewDate.getValue());
          cal.add(Calendar.HOUR, startHour);
          int hour = (count - tmpRowIndex) / 12;
          int min = ((count - tmpRowIndex) % 12) * 5;
          cal.add(Calendar.HOUR, hour);
          cal.add(Calendar.MINUTE, min);
          rd2.setStartDate(cal.getTime());
          hour = (st - sta - count) / 12;
          min = ((st - sta - count) % 12) * 5;
          cal.add(Calendar.HOUR, hour);
          cal.add(Calendar.MINUTE, min);
          rd2.setEndDate(cal.getTime());
          resultData[count] = rd2;
        } else if (st - sta - count != 0) {
          dlist.add(rd);
          rd.setDuplicate(true);
          dup = true;
          is_duplicate = true;
          (resultData[tmpIndex]).setDuplicate(true);
        }
        if (!dup) {
          resultData[st - sta] = rd;
          tmpIndex = st - sta;
          count = ed - sta;
        }
      }
    } catch (Exception e) {
      logger.error("schedule", e);

      return null;
    }
    return rd;
  }

  /**
   * 
   * @param record
   * @return
   */
  @Override
  protected Object getResultDataDetail(VEipTScheduleList record) {
    return null;
  }

  /**
   * 
   * @return
   */
  @Override
  protected Attributes getColumnMap() {
    return null;
  }

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public boolean doViewList(ALAction action, RunData rundata, Context context) {
    boolean res = super.doViewList(action, rundata, context);
    // 後処理
    if (res) {
      postDoList();
    }
    return res;
  }

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public boolean doSelectList(ALAction action, RunData rundata, Context context) {
    boolean res = super.doSelectList(action, rundata, context);
    // 後処理
    if (res) {
      postDoList();
    }
    return res;
  }

  /**
   * 検索後の処理を行います。
   * 
   */
  private void postDoList() {
    int index = (endHour - startHour) * 12 + rowIndex;
    if (index > count) {
      ScheduleResultData rd = new ScheduleResultData();
      rd.setFormat("yyyy-MM-dd-HH-mm");
      rd.initField();
      rd.setRowspan(index - count);
      Calendar cal = Calendar.getInstance();
      cal.setTime(viewDate.getValue());
      cal.add(Calendar.HOUR, startHour);
      int hour = (count - rowIndex) / 12;
      int min = ((count - rowIndex) % 12) * 5;
      cal.add(Calendar.HOUR, hour);
      cal.add(Calendar.MINUTE, min);
      rd.setStartDate(cal.getTime());
      hour = (index - count) / 12;
      min = ((index - count) % 12) * 5;
      cal.add(Calendar.HOUR, hour);
      cal.add(Calendar.MINUTE, min);
      rd.setEndDate(cal.getTime());
      resultData[count] = rd;
    }
  }

  public void loadToDo(RunData rundata, Context context) {
    todoList = new ArrayList<ScheduleToDoResultData>();
    try {
      SelectQuery<EipTTodo> query = getSelectQueryForTodo(rundata, context);
      List<EipTTodo> todos = query.fetchList();

      int todosize = todos.size();
      for (int i = 0; i < todosize; i++) {
        EipTTodo record = todos.get(i);
        ScheduleToDoResultData rd = new ScheduleToDoResultData();
        rd.initField();

        // ポートレット ToDo のへのリンクを取得する．
        String todo_url =
          getPortletURItoTodo(
            rundata,
            record.getTodoId().longValue(),
            portletId);

        rd.setTodoId(record.getTodoId().intValue());
        rd.setTodoName(record.getTodoName());
        rd.setUserId(record.getTurbineUser().getUserId().intValue());
        rd.setStartDate(record.getStartDate());
        rd.setEndDate(record.getEndDate());
        rd.setTodoUrl(todo_url);
        // 公開/非公開を設定する．
        rd.setPublicFlag("T".equals(record.getPublicFlag()));
        todoList.add(rd);
      }
    } catch (Exception ex) {
      logger.error("schedule", ex);
      return;
    }
  }

  protected String getPortletURItoTodo(RunData rundata, long entityid,
      String schedulePortletId) {
    return ScheduleUtils.getPortletURItoTodoDetailPane(
      rundata,
      "ToDo",
      entityid,
      schedulePortletId);
  }

  private SelectQuery<EipTTodo> getSelectQueryForTodo(RunData rundata,
      Context context) {
    Integer uid = Integer.valueOf(ALEipUtils.getUserId(rundata));
    SelectQuery<EipTTodo> query = Database.query(EipTTodo.class);

    Expression exp1 =
      ExpressionFactory.noMatchExp(EipTTodo.STATE_PROPERTY, Short
        .valueOf((short) 100));
    query.setQualifier(exp1);
    Expression exp2 =
      ExpressionFactory.matchExp(EipTTodo.ADDON_SCHEDULE_FLG_PROPERTY, "T");
    query.andQualifier(exp2);
    Expression exp3 =
      ExpressionFactory.matchDbExp(TurbineUser.USER_ID_PK_COLUMN, uid);
    query.andQualifier(exp3);

    // 終了日時
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(EipTTodo.END_DATE_PROPERTY, viewDate
        .getValue());
    // 開始日時
    Expression exp12 =
      ExpressionFactory.lessOrEqualExp(EipTTodo.START_DATE_PROPERTY, viewDate
        .getValue());

    // 開始日時のみ指定されている ToDo を検索
    Expression exp21 =
      ExpressionFactory.lessOrEqualExp(EipTTodo.START_DATE_PROPERTY, viewDate
        .getValue());
    Expression exp22 =
      ExpressionFactory.matchExp(EipTTodo.END_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    // 終了日時のみ指定されている ToDo を検索
    Expression exp31 =
      ExpressionFactory.greaterOrEqualExp(EipTTodo.END_DATE_PROPERTY, viewDate
        .getValue());
    Expression exp32 =
      ExpressionFactory.matchExp(EipTTodo.START_DATE_PROPERTY, ToDoUtils
        .getEmptyDate());

    query.andQualifier((exp11.andExp(exp12)).orExp(exp21.andExp(exp22)).orExp(
      exp31.andExp(exp32)));
    return query;
  }

  /**
   * 表示する日付を取得します。
   * 
   * @return
   */
  @Override
  public ALDateTimeField getViewDate() {
    return viewDate;
  }

  public String getViewDateText() {
    return ALLocalizationUtils.getl10nFormat("SCHEDULE_DATE_FORMAT", viewDate
      .getYear(), viewDate.getMonth(), viewDate.getDay());
  }

  public String getViewDateNospaceText() {
    return ALLocalizationUtils.getl10nFormat(
      "SCHEDULE_DATE_FORMAT_NOSPACE",
      viewDate.getYear(),
      viewDate.getMonth(),
      viewDate.getDay());
  }

  /**
   * 次の日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextDate() {
    return nextDate;
  }

  /**
   * 前の日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevDate() {
    return prevDate;
  }

  /**
   * 前の週を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevWeek() {
    return prevWeek;
  }

  /**
   * 次の週を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextWeek() {
    return nextWeek;
  }

  /**
   * 今日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getToday() {
    return today;
  }

  /**
   * 先月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevMonth() {
    return prevMonth;
  }

  /**
   * 来月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextMonth() {
    return nextMonth;
  }

  /**
   * スケジュールを取得します。
   * 
   * @param index
   * @return
   */
  public ScheduleResultData getResult(int index) {
    return resultData[index];
  }

  /**
   * 表示開始時間を取得します。
   * 
   * @return
   */
  public int getStartHour() {
    return startHour;
  }

  /**
   * 表示終了時間を取得します。
   * 
   * @return
   */
  public int getEndHour() {
    return endHour;
  }

  /**
   * 操作しているユーザーの姓名を取得します。
   */
  public String getUserName() {
    return userName;
  }

  /**
   * 表示タイプを取得します。
   * 
   * @return
   */
  public String getViewtype() {
    return viewtype;
  }

  /**
   * 重複スケジュールがあるかどうか
   * 
   * @return
   */
  public boolean isDuplicate() {
    return is_duplicate;
  }

  /**
   * 重複スケジュールリストを取得します。
   * 
   * @param id
   * @return
   */
  public List<ScheduleResultData> getDuplicateScheduleList() {
    return dlist;
  }

  // /**
  // * 期間スケジュールを取得します。
  // *
  // * @return
  // */
  // public ScheduleResultData getSpanSchedule() {
  // return spanResultData;
  // }

  /**
   * 表示日付（テンポラリ）を設定します。
   * 
   * @param date
   */
  public void setTmpViewDate(String date) {
    tmpViewDate2 = date;
  }

  /**
   * Rowspanを取得します。
   * 
   * @param hour
   * @return
   */
  public int getRowspan(int hour) {
    return rowspanMap.get(Integer.valueOf(hour)).intValue();
  }

  public List<ScheduleResultData> getTermResultDataList() {
    return termList;
  }

  public List<ScheduleToDoResultData> getToDoResultDataList() {
    return todoList;
  }

  public void setPortletId(String id) {
    portletId = id;
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_SCHEDULE_SELF;
  }

  public boolean hasAuthoritySelfInsert() {
    return hasAuthoritySelfInsert;
  }

  public boolean hasAuthorityNote() {
    return hasAuthorityNote;
  }

}
